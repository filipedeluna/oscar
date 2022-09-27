package oscar.controller.util;

import oscar.utils.logger.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

public abstract class ControllerConfigFile {
  private static final Logger logger = LoggerFactory.getInstance(ControllerConfigFile.class);

  public static Long parseLong(Properties props, String property, String defaultValue) {
    try {
      return Long.parseLong(props.getProperty(property, defaultValue));
    } catch (NumberFormatException e) {
      throw new RuntimeException("Invalid property value for '" + property + "'.", e);
    }
  }

  public static void readFile(ControllerOptions options) {
    logger.info("Looking for config file in location '" + options.ConfigFile + "'.");

    Properties props = loadFile(options.ConfigFile);

    logger.info("Found client.properties file. Reading properties.");

    /*
    // Read basic properties
    options.MaxSleepLength = ControllerConfigFile.parseLong(props, "max_sleep_length", "400");
    options.MinSleepLength = ControllerConfigFile.parseLong(props, "min_sleep_length", "0");

    // Read noise location file, if provided
    String noiseLocationsFile = props.getProperty("noise_locations_file");
    if (noiseLocationsFile != null) {
      HashMap<Long, SleepNoise> readNoiseLocations = FileControllerOutput.read(noiseLocationsFile);
      options.NoiseLocations.putAll(readNoiseLocations);
    }

    // Read which noise placements
    String noisePlacements = props.getProperty("noise_placements");
    if (noisePlacements != null) {
      options.ActiveNoisePlacements.clear();
      options.ActiveNoisePlacements.addAll(Arrays.stream(noisePlacements.split(","))
                                         .map(NoisePlacement::fromString)
                                         .collect(Collectors.toSet()));
    }
    */
  }

  private static Properties loadFile(String location) {

    try {
      Properties props = new Properties();
      props.load(new FileInputStream(location));
      return props;
    } catch (IOException e) {
      throw new RuntimeException("Failed to find client.properties file in '" + location + "'.", e);
    }
  }
}