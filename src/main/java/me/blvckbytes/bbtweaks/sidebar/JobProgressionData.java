package me.blvckbytes.bbtweaks.sidebar;

import at.blvckbytes.component_markup.markup.interpreter.DirectFieldAccess;
import com.gamingmesh.jobs.container.JobProgression;

import java.util.Set;

public record JobProgressionData(int level, int currentExperience, int maxExperience) implements DirectFieldAccess {

  @Override
  public Object accessField(String rawIdentifier) {
    return switch (rawIdentifier) {
      case "level" -> level;
      case "current_experience" -> currentExperience;
      case "max_experience" -> maxExperience;
      default -> DirectFieldAccess.UNKNOWN_FIELD_SENTINEL;
    };
  }

  @Override
  public Set<String> getAvailableFields() {
    return Set.of("level", "current_experience", "max_experience");
  }

  public static JobProgressionData fromProgression(JobProgression progression) {
    return new JobProgressionData(progression.getLevel(), (int) progression.getExperience(), progression.getMaxExperience());
  }
}
