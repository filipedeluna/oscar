package oscar.utils.logger;

import java.util.ArrayList;
import java.util.logging.*;

public final class LoggerFactory {
  private static final ArrayList<Logger> loggers = new ArrayList<>();
  private static final ConsoleHandler consoleHandler = new ConsoleHandler();
  static {
    consoleHandler.setFormatter(new LoggerFormatter());
    consoleHandler.setLevel(Level.ALL);
  }

  private static Level defaultLevel = Level.INFO;

  // private static FileHandler fileHandler;

  /*
  public static void initialize() {
    // Configure console handler
    // Configure file handler
    try {
      fileHandler = new FileHandler("log.txt", false);
      fileHandler.setFormatter(new LogFormatter());
    } catch (IOException | SecurityException e) {
      throw new RuntimeException("Failed to write to log file location in client.properties file.", e);
    }
  }
    */

  public static Logger getInstance(Class<?> clazz) {
    Logger logger = Logger.getLogger(clazz.getName());
    logger.setUseParentHandlers(false);
    logger.setLevel(defaultLevel);

    logger.addHandler(consoleHandler);
    //logger.addHandler(fileHandler);

    loggers.add(logger);

    return logger;
  }

  public static void setLevel(Level level) {
    defaultLevel = level;
    loggers.forEach(l -> l.setLevel(level));
  }
}