package oscar.transformers;

import oscar.engine.Engine;
import oscar.engine.body.JimpleBodyBox;
import oscar.transformers.analysers.Variable;
import soot.*;
import soot.jimple.JimpleBody;
import soot.jimple.internal.AbstractInvokeExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.toolkits.callgraph.Targets;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

public abstract class JimpleSceneTransformer extends SceneTransformer implements IJimpleTransformer {
  private final String phase = "wjtp";
  private final String subphase;

  private final Class<? extends JimpleSceneTransformer> clazz;
  private final HashSet<String> visited = new HashSet<>();
  protected Consumer<JimpleBodyBox> routine;

  public JimpleSceneTransformer(String subPhase, Class<? extends JimpleSceneTransformer> clazz) {
    this.subphase = phase + "." + subPhase;
    this.clazz = clazz;
  }

  @Override
  protected void internalTransform(String phaseName, Map<String, String> options) {
    Engine.startSceneTransformer(clazz);

    recursiveTransform(Scene.v().getMainMethod());

    Engine.endSceneTransformer(clazz);
  }

  private void recursiveTransform(SootMethod method) {
    // Avoid blacklisted methods/classes
    if (Engine.isClassBlacklisted(method) || method.isPhantom())
      return;

    // Avoid visiting same method multiple times
    if (visited.contains(getMethodFullName(method)))
      return;

    visited.add(getMethodFullName(method));

    Engine.startCallgraphRoutine(clazz, method);

    // Run routine
    routine.accept(new JimpleBodyBox((JimpleBody) method.getActiveBody()));

    // Check if method is dynamic bootstrap method
    // Since there are no calls to this body, we have to call it
    if (method.getName().equals("bootstrap$"))
      recursiveTransform(getBootstrapMethod(method));

    // Get all edges of this method in call graph and run routine recursively
    Iterator<MethodOrMethodContext> targets = new Targets(Scene.v().getCallGraph().edgesOutOf(method));

    while (targets.hasNext())
      recursiveTransform((SootMethod) targets.next());

    Engine.endCallgraphRoutine(clazz, method);
  }

  public String getPhase() {
    return phase;
  }

  public String getSubPhase() {
    return subphase;
  }

  private static String getMethodFullName(SootMethod method) {
    return method.getDeclaringClass() + " " + method.getSignature();
  }

  private static SootMethod getBootstrapMethod(SootMethod method) {
    SootClass sootClass = method.getDeclaringClass();

    // Base case
    if (sootClass.declaresMethodByName("run"))
      return sootClass.getMethodByName("run");

    // For predicates
    if (sootClass.implementsInterface("java.util.function.Predicate"))
      return Variable.getPredicateMethod(method);

    throw new RuntimeException("Bootstrap method not found.");
  }
}
