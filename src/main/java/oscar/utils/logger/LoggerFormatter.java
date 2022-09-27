package oscar.utils.logger;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public final class LoggerFormatter extends Formatter {
  private static final String format = "[OSCAR][%3$s][%2$s][%1$tF %1$tT]: %4$s %n";

  @Override
  public synchronized String format(LogRecord lr) {
    return String.format(
        format,
        new Date(lr.getMillis()),
        lr.getLoggerName(),
        lr.getLevel().getLocalizedName(),
        lr.getMessage()
    );
  }
}