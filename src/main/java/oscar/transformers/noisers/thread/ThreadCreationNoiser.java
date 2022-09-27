package oscar.transformers.noisers.thread;

import oscar.controller.noise.NoiseLocation;
import oscar.engine.body.JimpleBodyBox;
import oscar.engine.utils.JimpleThreadUtils;
import oscar.transformers.JimpleSceneTransformer;
import soot.*;
import soot.jimple.JimpleBody;
import soot.jimple.internal.*;
import soot.tagkit.StringConstantValueTag;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ThreadCreationNoiser extends JimpleSceneTransformer {
  private static final StringConstantValueTag THREAD_LAUNCH_NOISED = new StringConstantValueTag("THREAD_LAUNCH_NOISED");

  public ThreadCreationNoiser() {
    super("tcn", ThreadCreationNoiser.class);
    this.routine = this::routine;
  }

  private void routine(JimpleBodyBox bodyBox) {
    HashSet<SootClass> runnableClasses = new HashSet<>();

    // Tag all runnable classes launched by thread creation
    runnableClasses.addAll(getRunnableClasses(bodyBox.body()));

    // Tag all runnable methods and lambdas launched by thread creation
    runnableClasses.addAll(getRunnableMethods(bodyBox.body()));

    // Tag all classes that extend the thread interface and are launched
    runnableClasses.addAll(getThreadExtendingClasses(bodyBox.body()));

    // Noise all runnable classes' run methods
    runnableClasses.forEach(this::noiseThreadRoutine);

    // Get all thread start and run statements and then wrap them for noising
    Set<JInvokeStmt> invokeStmts = getStartAndRunStatements(bodyBox.body());

    //Replace original calls with calls to wrapped method
    for (JInvokeStmt stmt : invokeStmts) {
      bodyBox.body()
             .getUnits()
             .insertBefore(bodyBox.generator().Statement.noise(NoiseLocation.BEFORE_THREAD_LAUNCH), stmt);

      bodyBox.body()
             .getUnits()
             .insertAfter(bodyBox.generator().Statement.noise(NoiseLocation.AFTER_THREAD_LAUNCH), stmt);
    }
  }

  private void noiseThreadRoutine(SootClass sootClass) {
    JimpleBody classBody = (JimpleBody) sootClass.getMethod("void run()").getActiveBody();
    JimpleBodyBox bodyBox = new JimpleBodyBox(classBody);

    // Check if body has already been instrumented
    if (bodyBox.body().hasTag(THREAD_LAUNCH_NOISED.getName()))
      return;

    // Insert noise and signal statements after first (identity statement)
    List<Unit> noiseStmts = bodyBox.generator().Statement.noise(NoiseLocation.BEFORE_THREAD_ROUTINE);
    bodyBox.body().getUnits().insertBefore(noiseStmts, bodyBox.body().getFirstNonIdentityStmt());

    // Add instrumented tag and validate body
    bodyBox.body().addTag(THREAD_LAUNCH_NOISED);

    bodyBox.body().validate();
  }

  public static Set<JInvokeStmt> getStartAndRunStatements(JimpleBody body) {
    return body.getUnits()
               .stream()
               .filter(JInvokeStmt.class::isInstance)
               .map(JInvokeStmt.class::cast)
               .filter(s -> s.getInvokeExpr() instanceof JVirtualInvokeExpr)
               .filter(JimpleThreadUtils::isThreadStartOrRunStatement)
               .collect(Collectors.toSet());
  }

  /**
   * Get runnable classes that are inserted as parameter into initialized threads
   *
   * @param body jimple body from which to extract all runnable classes
   * @return set of runnable classes
   */
  private static Set<SootClass> getRunnableClasses(JimpleBody body) {
    return body.getUnits()
               .stream()
               .filter(JInvokeStmt.class::isInstance)
               .map(JInvokeStmt.class::cast)
               .map(JInvokeStmt::getInvokeExpr)
               .filter(JSpecialInvokeExpr.class::isInstance)
               .map(JSpecialInvokeExpr.class::cast)
               .filter(e -> e.getMethod().getDeclaringClass().getName().equals("java.lang.Thread"))
               .filter(e -> e.getMethod().getName().equals("<init>"))
               .filter(e -> e.getArgCount() != 0)
               .map(e -> e.getArg(0))
               .filter(JimpleLocal.class::isInstance)
               .map(JimpleLocal.class::cast)
               .map(JimpleLocal::getType)
               .filter(RefType.class::isInstance)
               .map(RefType.class::cast)
               .map(RefType::getSootClass)
               .filter(JimpleThreadUtils::isRunnableClass)
               .collect(Collectors.toSet());
  }

  /**
   * Get all classes that extend the thread superclass threads
   *
   * @param body jimple body from which to extract all thread extending classes
   * @return set of classes
   */
  private static Set<SootClass> getThreadExtendingClasses(JimpleBody body) {
    return body.getUnits()
               .stream()
               .filter(JInvokeStmt.class::isInstance)
               .map(JInvokeStmt.class::cast)
               .map(JInvokeStmt::getInvokeExpr)
               .filter(JSpecialInvokeExpr.class::isInstance)
               .map(JSpecialInvokeExpr.class::cast)
               .map(JSpecialInvokeExpr::getMethod)
               .filter(m -> m.getName().equals("<init>"))
               .map(SootMethod::getDeclaringClass)
               .filter(SootClass::hasSuperclass)
               .filter(c -> c.getSuperclass().getName().equals("java.lang.Thread"))
               .map(SootClass::getType)
               .map(RefType::getSootClass)
               .collect(Collectors.toSet());
  }

  /**
   * Get runnable methods and lambdas launched by thread creation
   * usually these are launched as a lambda parameter to a thread start
   *
   * @param body jimple body from which to extract all thread extending classes
   * @return list of classes
   */
  private static Set<SootClass> getRunnableMethods(JimpleBody body) {
    return body.getUnits()
               .stream()
               .filter(JAssignStmt.class::isInstance)
               .map(JAssignStmt.class::cast)
               .map(JAssignStmt::getRightOp)
               .filter(JStaticInvokeExpr.class::isInstance)
               .map(JStaticInvokeExpr.class::cast)
               .map(JStaticInvokeExpr::getMethodRef)
               .filter(sie -> sie.getReturnType() instanceof RefType)
               .filter(sie -> ((RefType) sie.getReturnType()).getClassName().equals("java.lang.Runnable"))
               .map(SootMethodInterface::getDeclaringClass)
               .collect(Collectors.toSet());
  }
}



