package me.blvckbytes.bbtweaks.block_facing;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.block_facing.command.BlockFacingCommandSection;
import me.blvckbytes.bbtweaks.block_facing.settings.FacingOverride;
import me.blvckbytes.bbtweaks.block_facing.settings_display.BlockFacingSettingsDisplaySection;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BlockFacingSection extends ConfigSection {

  public @CSAlways BlockFacingCommandSection command;

  public ComponentMarkup playersOnly;
  public ComponentMarkup noPermission;
  public ComponentMarkup actionUsage;
  public ComponentMarkup modifyPlacedBlocksNowEnabled;
  public ComponentMarkup modifyPlacedBlocksAlreadyEnabled;
  public ComponentMarkup modifyPlacedBlocksNowDisabled;
  public ComponentMarkup modifyPlacedBlocksAlreadyDisabled;
  public ComponentMarkup modifyExistingBlocksNowEnabled;
  public ComponentMarkup modifyExistingBlocksAlreadyEnabled;
  public ComponentMarkup modifyExistingBlocksNowDisabled;
  public ComponentMarkup modifyExistingBlocksAlreadyDisabled;
  public ComponentMarkup facingOverrideNowSelected;
  public ComponentMarkup facingOverrideAlreadySelected;

  public Map<String, String> facingNames = Collections.emptyMap();

  public @CSAlways BlockFacingSettingsDisplaySection settingsDisplay;

  public BlockFacingSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    for (var nameEntry : facingNames.entrySet()) {
      var constantName = nameEntry.getKey().trim().toUpperCase();

      FacingOverride facingOverride;

      try {
        facingOverride = FacingOverride.valueOf(constantName);
      } catch (Throwable e) {
        throw new MappingError("Could not find a facing-override with name \"" + constantName + "\" in map \"facingNames\"");
      }

      FacingOverride.matcher.getNormalizedConstant(facingOverride).setName(nameEntry.getValue());
    }
  }
}
