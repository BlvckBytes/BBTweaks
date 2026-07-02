package me.blvckbytes.bbtweaks.pipes;

import com.cryptomorin.xseries.XMaterial;
import me.blvckbytes.bbtweaks.pipes.notification.MalformedSignNotification;
import me.blvckbytes.bbtweaks.pipes.notification.PipeNotification;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class PipeSign {

    private static final Pattern COMMA_PATTERN = Pattern.compile(",", Pattern.LITERAL);
    private static final String NAMESPACE_PREFIX_LOWER = "minecraft:";

    public static final PipeSign NO_SIGN = new PipeSign(Collections.emptyList(), Collections.emptyList());

    public final List<Material> includeFilters;
    public final List<Material> excludeFilters;

    private PipeSign(List<Material> includeFilters, List<Material> excludeFilters) {
        this.includeFilters = Collections.unmodifiableList(includeFilters);
        this.excludeFilters = Collections.unmodifiableList(excludeFilters);
    }

    public static PipeSign fromSign(Sign sign, String[] lines, @Nullable List<PipeNotification> notificationOutput) {
        var includeFilters = new ArrayList<Material>();
        var excludeFilters = new ArrayList<Material>();

        parseLineMaterials(sign, lines, 2, includeFilters, notificationOutput);
        parseLineMaterials(sign, lines, 3, excludeFilters, notificationOutput);

        return new PipeSign(includeFilters, excludeFilters);
    }

    private static void parseLineMaterials(Sign sign, String[] lines, int lineId, List<Material> output, @Nullable List<PipeNotification> notificationOutput) {
        for (var token : COMMA_PATTERN.split(lines[lineId])) {
            token = token.trim().toLowerCase();

            if (token.isEmpty())
                continue;

            var material = parseTokenMaterial(token);

            if (material != null) {
                output.add(material);
                continue;
            }

            if (notificationOutput != null)
                notificationOutput.add(new MalformedSignNotification(sign.getLocation(), token, lineId + 1));
        }
    }

    public static @Nullable Material parseTokenMaterial(String trimmedLowerToken) {
        if (trimmedLowerToken.length() > NAMESPACE_PREFIX_LOWER.length() && trimmedLowerToken.startsWith(NAMESPACE_PREFIX_LOWER))
            trimmedLowerToken = trimmedLowerToken.substring(NAMESPACE_PREFIX_LOWER.length()).trim();

        Material material;

        var xMaterial = XMaterial.matchXMaterial(trimmedLowerToken);

        if (xMaterial.isPresent() && (material = xMaterial.get().get()) != null)
            return material;

        if ((material = NumericItemMaterials.tryGetMaterialByNotation(trimmedLowerToken)) != null)
            return material;

        return null;
    }
}
