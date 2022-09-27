package oscar.controller.util;

import java.util.Arrays;
import java.util.List;

public class ControllerOption {
  private final List<String> aliases;
  private final String description;
  private final String name;
  private final String defaultVal;
  private final String type;

  public ControllerOption(String name, String description, String type, String defaultVal, String... aliases) {
    this.name = name;
    this.description = description;
    this.defaultVal = defaultVal;
    this.type = type;
    this.aliases = Arrays.asList(aliases);
  }

  public String getName() {
    return name;
  }

  public String getDefaultVal() {
    return defaultVal;
  }

  public String getType() {
    return type;
  }

  public List<String> getAliases() {
    return aliases;
  }

  public String getAliasesString() {
    return aliases.stream().reduce("", (a, b) -> a + b + " ");
  }

  public boolean matchesAlias(String alias) {
    return aliases.contains(alias);
  }

  public String getDescription() {
    return description;
  }
}

