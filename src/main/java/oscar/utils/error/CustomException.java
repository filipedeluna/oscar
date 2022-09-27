package oscar.utils.error;

import java.util.logging.Logger;

public final class CustomException extends Exception {
  public CustomException(Logger logger, String text) {
    super(log(logger, text));
  }

  private static String log(Logger logger, String text) {
    logger.fine("[" + logger.getName() + "]: " + text);

    return text;
  }
}