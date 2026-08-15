package me.blvckbytes.bbtweaks.emotions;

import at.blvckbytes.cm_mapper.MaterialMatcher;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import com.cryptomorin.xseries.particles.XParticle;
import me.blvckbytes.bbtweaks.emotions.user_profile.EmotionUserProfileStore;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class DisplayedEffectSection extends ConfigSection {

  private static final Random LOCAL_RANDOM = ThreadLocalRandom.current();

  private static final Map<String, Color> colorByConstantName;

  static {
    colorByConstantName = new HashMap<>();

    for (var field : Color.class.getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers()) || !Modifier.isPublic(field.getModifiers()))
        continue;

      if (field.getType() != Color.class)
        continue;

      try {
        colorByConstantName.put(field.getName(), (Color) field.get(null));
      } catch (Throwable ignored) {}
    }
  }

  public long frequencyTicks;
  public long numberOfExecutions;

  public String particle;
  public @Nullable String particleMaterial;
  public @Nullable String particleColor;
  public double particleSize;

  public EffectDisplayType displayType;
  public double yOffset;

  public double cloudRadius;
  public int cloudParticleCount;

  public int numberOfHelixCurves;
  public double helixHeight;
  public double helixRadius;
  public int helixWindings;
  public double helixAngleStepSize;

  @CSIgnore
  public Particle _particle;

  @CSIgnore
  public @Nullable Material _particleMaterial;

  @CSIgnore
  public @Nullable Color _particleColor;

  public DisplayedEffectSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);

    this.displayType = EffectDisplayType.SINGLE;
    this.yOffset = 0;
    this.particleSize = 1;

    this.frequencyTicks = 5;
    this.numberOfExecutions = 1;

    this.cloudRadius = 4;
    this.cloudParticleCount = 20;

    this.numberOfHelixCurves = 2;
    this.helixHeight = 2;
    this.helixRadius = 1;
    this.helixAngleStepSize = .1;
    this.helixWindings = 1;
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (particle == null)
      throw new MappingError("Missing mandatory property \"particle\"");

    var xParticle = XParticle.of(particle);

    if (xParticle.isEmpty() || (_particle = xParticle.get().get()) == null)
      throw new MappingError("Property \"particle\" with value \"" + particle + "\" could not be corresponded to an XParticle");

    if (particleMaterial != null) {
      _particleMaterial = MaterialMatcher.tryMatch(particleMaterial);

      if (_particleMaterial == null)
        throw new MappingError("Property \"particleMaterial\" with value \"" + particleMaterial + "\" could not be corresponded to a Material");
    }

    if (particleColor != null) {
      Color color = null;

      try {
        var parts = particleColor.split(" ");
        color = Color.fromRGB(
          Integer.parseInt(parts[0]),
          Integer.parseInt(parts[1]),
          Integer.parseInt(parts[2])
        );
      } catch (Throwable ignored) {}

      if (color == null)
        color = colorByConstantName.get(particleColor.toUpperCase().trim());

      if (color == null)
        throw new MappingError("Property \"particleColor\" with value \"" + particleColor + "\" does not represent a valid bukkit- or RGB-color (\"R G B\")");

      _particleColor = color;
    }

    if (yOffset < 0)
      throw new MappingError("Property \"yOffset\" cannot be negative");

    if (frequencyTicks < 0)
      throw new MappingError("Property \"frequencyTicks\" cannot be negative");

    if (numberOfExecutions < 0)
      throw new MappingError("Property \"numberOfExecutions\" cannot be negative");

    if (cloudRadius < 0)
      throw new MappingError("Property \"cloudRadius\" cannot be negative");

    if (cloudParticleCount < 0)
      throw new MappingError("Property \"cloudParticleCount\" cannot be negative");

    if (numberOfHelixCurves < 0)
      throw new MappingError("Property \"numberOfHelixCurves\" cannot be negative");

    if (helixHeight < 0) {
      throw new MappingError("Property \"helixHeight\" cannot be negative");
    }

    if (helixRadius < 0)
      throw new MappingError("Property \"helixRadius\" cannot be negative");

    if (helixWindings < 0)
      throw new MappingError("Property \"helixWindings\" cannot be negative");

    if (helixAngleStepSize < 0)
      throw new MappingError("Property \"helixAngleStepSize\" cannot be negative");
  }

  public void playEffect(
    Player receiver,
    NotificationOrigin origin,
    EmotionUserProfileStore profileStore,
    Plugin plugin
  ) {
    if (!profileStore.accessUserProfile(receiver).doesReceive(NotificationPart.EFFECT, origin))
      return;

    if (numberOfExecutions <= 0)
      return;

    playEffectInstance(receiver, plugin, 1);
  }

  private void playEffectInstance(Player receiver, Plugin plugin, int executionCounter) {
    var effectLocation = receiver.getLocation().add(0, yOffset, 0);

    switch (displayType) {
      case SINGLE -> playParticle(receiver, effectLocation);
      case CLOUD -> playEffectCloud(receiver, effectLocation);
      case HELIX -> playEffectHelix(receiver, effectLocation);
    }

    if (executionCounter < numberOfExecutions) {
      Bukkit.getScheduler().runTaskLater(
        plugin,
        () -> playEffectInstance(receiver, plugin, executionCounter + 1),
        frequencyTicks
      );
    }
  }

  private void playEffectHelix(Player target, Location location) {
    var phaseShiftUnit = (2 * Math.PI) / numberOfHelixCurves;

    int numberOfAngleSteps = (int) ((2 * Math.PI) / helixAngleStepSize);
    double heightStepUnit = helixHeight / numberOfAngleSteps / helixWindings;

    double alpha = 0;
    double deltaY = 0;

    for (var alphaStepIndex = 0; alphaStepIndex < numberOfAngleSteps * helixWindings; ++alphaStepIndex) {
      for (var curveIndex = 0; curveIndex < numberOfHelixCurves; ++curveIndex) {
        var phaseShift = phaseShiftUnit * curveIndex;

        var deltaZ = helixRadius * Math.sin(alpha + phaseShift);
        var deltaX = helixRadius * Math.cos(alpha + phaseShift);

        playParticle(target, location.clone().add(deltaX, deltaY, deltaZ));
      }

      deltaY += heightStepUnit;
      alpha += helixAngleStepSize;
    }
  }

  private double generateRandomRadiusOffset(double radius) {
    var radiusScaleFactor = LOCAL_RANDOM.nextDouble();

    if (LOCAL_RANDOM.nextBoolean())
      return -radius * radiusScaleFactor;

    return radius * radiusScaleFactor;
  }

  private void playEffectCloud(Player target, Location location) {
    for (var particleIndex = 0; particleIndex < cloudParticleCount; ++particleIndex) {
      var particleLocation = location.clone().add(
        generateRandomRadiusOffset(cloudRadius),
        generateRandomRadiusOffset(cloudRadius),
        generateRandomRadiusOffset(cloudRadius)
      );

      playParticle(target, particleLocation);
    }
  }

  private void playParticle(Player target, Location location) {
    Object parameter = null;

    if (_particle.getDataType() == Particle.DustOptions.class && _particleColor != null)
      parameter = new Particle.DustOptions(_particleColor, (float) particleSize);

    if (_particle.getDataType() == BlockData.class && _particleMaterial != null)
      parameter = _particleMaterial.createBlockData();

    target.spawnParticle(_particle, location, 1, parameter);
  }
}
