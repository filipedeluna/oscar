package oscar.transformers.noisers.sync;

import oscar.controller.noise.NoiseLocation;
import oscar.engine.body.JimpleBodyBox;
import oscar.transformers.JimpleTransformer;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;

import java.util.List;
import java.util.stream.Collectors;

public final class SynchronizedBlockNoiser extends JimpleTransformer {

  public SynchronizedBlockNoiser() {
    super("sbn", SynchronizedBlockNoiser.class, SynchronizedBlockNoiser::routine);
  }

  private static void routine(JimpleBodyBox bodyBox) {
    // Find monitor calls for synchronized blocks
    List<Unit> monitorCalls = getMonitorCalls(bodyBox.body());

    // Create statement to insert sleep noise before and after sync blocks
    for (Unit monitorCall : monitorCalls) {
      if (monitorCall instanceof JEnterMonitorStmt)
        bodyBox.body()
               .getUnits()
               .insertBefore(bodyBox.generator().Statement.noise(NoiseLocation.BEFORE_SYNC_BLOCK), monitorCall);
      else if (monitorCall instanceof JExitMonitorStmt) {
        List<Unit> units = bodyBox.generator().Statement.noise(NoiseLocation.AFTER_SYNC_BLOCK);
        bodyBox.body().getUnits().insertAfter(units, monitorCall);
      } else
        throw new RuntimeException("Invalid monitor call statement");
    }
  }

  private static List<Unit> getMonitorCalls(JimpleBody body) {
    return body.getUnits()
               .stream()
               .filter(s -> s instanceof JEnterMonitorStmt || s instanceof JExitMonitorStmt)
               .collect(Collectors.toList());
  }
}
