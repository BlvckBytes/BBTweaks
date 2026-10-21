package me.blvckbytes.bbtweaks.mspt_monitoring;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.ConfigKeeperReloadEvent;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.AfterStartup;
import me.blvckbytes.bbtweaks.auto_wirer.Disableable;
import me.blvckbytes.bbtweaks.util.ThrowingConsumer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

public class MsptMonitoring implements Listener, AfterStartup, Disableable {

  private record DurationsCapture(
    long timestampMillis,
    long usedMemory,
    int playerCount,
    long captureDurationNanos,
    double[] durationsMillis
  ) {}

  private static final int COUNT_TOLERANCE_FACTOR = 3;

  private static final long CAPTURE_PERIOD_S = 1;
  private static final long MAX_DURATIONS_COUNT = 20 * CAPTURE_PERIOD_S * COUNT_TOLERANCE_FACTOR;

  private static final long WRITE_PERIOD_S = 5;
  private static final long MAX_CAPTURES_COUNT = (WRITE_PERIOD_S + CAPTURE_PERIOD_S - 1) / CAPTURE_PERIOD_S * COUNT_TOLERANCE_FACTOR;

  private final ScheduledExecutorService captureExecutorService;
  private final ScheduledExecutorService writeExecutorService;

  private final Deque<Double> tickDurationsMillis;

  private long lastCaptureNanos;
  private final Deque<DurationsCapture> captures;

  private volatile int lastKnownPlayerCount;

  private final ConfigKeeper<MainSection> config;
  private final Plugin plugin;
  private final Connection connection;

  private @Nullable HttpServer apiServer;

  public MsptMonitoring(
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) throws Exception {
    this.captureExecutorService = Executors.newSingleThreadScheduledExecutor();
    this.writeExecutorService = Executors.newSingleThreadScheduledExecutor();

    this.tickDurationsMillis = new ArrayDeque<>();
    this.captures = new ArrayDeque<>();

    this.config = config;
    this.plugin = plugin;

    var databaseFile = new File(plugin.getDataFolder(), "mspt-monitoring.db");

    if (!databaseFile.exists()) {
      if (!databaseFile.createNewFile())
        throw new IllegalStateException("Could not create file at " + databaseFile);
    }

    else if (!databaseFile.isFile())
      throw new IllegalStateException("Expected a file at " + databaseFile);

    this.connection = openDatabaseFile(databaseFile);

    startOrRestartApiServer();
  }

  @EventHandler
  public void onTickEnd(ServerTickEndEvent event) {
    synchronized (tickDurationsMillis) {
      while (tickDurationsMillis.size() >= MAX_DURATIONS_COUNT)
        tickDurationsMillis.removeFirst();

      tickDurationsMillis.add(event.getTickDuration());
    }

    lastKnownPlayerCount = Bukkit.getOnlinePlayers().size();
  }

  @EventHandler
  public void onConfigReload(ConfigKeeperReloadEvent event) {
    if (event.configKeeper != config)
      return;

    startOrRestartApiServer();
  }

  @Override
  public void afterStartup() {
    tickDurationsMillis.clear();

    captureExecutorService.scheduleAtFixedRate(this::captureDurations, CAPTURE_PERIOD_S, CAPTURE_PERIOD_S, TimeUnit.SECONDS);
    writeExecutorService.scheduleAtFixedRate(this::writeCapturedDurations, WRITE_PERIOD_S, WRITE_PERIOD_S, TimeUnit.SECONDS);
  }

  @Override
  public void disable() {
    try {
      captureExecutorService.shutdown();
      if (!captureExecutorService.awaitTermination(5, TimeUnit.SECONDS))
        throw new IllegalStateException("Timed out waiting for termination");
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to terminate the MSPT-monitoring capture thread", e);
    }

    try {
      writeExecutorService.shutdown();
      if (!writeExecutorService.awaitTermination(5, TimeUnit.SECONDS))
        throw new IllegalStateException("Timed out waiting for termination");
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to terminate the MSPT-monitoring writer thread", e);
    }

    stopApiServer();
    closeConnection();
  }

  private void stopApiServer() {
    if (apiServer == null)
      return;

    try {
      apiServer.stop(0);
      apiServer = null;

      plugin.getLogger().info("Stopped the MSPT-monitoring API web-server!");
    } catch (Throwable e) {
      plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to stop the MSPT-monitoring API web-server", e);
    }
  }

  private void startOrRestartApiServer() {
    stopApiServer();

    var port = config.rootSection.msptMonitoring.apiServerPort;

    if (port <= 0) {
      plugin.getLogger().info("The MSPT-monitoring API web-server is not enabled.");
      return;
    }

    try {
      apiServer = HttpServer.create(new InetSocketAddress(port), 0);
      apiServer.setExecutor(Executors.newFixedThreadPool(10));

      apiServer.createContext("/mspt", exchange -> {
        var queryArgs = queryToMap(exchange.getRequestURI().getRawQuery());
        var fromValue = queryArgs.get("from");
        var toValue = queryArgs.get("to");

        if (fromValue == null || toValue == null) {
          respondToExchange(exchange, 400, writer -> writer.write("Missing \"from\" and \"to\" query-parameters"));
          return;
        }

        long from, to;

        try {
          from = Long.parseLong(fromValue);
          to = Long.parseLong(toValue);
        } catch (Exception e) {
          respondToExchange(exchange, 400, writer -> writer.write("Query-parameters \"from\" and \"to\" have to be longs"));
          return;
        }

        if (from >= to) {
          respondToExchange(exchange, 400, writer -> writer.write("Query-parameter \"from\" cannot be greater than or equal to \"to\""));
          return;
        }

        respondToExchange(exchange, 200, writer -> queryAndWriteCaptureRowsCSV(from, to, writer));
      });

      apiServer.start();
      plugin.getLogger().info("Started the MSPT-monitoring API web-server; listening on :" + port + "!");
    } catch (Throwable e) {
      plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to set up the MSPT-monitoring API web-server", e);
    }
  }

  private void respondToExchange(
    HttpExchange exchange,
    int statusCode,
    ThrowingConsumer<Writer> writerHandler
  ) {
    try {
      var acceptEncoding = exchange.getRequestHeaders().getFirst("Accept-Encoding");
      var gzipSupported = acceptEncoding != null && acceptEncoding.contains("gzip");

      var responseHeaders = exchange.getResponseHeaders();

      if (gzipSupported)
        responseHeaders.set("Content-Encoding", "gzip");

      responseHeaders.add("Content-Type", "text/plain; charset=UTF-8");
      responseHeaders.add("Access-Control-Allow-Origin", "*");
      responseHeaders.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
      responseHeaders.add("Access-Control-Allow-Headers", "*");

      exchange.sendResponseHeaders(statusCode, 0);

      var outputStream = exchange.getResponseBody();

      if (gzipSupported)
        outputStream = new GZIPOutputStream(outputStream);

      try (var writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
        writerHandler.accept(writer);
      }
    } catch (Throwable e) {
      plugin.getLogger().log(Level.SEVERE, "An error occurred on the MSPT-monitoring API web-server while trying to respond", e);
    }
  }

  private Map<String, String> queryToMap(String query) {
    var result = new HashMap<String, String>();

    if (query == null || query.isEmpty())
      return result;

    for (var param : query.split("&")) {
      var entry = param.split("=");
      var key = URLDecoder.decode(entry[0], StandardCharsets.UTF_8);
      var value = entry.length > 1 ? URLDecoder.decode(entry[1], StandardCharsets.UTF_8) : "";
      result.put(key, value);
    }

    return result;
  }

  private void captureDurations() {
    if (captureExecutorService.isShutdown())
      return;

    var currentMillis = System.currentTimeMillis();

    var currentNanos = System.nanoTime();
    var lastNanos = lastCaptureNanos;
    lastCaptureNanos = currentNanos;

    double[] clonedElements;

    synchronized (tickDurationsMillis) {
      clonedElements = new double[tickDurationsMillis.size()];

      var nextIndex = 0;

      for (var duration : tickDurationsMillis)
        clonedElements[nextIndex++] = duration;

      tickDurationsMillis.clear();
    }

    // Drop the first capture, as we have no well-defined capture-duration otherwise.
    if (lastNanos == 0)
      return;

    var runtime = Runtime.getRuntime();
    var usedMemory = runtime.totalMemory() - runtime.freeMemory();

    synchronized (captures) {
      while (captures.size() >= MAX_CAPTURES_COUNT)
        captures.removeFirst();

      captures.add(new DurationsCapture(
        currentMillis,
        usedMemory,
        lastKnownPlayerCount,
        currentNanos - lastNanos,
        clonedElements
      ));
    }
  }

  private void writeCapturedDurations() {
    List<DurationsCapture> clonedCaptures;

    synchronized (captures) {
      clonedCaptures = new ArrayList<>(captures);
      captures.clear();
    }

    if (clonedCaptures.isEmpty())
      return;

    var insertSql = """
      INSERT INTO captures (
        `timestamp_millis`,
        `player_count`,
        `used_memory`,
        `tick_count`,
        `capture_duration_nanos`,
        `avg_tick_duration`,
        `max_tick_duration`
      ) VALUES (?, ?, ?, ?, ?, ?, ?)
      """;

    try {
      connection.setAutoCommit(false);
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Could not disable auto-commit on the MSPT-monitoring database connection", e);
      return;
    }

    try (var statement = connection.prepareStatement(insertSql)) {
      for (var capture : clonedCaptures) {
        var durations = capture.durationsMillis();

        double durationSum = 0;
        double maxDuration = 0;

        for (var duration : durations) {
          durationSum += duration;
          maxDuration = Math.max(maxDuration, duration);
        }

        statement.setLong(1, capture.timestampMillis());
        statement.setInt(2, capture.playerCount());
        statement.setLong(3, capture.usedMemory());
        statement.setInt(4, durations.length);
        statement.setLong(5, capture.captureDurationNanos());
        statement.setDouble(6, durations.length == 0 ? 0 : durationSum / durations.length);
        statement.setDouble(7, maxDuration);
        statement.addBatch();
      }

      statement.executeBatch();
      connection.commit();
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to insert a batch into the MSPT-monitoring database", e);

      try {
        connection.rollback();
      } catch (Exception e2) {
        plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to rollback changes made by the insertion-attempt", e2);
      }
    }

    try (var statement = connection.prepareStatement(
      "DELETE FROM `captures` WHERE `timestamp_millis` < ?"
    )) {
      var earliestRetainedStamp = System.currentTimeMillis() - config.rootSection.msptMonitoring.retentionDurationSeconds * 1000L;

      statement.setLong(1, earliestRetainedStamp);
      statement.executeUpdate();
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to delete old rows from the MSPT-monitoring database", e);
    }
  }

  private void closeConnection() {
    try {
      if (connection == null || connection.isClosed())
        return;

      connection.close();

      plugin.getLogger().info("Closed MSPT-monitoring database");
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to close the MSPT-monitoring database", e);
    }
  }

  private Connection openDatabaseFile(File databaseFile) throws Exception {
    var connection = DriverManager.getConnection(
      "jdbc:sqlite:" + databaseFile.getAbsolutePath()
    );

    try (var statement = connection.createStatement()) {
      statement.execute("PRAGMA busy_timeout = 5000");
      statement.execute("PRAGMA journal_mode = WAL");
      statement.execute("PRAGMA synchronous = NORMAL");
    }

    try (var statement = connection.createStatement()) {
      statement.execute(
        """
        CREATE TABLE IF NOT EXISTS `captures` (
          `timestamp_millis`         INTEGER PRIMARY KEY,
          `player_count`             INTEGER NOT NULL,
          `used_memory`              INTEGER NOT NULL,
          `tick_count`               INTEGER NOT NULL,
          `capture_duration_nanos`   INTEGER NOT NULL,
          `avg_tick_duration`        REAL NOT NULL,
          `max_tick_duration`        REAL NOT NULL
        )
        """
      );
    }

    plugin.getLogger().info("Connected to MSPT-monitoring database");

    return connection;
  }

  private void queryAndWriteCaptureRowsCSV(long from, long to, Writer writer) {
    try (var statement = connection.prepareStatement(
      """
      SELECT
        `timestamp_millis`,
        `player_count`,
        `used_memory` / 1048576 AS `used_memory_mb`,
        `tick_count`,
        `capture_duration_nanos` / 1000000 AS `capture_duration_millis`,
        CAST(ROUND(`avg_tick_duration`) AS INTEGER) AS avg_rounded_tick_duration,
        CAST(ROUND(`max_tick_duration`) AS INTEGER) AS max_rounded_tick_duration
      FROM `captures`
      WHERE `timestamp_millis` >= ?
      AND `timestamp_millis` <= ?
      ORDER BY `timestamp_millis` ASC
      """
    )) {
      statement.setLong(1, from);
      statement.setLong(2, to);

      var resultSet = statement.executeQuery();

      var metadata = resultSet.getMetaData();
      var columnCount = metadata.getColumnCount();

      for (var columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
        if (columnIndex > 1)
          writer.write(',');

        writer.write(metadata.getColumnLabel(columnIndex));
      }

      while (resultSet.next()) {
        writer.write('\n');

        for (var columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
          if (columnIndex > 1)
            writer.write(',');

          var value = resultSet.getString(columnIndex);

          if (value != null)
            writer.write(value);
        }
      }
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to build the capture-rows CSV", e);
    }
  }
}
