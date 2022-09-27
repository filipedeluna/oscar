package oscar.controller.util.output;

import oscar.controller.Controller;
import oscar.utils.logger.LoggerFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ConsoleOutput implements ControllerOutput {
  public ConsoleOutput() {
  }

  @Override
  public void write(String output) {
    System.out.println(output);
  }

  @Override
  public void terminate() {
    System.out.flush();
  }
}
