package oscar.transformers.noisers.sync;

import oscar.controller.noise.NoiseLocation;
import oscar.engine.body.JimpleBodyBox;
import oscar.transformers.JimpleTransformer;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;

import java.util.List;
import java.util.stream.Collectors;

public final class SynchronizedMethodCallNoiser extends JimpleTransformer {
  public SynchronizedMethodCallNoiser() {
    super( "smcn", SynchronizedMethodCallNoiser.class, SynchronizedMethodCallNoiser::routine);
  }

  public static void routine(JimpleBodyBox body) {
    // Find invocations of synchronized methods
    List<JInvokeStmt> syncMethodInvocations = getSyncMethodInvocations(body.body());

    // Create statement to insert sleep noise before and after sync blocks
    for (Unit invocation : syncMethodInvocations) {
      body.body().getUnits().insertBefore(body.generator().Statement.noise(NoiseLocation.BEFORE_SYNC_METHOD_CALL), invocation);
      body.body().getUnits().insertAfter(body.generator().Statement.noise(NoiseLocation.AFTER_SYNC_METHOD_CALL), invocation);
    }
  }

  private static List<JInvokeStmt> getSyncMethodInvocations(JimpleBody body) {
    return body.getUnits().stream()
               .filter(JInvokeStmt.class::isInstance)
               .map(box -> ((JInvokeStmt) box))
               .filter(box -> box.getInvokeExpr().getMethod().isSynchronized())
               .filter(box -> !box.getInvokeExpr().getMethod().getDeclaringClass().getName().equals("java.lang.Thread"))
               .collect(Collectors.toList());
  }
}
