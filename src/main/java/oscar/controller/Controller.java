package oscar.controller;

import oscar.controller.noise.NoiseLocation;
import oscar.controller.util.ControllerOptions;
import oscar.utils.logger.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public final class Controller {
  private static final Logger logger = LoggerFactory.getInstance(Controller.class);
  private static final Random rand = new Random();

  private static ControllerOptions options;
  private static AtomicInteger noiseStatementCallCount = new AtomicInteger(0);
  private static AtomicInteger noiseTriggeredCount = new AtomicInteger(0);

  public static String[] start(String[] argv) {
    logger.info("Starting OSCAR noising controller.");
    logger.info("Parsing arguments.");

    options = ControllerOptions.parse(argv);

    logger.info("Arguments parsed.");

    return options.InjectedArgs.split(" ");
  }

  public synchronized static void end() {
    if (options.ControllerOutput != null)
      options.ControllerOutput.terminate();

    logger.info("Noise function invoked " + noiseStatementCallCount + " times.");
    logger.info("Noise triggered " + noiseTriggeredCount + " times.");
    logger.info("OSCAR noising controller routine ended.");
    System.exit(0);
  }

  /**
   * Make the injected program sleep
   *
   * @param noiseLoc instrumented location type
   * @param uuid     instrumented location generated uuid
   */
  public static void noise(NoiseLocation noiseLoc, String uuid) {
    // Increment noise statement call counter
    noiseStatementCallCount.incrementAndGet();

    // Check if the controller has been initialized. This can occur if noise is inserted into static blocks.
    if (options == null)
      return;

    // Check if location enabled
    if (!options.noiseCategories.contains(noiseLoc.getCategory()) && !options.NoiseLocations.contains(noiseLoc)) {
      if (!options.DisableSkippedLocations)
        logger.fine("[SLEEP] Skipping noise location '" + noiseLoc.name() + "'.");
      return;
    }

    long threadID = Thread.currentThread().getId();

    // Write pre-noise location trace
    if (!options.DisablePreNoiseTracing && options.ControllerOutput != null) {
      options.ControllerOutput.write(threadID + " " + uuid);

      if (!options.Quiet)
        logger.fine("[SIGNAL][PRE-NOISE][" + noiseLoc.getCategory() + "]" + "[" + uuid + "]");
    }

    // Do not noise if noise is disabled
    if (!options.DisableNoise) {
      // Compute probability for noise if probability activated
      if (options.NoiseProbability == 1 || rand.nextFloat() < options.NoiseProbability) {
        // Get a random noise intensity
        long noiseIntensity = options.MinNoiseIntensity;
        noiseIntensity += Math.abs(rand.nextLong() % (1 + options.MaxNoiseIntensity - options.MinNoiseIntensity));

        // Sleep for a determined amount of time
        try {
          if (!options.YieldMode) {
            logger.finest("[SLEEP]" +
                              "[" + noiseLoc.getCategory().name() + "]" +
                              "[" + noiseLoc.name() + "]" +
                              "[" + uuid + "]: "
                              + noiseIntensity + " MS."
            );

            Thread.sleep(noiseIntensity);
          } else {
            logger.finest("[" + "Yield" + "]" +
                              "[" + noiseLoc.getCategory().name() + "]" +
                              "[" + noiseLoc.name() + "]" +
                              "[" + uuid + "]: "
                              + noiseIntensity + " times."
            );

            for (int i = 0; i < noiseIntensity; i++)
              Thread.yield();
          }

          noiseTriggeredCount.incrementAndGet();
        } catch (InterruptedException e) {
          throw new RuntimeException("OSCAR sleep statement was interrupted.", e);
        }
      }
    }

    // Write post-noise location trace
    if (!options.DisablePostNoiseTracing && options.ControllerOutput != null) {
      options.ControllerOutput.write(threadID + " " + uuid);

      if (!options.Quiet)
        logger.fine("[SIGNAL][POST-NOISE][" + noiseLoc.getCategory() + "]" + "[" + uuid + "]");
    }
  }

  // Capture exit codes
  public static void exit(int code) {
    if (code == 0) {
      logger.info("Detected exit code 0. Exiting gracefully.");
      end();
      System.exit(0);
    } else {
      logger.info("Detected exit code " + code + ". Exiting gracefully.");
      end();
      System.exit(code);
    }
  }

  // Capture exit codes
  public static void exception(Exception exception) {
    logger.info("Caught exception in instrumented program:");

    for (StackTraceElement trace : exception.getStackTrace())
      logger.info("\t" + trace.toString());

    logger.info("Exiting gracefully.");
    end();
    System.exit(1);
  }
}
