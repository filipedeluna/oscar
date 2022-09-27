package oscar;

import oscar.controller.util.ControllerOption;
import oscar.engine.Engine;
import oscar.utils.logger.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Main {
  public static final String VERSION = "0.2";

  private static final List<ControllerOption> ENGINE_OPTIONS = Arrays.asList(
      new ControllerOption("Verbose", "Enable full logging.", "Flag", "False", "-vb", "--verbose"),
      new ControllerOption("Blacklist", "Set blacklisted classes by prefix (these will not be noised)", "List", Engine.BlacklistedClasses.toString(), "-b", "--blacklist"),
      new ControllerOption("Jar", "Inject a program as a JAR file.", "Flag", "False", "-j", "--jar"),
      new ControllerOption("Help", "Print Help.", "Flag", "False", "-h", "--help"),
      new ControllerOption("Version", "Print Version.", "Flag", "False", "-v", "--version")
  );

  public static void main(String[] argv) {
    if (argv.length < 3) {
      System.out.println("Invalid number of arguments, expected at least 3. Use --help or -h for help.");
      System.exit(1);
    }

    // Process arguments
    for (int i = 3; i < argv.length; i++) {
      String arg = argv[i];

      List<ControllerOption> matchingArgs = ENGINE_OPTIONS.stream()
                                                          .filter(c -> c.matchesAlias(arg))
                                                          .collect(Collectors.toList());

      if (matchingArgs.size() > 1)
        throw new RuntimeException("Argument '" + arg + "' matched  more than one option.");

      if (matchingArgs.size() == 0)
        throw new RuntimeException("Argument '" + arg + "' matches no known options, use --help or -h.");

      switch (matchingArgs.get(0).getName()) {
        case "Verbose":
          LoggerFactory.setLevel(Level.ALL);
          break;
        case "Jar":
          Engine.JarMode = true;
          break;
        case "Blacklist":
          Engine.BlacklistedClasses = new ArrayList<>();

          while (i + 1 < argv.length && !argv[i + 1].contains("-"))
            Engine.BlacklistedClasses.add(argv[++i]);
          break;
        case "Version":
          System.out.println("OSCAR " + Main.VERSION);
          System.exit(0);
          break;
        case "Help":
          System.out.println("Usage:");
          System.out.println("\tjava [java_options] oscar <targetfile> <mainclass> <outputdirectory> [oscar_options]");
          System.out.println("OSCAR options include:");
          for (ControllerOption option : ENGINE_OPTIONS)
            System.out.printf(
                "\t%-25s\t%-15s\t%-10s\t%s\n",
                option.getAliasesString(),
                option.getType(),
                option.getDefaultVal(),
                option.getDescription()
            );

          System.exit(0);
          break;
      }
    }

    String targetFile = argv[0];
    String mainClass = argv[1];
    String outputDirectory = argv[2];

    // Init soot
    Engine engine = new Engine(targetFile, mainClass, outputDirectory);
    engine.run();

    System.exit(0);
  }
}