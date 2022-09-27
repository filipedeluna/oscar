package oscar.engine.utils;

import oscar.transformers.noisers.thread.ThreadCreationNoiser;
import soot.*;
import soot.jimple.internal.JInvokeStmt;

import java.util.List;

public class JimpleThreadUtils {
  public static boolean isThreadStartOrRunStatement(JInvokeStmt stmt) {
    SootMethodRef ref = stmt.getInvokeExpr().getMethodRef();
    String methodName = ref.getName();
    SootClass sootClass = ref.getDeclaringClass();

    // Check if class contains start or run methods
    return List.of("start", "run").contains(methodName) && JimpleThreadUtils.isThreadOrRunnableClass(sootClass);
  }

  /***
   * Recursively check if the class is a thread class, extends a thread class or implements a Runnable interface
   * @param sootClass class that will be checked recursively
   * @return true if class is extends Thread or implements Runnable
   */
  public static boolean isThreadOrRunnableClass(SootClass sootClass) {
    if (sootClass.getName().equals("java.lang.Thread"))
      return true;

    for (SootClass implementsClass : sootClass.getInterfaces())
      if (implementsRunnable(implementsClass))
        return true;

    if (sootClass.hasSuperclass())
      return isThreadOrRunnableClass(sootClass.getSuperclass());

    return false;
  }

  /***
   * Recursively check if class extends from a Runnable class
   * @param sootClass class to check
   * @return true if extends Runnable
   */
  @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
  public static boolean implementsRunnable(SootClass sootClass) {
    if (sootClass.getName().equals("java.lang.Runnable"))
      return true;
    else if (sootClass.hasSuperclass())
      return implementsRunnable(sootClass.getSuperclass());

    return false;
  }

  /***
   * Recursively check if the class implements a Runnable interface
   * @param sootClass class to check
   * @return true if is Runnable
   */
  public static boolean isRunnableClass(SootClass sootClass) {
    for (SootClass implementsClass : sootClass.getInterfaces())
      if (implementsRunnable(implementsClass))
        return true;

    if (sootClass.hasSuperclass())
      return isRunnableClass(sootClass.getSuperclass());

    return false;
  }
}
