package oscar.transformers.injectors;

import oscar.engine.body.JimpleBodyBox;
import oscar.transformers.JimpleTransformer;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JInvokeStmt;

import java.util.List;

public class ExitCaptureInjector extends JimpleTransformer {
  public ExitCaptureInjector() {
    super( "eci", ExitCaptureInjector.class, ExitCaptureInjector::routine);
  }

  private static void routine(JimpleBodyBox body) {
    // Cycle all methods to find calls to system.exit
    for (Unit unit : body.body().getUnits()) {
      if (!(unit instanceof JInvokeStmt))
        continue;

      InvokeExpr invokeExpr = ((JInvokeStmt) unit).getInvokeExpr();
      SootMethod invokeMethod = invokeExpr.getMethod();

      if (!invokeMethod.getDeclaringClass().getName().equals("java.lang.System"))
        continue;

      if (!invokeMethod.getName().equals("exit"))
        continue;

      SootMethodRefImpl methodRef = new SootMethodRefImpl(
          Scene.v().getSootClass("oscar.controller.Controller"),
          "exit",
          List.of(IntType.v()),
          VoidType.v(),
          true
      );

      invokeExpr.setMethodRef(methodRef);
    }
  }
}