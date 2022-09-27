package oscar.controller.noise;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public enum NoiseCategory {
  SYNCHRONIZATION_BASED,
  THREAD_BASED,
  LOCK_BASED,
  MISCELLANEOUS,
  SHARED_VARIABLE_BASED;

  NoiseCategory() {}

  public String getShorthand() {
    return NoiseLocation.generateShorthand(name());
  }

  public static NoiseCategory fromString(String shorthand) {
    List<NoiseCategory> results = Arrays.stream(NoiseCategory.values())
                                        .filter(np -> np.getShorthand().equals(shorthand.toLowerCase()))
                                        .collect(Collectors.toList());

    if (results.size() > 1)
      throw new RuntimeException("More than one noise placement categories match shorthand '" + shorthand + "'.");

    if (results.size() == 0)
      throw new RuntimeException("No noise placement categories match shorthand '" + shorthand + "'.");

    return results.get(0);
  }

  public static HashSet<NoiseCategory> getAll() {
    return Arrays.stream(values()).collect(Collectors.toCollection(HashSet::new));
  }
}