package oscar.controller.util.output;

import oscar.utils.logger.LoggerFactory;

import java.io.IOException;
import java.util.logging.Logger;

public class RegularFileOutput extends FileOutput {
  private static final Logger logger = LoggerFactory.getInstance(RegularFileOutput.class);

  public RegularFileOutput() {
    super();
  }

  @Override
  public synchronized void write(String output) {
    logger.fine("Writing to file '" + filepath + "'.");

    try {
      writer.write(output + "\n");
    } catch (IOException e) {
      throw new RuntimeException("Failed to write to file '" + filepath + "'.", e);
    }
  }


  /* // TODO leftover for a reader
  public List<String> read() {
    logger.info("Reading input file '" + fileLocation + "'.");

    ArrayList<String> list = new ArrayList<>();
    while (scanner.hasNext())
      list.add(scanner.nextLine());

    return list;
  }
  */

  @Override
  public void terminate() {
    if (writer != null)
      try {
        writer.close();
      } catch (IOException e) {
        throw new RuntimeException("Failed to close writer.");
      }

    // if (scanner != null)
    //  scanner.close();
  }
}
