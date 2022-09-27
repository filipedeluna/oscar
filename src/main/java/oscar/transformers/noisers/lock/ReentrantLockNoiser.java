package oscar.transformers.noisers.lock;

import oscar.controller.noise.NoiseLocation;
import oscar.engine.body.JimpleBodyBox;
import oscar.transformers.JimpleTransformer;
import soot.Unit;
import soot.jimple.JimpleBody;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JVirtualInvokeExpr;

import java.util.List;
import java.util.stream.Collectors;

public final class ReentrantLockNoiser extends JimpleTransformer {

  public ReentrantLockNoiser() {
    super("rln", ReentrantLockNoiser.class, ReentrantLockNoiser::routine);
  }

  private static void routine(JimpleBodyBox bodyBox) {
    // Find calls to reentrant lock locks and unlocks
    List<JInvokeStmt> reentrantLockCalls = getReentrantLockCalls(bodyBox.body());

    // Create statement to insert sleep noise before and after sync blocks
    for (JInvokeStmt lockCall : reentrantLockCalls) {
      if (getInvokeExprMethodName(lockCall).equals("lock"))
        bodyBox.body()
            .getUnits()
            .insertBefore(bodyBox.generator().Statement.noise(NoiseLocation.BEFORE_REENTRANT_LOCK_LOCK), lockCall);
      else if (getInvokeExprMethodName(lockCall).equals("unlock")) {
        List<Unit> units = bodyBox.generator().Statement.noise(NoiseLocation.AFTER_REENTRANT_LOCK_UNLOCK);
        bodyBox.body().getUnits().insertAfter(units, lockCall);
      } else
        throw new RuntimeException("Invalid reentrant lock call statement");
    }
  }

  private static List<JInvokeStmt> getReentrantLockCalls(JimpleBody body) {
    return body.getUnits()
               .stream()
               .filter(JInvokeStmt.class::isInstance)
               .map(JInvokeStmt.class::cast)
               .filter(s -> s.getInvokeExpr() instanceof JVirtualInvokeExpr)
               .filter(s -> getInvokeExprClassName(s).equals("java.util.concurrent.locks.ReentrantLock"))
               .filter(s -> List.of("lock", "unlock").contains(getInvokeExprMethodName(s)))
               .collect(Collectors.toList());
  }

  private static String getInvokeExprMethodName(JInvokeStmt expr) {
    return expr.getInvokeExpr().getMethodRef().getName();
  }

  private static String getInvokeExprClassName(JInvokeStmt expr) {
    return expr.getInvokeExpr().getMethodRef().getDeclaringClass().getName();
  }
}
