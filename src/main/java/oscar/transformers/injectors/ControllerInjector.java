package oscar.transformers.injectors;

import oscar.engine.body.JimpleBodyBox;
import oscar.engine.Engine;
import oscar.transformers.JimpleTransformer;
import soot.*;
import soot.jimple.JimpleBody;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.internal.*;

import java.util.List;

public final class ControllerInjector extends JimpleTransformer {
  public ControllerInjector() {
    super( "ci", ControllerInjector.class, ControllerInjector::routine);
  }

  private static void routine(JimpleBodyBox oldBody) {
    // Check if class name is Main class name and method body is name
    SootClass mainClass = oldBody.body().getMethod().getDeclaringClass();
    String mainClassName = mainClass.getName();

    if (!oldBody.body().getMethod().isMain())
      return;

    if (!mainClassName.equals(Engine.getMainClass()))
      return;

    // Create new main method with original main's active body to then be wrapped
    SootMethod wrappedMainMethod = new SootMethod(
        "main_wrapped",
        List.of(ArrayType.v(RefType.v("java.lang.String"), 1)),
        VoidType.v()
    );

    wrappedMainMethod.setModifiers(Modifier.STATIC + Modifier.PUBLIC);
    wrappedMainMethod.setPhantom(false);
    mainClass.addMethod(wrappedMainMethod);

    // Copy statements from old main to new wrapped main
    wrappedMainMethod.setActiveBody(new JimpleBody());
    oldBody.body().getUnits().forEach(wrappedMainMethod.getActiveBody().getUnits()::add);
    oldBody.body().getLocals().forEach(wrappedMainMethod.getActiveBody().getLocals()::add);
    oldBody.body().getTraps().forEach(wrappedMainMethod.getActiveBody().getTraps()::add);

    // Create new body for the original (wrapper) main
    JimpleBodyBox newBody = new JimpleBodyBox(new JimpleBody());
    oldBody.body().getMethod().setActiveBody(newBody.body());
    JimpleLocal mainIdentityLocal = newBody.generator().Local.arrayFromType(RefType.v("java.lang.String"), 1);
    ParameterRef newMainParamRef = new ParameterRef(ArrayType.v(RefType.v("java.lang.String"), 1), 0);
    JIdentityStmt identityStmt = newBody.generator().Statement.identity(mainIdentityLocal, newMainParamRef);
    newBody.body().getUnits().add(identityStmt);

    // Add Oscar controller routine to parse main arguments and initialize
    JAssignStmt oscarStartStmt = (JAssignStmt) newBody.generator().Statement.staticInvoke(
        "oscar.controller.Controller",
        "java.lang.String[] start(java.lang.String[])",
        List.of(newBody.body().getParameterLocal(0))
    );

    UnitPatchingChain newMainUnits = newBody.body().getUnits();
    newMainUnits.insertAfter(oscarStartStmt, newMainUnits.getLast());

    // Call old main with parsed args
    Stmt callOrigMainStmt = newBody.generator().Statement.staticInvoke(
        mainClass.getName(),
        "void main_wrapped(java.lang.String[])",
        List.of(oscarStartStmt.getLeftOp())
    );
    newMainUnits.insertAfter(callOrigMainStmt, newMainUnits.getLast());

    // Insert end statement
    Stmt endStatement = newBody.generator().Statement.staticInvoke(
        "oscar.controller.Controller",
        "void end()",
        List.of()
    );
    newMainUnits.insertAfter(endStatement, newMainUnits.getLast());

    // Insert return statement at end
    newMainUnits.insertAfter(new JReturnVoidStmt(), newMainUnits.getLast());

    oldBody.body().validate();
    newBody.body().validate();

    // Create trap around the main call in new body --------------------------
    // Add a goto to after the main call statement
    JGotoStmt exceptionGotoStmt = new JGotoStmt(newBody.body().getUnits().getSuccOf(callOrigMainStmt));
    newBody.body().getUnits().insertAfter(exceptionGotoStmt, callOrigMainStmt);

    // Create identity statement fo exception and insert it after the goto
    JimpleLocal exceptionLocal = newBody.generator().Local.fromType(RefType.v("java.lang.Exception"));
    JIdentityStmt exceptionIdentity = new JIdentityStmt(exceptionLocal, new JCaughtExceptionRef());
    newBody.body().getUnits().insertAfter(exceptionIdentity, exceptionGotoStmt);

    // Create and insert a call to controller.exception to handle the exception
    Stmt catchStatement = newBody.generator().Statement.staticInvoke(
        "oscar.controller.Controller",
        "void exception(java.lang.Exception)",
        List.of(exceptionLocal)
    );
    newBody.body().getUnits().insertAfter(catchStatement, exceptionIdentity);

    // Finally create trap
    newBody.body().getTraps().add(
        new JTrap(
            Scene.v().getSootClass("java.lang.Exception"),
            callOrigMainStmt,
            exceptionGotoStmt,
            exceptionIdentity
        )
    );
  }
}
