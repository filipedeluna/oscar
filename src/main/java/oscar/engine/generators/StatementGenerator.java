package oscar.engine.generators;

import oscar.controller.noise.NoiseLocation;
import oscar.transformers.noisers.NoiserTag;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StatementGenerator {
  private final LocalGenerator localGenerator;
  private final JimpleBody body;

  public StatementGenerator(LocalGenerator localGenerator, JimpleBody body) {
    this.localGenerator = localGenerator;
    this.body = body;
  }

  public JimpleLocal instantiateClass(String className, List<Value> initArgs) {
    // Add a new local
    SootClass sootClass = Scene.v().getSootClass(className);
    JimpleLocal local = localGenerator.fromClass(sootClass);
    JNewExpr newExp = new JNewExpr(RefType.v(sootClass));

    // Invoke the init
    SootMethod initMethod = sootClass.getMethod("void <init>()");
    JSpecialInvokeExpr classInvokeExpr = new JSpecialInvokeExpr(local, initMethod.makeRef(), initArgs);

    appendUnit(new JInvokeStmt(classInvokeExpr));
    appendUnit(new JAssignStmt(local, newExp));

    return (JimpleLocal) classInvokeExpr.getBase();
  }

  public Stmt virtualInvoke(JimpleLocal refLocal, String methodClass, String methodSignature, List<Value> args) {
    SootClass sootClass = Scene.v().getSootClass(methodClass);
    SootMethod method = sootClass.getMethod(methodSignature);

    JVirtualInvokeExpr invokeExpr =
        new JVirtualInvokeExpr(refLocal, method.makeRef(), args);

    if (methodSignature.startsWith("void"))
      return new JInvokeStmt(invokeExpr);
    else {
      JimpleLocal resultLocal = localGenerator.fromType(method.getReturnType());
      return new JAssignStmt(resultLocal, invokeExpr);
    }
  }

  public Stmt staticInvoke(String methodClass, String methodSignature, List<Value> args) {
    SootClass sootClass = Scene.v().getSootClass(methodClass);
    SootMethod method = sootClass.getMethod(methodSignature);

    JStaticInvokeExpr invokeExpr =
        new JStaticInvokeExpr(method.makeRef(), args);

    if (methodSignature.startsWith("void"))
      return new JInvokeStmt(invokeExpr);
    else {
      JimpleLocal resultLocal = localGenerator.fromType(method.getReturnType());
      return new JAssignStmt(resultLocal, invokeExpr);
    }
  }

  public JIdentityStmt identity(JimpleLocal local, ParameterRef paramRef) {
    return new JIdentityStmt(local, paramRef);
  }

  public List<Unit> printf(String message, List<Value> args) {
    ArrayList<Unit> statements = new ArrayList<>();

    // Print the random length
    JimpleLocal printLocal = localGenerator.fromType(RefType.v("java.io.PrintStream"));
    SootField sysOutField = Scene.v().getField("<java.lang.System: java.io.PrintStream out>");
    statements.add(new JAssignStmt(printLocal, Jimple.v().newStaticFieldRef(sysOutField.makeRef())));

    // Create array for print parameters
    JAssignStmt arrayCreationStmt = assignArray("java.lang.Object", 1, args.size());
    JimpleLocal arrayLocal = (JimpleLocal) arrayCreationStmt.getLeftOp();
    statements.add(arrayCreationStmt);

    // Assign args values to array
    for (int i = 0; i < args.size(); i++) {
      JArrayRef arrayForPrint = new JArrayRef(arrayLocal, IntConstant.v(i));
      statements.add(new JAssignStmt(arrayForPrint, args.get(i)));
    }

    // Print
    SootClass printStreamClass = Scene.v().getSootClass("java.io.PrintStream");
    SootMethod printStreamMethod = printStreamClass.getMethod("java.io.PrintStream printf(java.lang.String,java.lang.Object[])");
    StringConstant printMsg = StringConstant.v(message);
    JVirtualInvokeExpr printInvokeExpr =
        new JVirtualInvokeExpr(printLocal, printStreamMethod.makeRef(), List.of(printMsg, arrayLocal));
    statements.add(new JInvokeStmt(printInvokeExpr));

    for (Unit statement : statements)
      statement.addTag(NoiserTag.OSCAR_INSTRUMENTED);

    return statements;
  }

  public List<Unit> noise(NoiseLocation noiseLoc) {
    // Instantiate enum value
    JimpleLocal enumLocal = localGenerator.fromType(RefType.v(noiseLoc.getClass().getName()));

    StaticFieldRef enumField = Jimple.v().newStaticFieldRef(new AbstractSootFieldRef(
        Scene.v().getSootClass(noiseLoc.getClass().getName()),
        noiseLoc.name(),
        enumLocal.getType(),
        true
    ));

    JAssignStmt enumAssign = new JAssignStmt(enumLocal, enumField);

    Stmt noiseStmt = staticInvoke(
        "oscar.controller.Controller",
        "void noise(oscar.controller.noise.NoiseLocation,java.lang.String)",
        List.of(enumLocal, StringConstant.v(UUID.randomUUID().toString()))
    );

    enumAssign.addTag(NoiserTag.OSCAR_INSTRUMENTED);
    noiseStmt.addTag(NoiserTag.OSCAR_INSTRUMENTED);

    return List.of(enumAssign, noiseStmt);
  }

  /*
  public List<Unit> signal(NoiseCategory category) {
    // Instantiate enum value
    JimpleLocal enumLocal = localGenerator.fromType(RefType.v(category.getClass().getName()));
    StaticFieldRef enumField = Jimple.v().newStaticFieldRef(new AbstractSootFieldRef(
        Scene.v().getSootClass(category.getClass().getName()),
        category.name(),
        enumLocal.getType(),
        true
    ));

    JAssignStmt enumAssign = new JAssignStmt(enumLocal, enumField);

    Stmt noiseStmt = staticInvoke(
        "oscar.controller.Controller",
        "void signal(oscar.controller.noise.NoiseCategory,java.lang.String)",
        List.of(enumLocal, StringConstant.v(UUID.randomUUID().toString()))
    );

    return List.of(enumAssign, noiseStmt);
  }
  */

  public void appendUnit(Unit unit) {
    Unit indexUnit = body.getUnits().stream()
                         .filter(JIdentityStmt.class::isInstance)
                         .findFirst().get();

    body.getUnits().insertAfter(unit, indexUnit);
  }

  public JAssignStmt assignArray(String arrayTypeName, int arrayDimensions, int arraySize) {
    JimpleLocal arrayLocal = localGenerator.fromType(ArrayType.v(RefType.v(arrayTypeName), arrayDimensions));
    return new JAssignStmt(arrayLocal, new JNewArrayExpr(RefType.v(arrayTypeName), IntConstant.v(arraySize)));
  }
}
