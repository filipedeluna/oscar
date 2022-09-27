package oscar.controller.util.output;

import oscar.utils.logger.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class LazyFileOutput extends FileOutput {
  private static final Logger logger = LoggerFactory.getInstance(LazyFileOutput.class);

  private final ArrayList<String> buffer = new ArrayList<>();

  public LazyFileOutput() {
    super();
  }

  @Override
  public void write(String output) {
    logger.fine("Writing to buffer.");

    if (output == null)
      throw new RuntimeException("Null string fed to file output.");

    synchronized (buffer) {
      buffer.add(output + "\n");
    }
  }

  @Override
  public void terminate() {
    if (writer == null)
      throw new RuntimeException("Writer is null.");

    try {
      synchronized (buffer) {
        for (String s : buffer) {
          if (s == null)
            throw new RuntimeException("Null string fed to file output buffer.");

          writer.write(s);
        }
      }
      writer.flush();
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException("Failed to write to file '" + filepath + "'.", e);
    }
  }
}
