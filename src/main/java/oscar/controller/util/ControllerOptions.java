package oscar.controller.util;

import oscar.Main;
import oscar.controller.noise.NoiseLocation;
import oscar.controller.noise.NoiseCategory;
import oscar.controller.util.output.*;
import oscar.utils.logger.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class ControllerOptions {
  private static final Logger logger = LoggerFactory.getInstance(ControllerOptions.class);

  private static final List<ControllerOption> CONTROLLER_OPTIONS = Arrays.asList(
      new ControllerOption("InjectedArgs", "Inject arguments into the program", "String", "", "-a", "--args"),
      // new ControllerOption("ConfigFile", "Set config file location to load", "String", "", "-c", "--config_file"),
      new ControllerOption("ConsoleOutput", "Output trace to console", "Flag", "False", "-co", "--console-output"),
      new ControllerOption("FileOutput", "Output trace to a file", "Flag", "False", "-fo", "--file-output"),
      new ControllerOption("LazyFileOutput", "Lazily output trace to a file", "Flag", "False", "-lfo", "--lazy-file-output"),
      new ControllerOption("MaxNoiseIntensity", "Set maximum noise intensity", "Long", "10", "-M", "--max-noise-intensity"),
      new ControllerOption("MinNoiseIntensity", "Set minimum noise intensity", "Long", "0", "-m", "--min-noise-intensity"),
      new ControllerOption("NoiseProbability", "Probability of noise being triggered", "Float", "1", "-p", "--noise-probability"),
      new ControllerOption("DisableNoise", "Disable all noise", "Flag", "False", "-d", "--disable-noise"),
      new ControllerOption("DisableNoiseTracing", "Disable all noise tracing", "Flag", "False", "-dt", "--disable-tracing"),
      new ControllerOption("DisablePreNoiseTracing", "Disable pre-noise tracing", "Flag", "False", "-d1", "--disable-pre-noise-trace"),
      new ControllerOption("DisablePostNoiseTracing", "Disable post-noise tracing", "Flag", "False", "-d2", "--disable-post-noise-trace"),
      new ControllerOption("YieldMode", "Set noise type to yield.", "Flag", "False", "-y", "--yield"),
      new ControllerOption("NoiseCategories", "Set active noise placement categories.", "List<String>", "{}", "-nc", "--noise-categories"),
      new ControllerOption("NoiseLocations", "Set active noise locations.", "List<String>", "{}", "-nl", "--noise-locations"),
      new ControllerOption("PrintNoiseLocations", "Print all noise placement locations.", "Flag", "-", "-pnl", "--print-noise-locations"),
      new ControllerOption("Version", "Print OSCAR version.", "Flag", "-", "-v", "--version"),
      new ControllerOption("Verbose", "Enable full logging.", "Flag", "False", "-vb", "--verbose"),
      new ControllerOption("DisableSkippedLocations", "Disable logging of skipped noise locations.", "Flag", "False", "-dsl", "--disable-skipped-logging"),
      new ControllerOption("Quiet", "Disable logging.", "Flag", "False", "-q", "--quiet"),
      new ControllerOption("Help", "Print Help.", "Flag", "False", "-h", "--help")
  );

  public String InjectedArgs = "";
  public String ConfigFile = null;
  public ControllerOutput ControllerOutput = null;
  public Long MaxNoiseIntensity = 10L;
  public Long MinNoiseIntensity = 0L;
  public boolean DisableNoise = false;
  public boolean DisablePostNoiseTracing = false;
  public boolean DisablePreNoiseTracing = false;
  public final HashSet<NoiseLocation> NoiseLocations = new HashSet<>();
  public final HashSet<NoiseCategory> noiseCategories = new HashSet<>();
  public boolean DisableSkippedLocations = false;
  public float NoiseProbability = 1;

  public boolean Verbose = false;
  public boolean YieldMode = false;
  public boolean Quiet = false;

  public static ControllerOptions parse(String[] argv) {
    ControllerOptions options = new ControllerOptions();

    for (int i = 0; i < argv.length; i++) {
      String arg = argv[i];

      List<ControllerOption> matchingArgs = CONTROLLER_OPTIONS.stream()
                                                              .filter(c -> c.matchesAlias(arg))
                                                              .collect(Collectors.toList());

      if (matchingArgs.size() > 1)
        throw new RuntimeException("Argument '" + arg + "' matched  more than one option.");

      if (matchingArgs.size() == 0)
        throw new RuntimeException("Argument '" + arg + "' matches no known options, use --help or -h.");

      switch (matchingArgs.get(0).getName()) {
        case "InjectedArgs":
          // Read all injected args
          while (i + 1 < argv.length && !argv[i + 1].startsWith("-"))
            options.InjectedArgs += argv[++i] + " ";
          break;
        case "ConfigFile":
          options.ConfigFile = argv[i + 1];
          ControllerConfigFile.readFile(options);
          i++;
          break;
        case "FileOutput":
          if (options.ControllerOutput != null)
            throw new RuntimeException("Output method already set.");

          options.ControllerOutput = new RegularFileOutput();
          break;
        case "LazyFileOutput":
          if (options.ControllerOutput != null)
            throw new RuntimeException("Output method already set.");

          options.ControllerOutput = new LazyFileOutput();
          break;
        case "ConsoleOutput":
          if (options.ControllerOutput != null)
            throw new RuntimeException("Output method already set.");

          options.ControllerOutput = new ConsoleOutput();
          break;
        case "NoiseProbability":
          if (options.DisableNoise)
            throw new RuntimeException("Noise disabled.");

          options.NoiseProbability = parseFloat(argv[i + 1]);
          i++;

          if (options.NoiseProbability < 0 || options.NoiseProbability > 1)
            throw new RuntimeException("Invalid value for 'noise probability', must be higher or equal to 0 and lower or equal to 1.");

          break;
        case "MaxNoiseIntensity":
          if (options.DisableNoise)
            throw new RuntimeException("Noise disabled.");

          options.MaxNoiseIntensity = parseLong(argv[i + 1]);
          i++;
          if (options.MaxNoiseIntensity < 0)
            throw new RuntimeException("Invalid value for 'max noise intensity', must be higher or equal to 0.");

          if (options.MaxNoiseIntensity < options.MinNoiseIntensity)
            throw new RuntimeException("Invalid value for 'max noise intensity', must be lower or equal to 'min-noise-intensity'.");
          break;
        case "MinNoiseIntensity":
          if (options.DisableNoise)
            throw new RuntimeException("Noise disabled.");

          options.MinNoiseIntensity = parseLong(argv[i + 1]);
          i++;

          if (options.MinNoiseIntensity < 0)
            throw new RuntimeException("Invalid value for 'min noise intensity', must be higher or equal to 0.");

          if (options.MinNoiseIntensity > options.MaxNoiseIntensity)
            throw new RuntimeException("Invalid value for 'min noise intensity', must be lower or equal to 'max noise intensity'.");
          break;
        case "YieldMode":
          if (options.DisableNoise)
            throw new RuntimeException("Noise disabled.");

          options.YieldMode = true;
          break;
        case "NoiseCategories":
          options.noiseCategories.clear();

          // Read all noise placements
          while (i + 1 < argv.length && !argv[i + 1].startsWith("-"))
            options.noiseCategories.add(NoiseCategory.fromString(argv[++i]));
          break;
        case "NoiseLocations":
          options.NoiseLocations.clear();

          // Read all noise locations and add all of their respective noise types
          while (i + 1 < argv.length && !argv[i + 1].startsWith("-"))
            options.NoiseLocations.add(NoiseLocation.fromString(argv[++i]));

          break;
        case "PrintNoiseLocations":
          printNoiseLocations();
          System.exit(0);
          break;
        case "DisableNoise":
          options.DisableNoise = true;
          break;
        case "DisablePreNoiseTracing":
          options.DisablePreNoiseTracing = true;
          break;
        case "DisablePostNoiseTracing":
          options.DisablePostNoiseTracing = true;
          break;
        case "Verbose":
          if (options.Quiet)
            continue;

          logger.info("Setting logger level to verbose (FINEST).");
          LoggerFactory.setLevel(Level.FINEST);
          break;
        case "DisableSkippedLocations":
          options.DisableSkippedLocations = true;
          break;
        case "Quiet":
          if (options.Verbose)
            continue;

          logger.info("Setting logger level to quiet (OFF).");
          LoggerFactory.setLevel(Level.OFF);
          break;
        case "Version":
          System.out.println("OSCAR " + Main.VERSION);
          System.exit(0);
          break;
        case "Help":
          printHelp();
          System.exit(0);
          break;
      }
    }

    if (options.MinNoiseIntensity > options.MaxNoiseIntensity)
      throw new RuntimeException("Minimum sleep length should be lower than maximum.");

    if (options.MinNoiseIntensity + options.MaxNoiseIntensity == 0)
      options.DisableNoise = true;

    return options;
  }

  private static void printHelp() {
    System.out.println("Usage:");
    System.out.println("\tjava [java_options] <mainclass> [oscar_controller_options]");
    System.out.println("\t\t(to execute a class)");
    System.out.println("\tor: java [java_options] -jar <mainclass> [oscar_controller_options]");
    System.out.println("\t\t(to execute a jar file)");

    System.out.println();
    System.out.println("OSCAR controller options include:");

    for (ControllerOption option : CONTROLLER_OPTIONS)
      System.out.printf(
          "\t%-25s\t%-15s\t%-10s\t%s\n",
          option.getAliasesString(),
          option.getType(),
          option.getDefaultVal(),
          option.getDescription()
      );

    System.out.println();

    System.out.println("OSCAR Noise Injector 2022");
  }

  private static void printNoiseLocations() {
    System.out.println("Available noise placement categories:");

    System.out.printf(
        "\t%-25s\t%-25s\n",
        "Category",
        "Shorthand code"
    );

    System.out.printf(
        "\t%-25s\t%-25s\n",
        "-------------------------",
        "-------------------------"
    );

    for (NoiseCategory nc : NoiseCategory.values())
      System.out.printf(
          "\t%-25s\t%-25s\n",
          nc.name().replace("_", " "),
          nc.getShorthand()
      );

    System.out.println();

    System.out.println("Possible noising placement locations:");

    System.out.printf(
        "\t%-25s\t%-35s\t%-25s\n",
        "Category",
        "Location",
        "Shorthand code"
    );

    System.out.printf(
        "\t%-25s\t%-35s\t%-25s\n",
        "-------------------------",
        "-------------------------",
        "-------------------------"
    );

    for (NoiseLocation np : NoiseLocation.values())
      System.out.printf(
          "\t%-25s\t%-35s\t%-25s\n",
          np.getCategory().name().replace("_", " "),
          np.name().replace("_", " "),
          np.getShorthand()
      );
  }

  private static int parseInt(String arg) {
    try {
      return Integer.parseInt(arg);
    } catch (NumberFormatException e) {
      throw new RuntimeException("Invalid argument for option '" + arg + "', expected an integer.");
    }
  }

  private static long parseLong(String arg) {
    try {
      return Long.parseLong(arg);
    } catch (NumberFormatException e) {
      throw new RuntimeException("Invalid argument for option '" + arg + "', expected a long.");
    }
  }

  private static float parseFloat(String arg) {
    try {
      return Float.parseFloat(arg);
    } catch (NumberFormatException e) {
      throw new RuntimeException("Invalid argument for option '" + arg + "', expected a float.");
    }
  }
}
