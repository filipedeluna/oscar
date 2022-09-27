package oscar.controller.util.output;

import oscar.utils.logger.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.logging.Logger;

public abstract class FileOutput implements ControllerOutput {
  private static final Logger logger = LoggerFactory.getInstance(FileOutput.class);

  protected final FileWriter writer;
  // protected final Scanner scanner;
  protected final String filepath;

  public FileOutput() {
    logger.info("Initializing File controller output.");

    this.filepath = "oscar_output" + File.separator + "oscar_output_" + Instant.now() + ".txt";
    File file = new File(filepath);

    logger.fine("Creating directories for output file'" + filepath + "'.");

    try {
      Files.createDirectories(file.toPath().getParent());
    } catch (IOException e) {
      logger.info("Directories for file '" + filepath + "' seem to already exist.");
    }

    logger.fine("Creating controller output file '" + filepath + "'.");
    try {
      Files.createFile(file.toPath());
    } catch (IOException e) {
      logger.info("File '" + filepath + "' seems to already exist.");
    }

    logger.fine("Opening file '" + filepath + "'.");
    try {
      // scanner = new Scanner(file);
      writer = new FileWriter(file);
    } catch (IOException e) {
      throw new RuntimeException("Failed to open file '" + filepath + "'.", e);
    }
  }
}
