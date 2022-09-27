package oscar.engine.generators;

import soot.*;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.internal.JimpleLocal;

import java.util.Collections;

public class ConversionGenerator{
  private final LocalGenerator localGenerator;

  public ConversionGenerator(LocalGenerator localGenerator) {
    this.localGenerator = localGenerator;
  }

  private JAssignStmt valueOf(String originTypeClass, String valueOfMethodSig, Value valueLocal) {
    JimpleLocal local = localGenerator.fromType(RefType.v(originTypeClass));
    SootClass sootClass = Scene.v().getSootClass(originTypeClass);

    SootMethod longValueOf = sootClass.getMethod(valueOfMethodSig);
    JStaticInvokeExpr valueOfLong = new JStaticInvokeExpr(longValueOf.makeRef(), Collections.singletonList(valueLocal));

    return new JAssignStmt(local, valueOfLong);
  }

  public JAssignStmt longValueOf(Value valueLocal) {
    return valueOf("java.lang.Long", "java.lang.Long valueOf(long)", valueLocal);
  }
}
