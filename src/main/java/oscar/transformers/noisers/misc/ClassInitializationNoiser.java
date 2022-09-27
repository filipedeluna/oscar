package oscar.transformers.noisers.misc;

import oscar.controller.noise.NoiseLocation;
import oscar.engine.body.JimpleBodyBox;
import oscar.transformers.JimpleTransformer;
import oscar.transformers.noisers.NoiserTag;
import soot.Unit;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JInvokeStmt;

import java.util.Set;

public final class ClassInitializationNoiser extends JimpleTransformer {

  public ClassInitializationNoiser() {
    super("cin", ClassInitializationNoiser.class);
    this.routine = this::routine;
  }

  private void routine(JimpleBodyBox bodyBox) {
    // Check if method is an initialization method
    if (!Set.of("<clinit>", "<init>").contains(bodyBox.body().getMethod().getName()))
      return;

    int statementCount = 1;
    Unit stmt = bodyBox.body().getFirstNonIdentityStmt();
    while (bodyBox.body().getUnits().getSuccOf(stmt) != null ) {
      stmt = bodyBox.body().getUnits().getSuccOf(stmt);

      // Ignore statements added via oscar instrumentation
      if (!stmt.hasTag(NoiserTag.OSCAR_INSTRUMENTED.getName()))
        statementCount++;
    }

    // If init body is empty, do not add useless noise
    if (statementCount < 3)
      return;

    // Insert noise after first invoke (super()) statement
    for (Unit unit : bodyBox.body().getUnits())
      if (unit instanceof JInvokeStmt && ((JInvokeStmt) unit).getInvokeExpr() instanceof SpecialInvokeExpr) {
        bodyBox.body()
               .getUnits()
               .insertAfter(bodyBox.generator().Statement.noise(NoiseLocation.BEFORE_CLASS_INITIALIZATION), unit);
        break;
      }
  }
}



