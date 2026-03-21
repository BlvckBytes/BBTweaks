package me.blvckbytes.bbtweaks.main_command;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.un_craft.MaterialTagRegistry;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LWCExtendBlocksSection extends ConfigSection {

  @CSIgnore
  public final Set<Material> _materialsToExtend;

  public @Nullable List<String> materialTags;

  public ComponentMarkup configNotFound;
  public ComponentMarkup protectionsSectionNotFound;
  public ComponentMarkup couldNotWriteTemplate;
  public ComponentMarkup templateWritten;

  public LWCExtendBlocksSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);

    this._materialsToExtend = new HashSet<>();
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (materialTags != null) {
      for (var tagName : materialTags) {
        var tag = MaterialTagRegistry.getBlockTagByName(tagName);

        if (tag == null)
          throw new MappingError("Could not find a block-material-tag called \"" + tagName + "\"");

        _materialsToExtend.addAll(tag.getValues());
      }
    }
  }
}
