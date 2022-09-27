package oscar.transformers.noisers.misc;

import oscar.controller.noise.NoiseLocation;
import oscar.engine.Engine;
import oscar.engine.body.JimpleBodyBox;
import oscar.engine.utils.JimpleThreadUtils;
import oscar.transformers.JimpleTransformer;
import oscar.transformers.analysers.StatementVariables;
import oscar.transformers.analysers.Variable;
import soot.*;
import soot.jimple.internal.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class ThreadExternalFieldNoiser extends JimpleTransformer {
  public ThreadExternalFieldNoiser() {
    super("tefn", ThreadExternalFieldNoiser.class);
    this.routine = this::routine;
  }

  private void routine(JimpleBodyBox bodyBox) {
    // Check if this method body routine belongs to a thread class
    SootClass methodClass = bodyBox.body().getMethod().getDeclaringClass();
    if (!JimpleThreadUtils.isThreadOrRunnableClass(methodClass))
      return;

    // Get all assignments which access outside variables
    for (JAssignStmt stmt :  getOutsideVariableAccess(bodyBox, methodClass)) {
      bodyBox.body()
             .getUnits()
             .insertBefore(bodyBox.generator().Statement.noise(NoiseLocation.BEFORE_THREAD_EXTERNAL_FIELD_REF), stmt);

      bodyBox.body()
             .getUnits()
             .insertAfter(bodyBox.generator().Statement.noise(NoiseLocation.AFTER_THREAD_EXTERNAL_FIELD_REF), stmt);
    }
  }

  private Set<JAssignStmt> getOutsideVariableAccess(JimpleBodyBox bodyBox, SootClass methodClass) {
    return bodyBox.body()
                  .getUnits()
                  .stream()
                  .filter(JAssignStmt.class::isInstance)
                  .map(JAssignStmt.class::cast)
                  .filter(stmt -> stmtReferencesExternalField(stmt, methodClass, bodyBox))
                  .collect(Collectors.toSet());
  }

  /***
   * Check if any assignment access being made to an external field
   * @param bodyBox bodybox of the method body being checked
   * @param methodClass class that this method body being checked belongs to
   * @return true if any assignment access being made to an external field
   */
  public static boolean stmtReferencesExternalField(JAssignStmt stmt, SootClass methodClass, JimpleBodyBox bodyBox) {
    StatementVariables allVars = Variable.getVariablesFromAssignment(stmt, bodyBox);

    // Join all variables which are fields
    HashSet<Variable> sVars = new HashSet<>();

    if (allVars.getLValue().isField())
      sVars.add(allVars.getLValue());

    allVars.getRValues().stream().filter(Variable::isField).forEach(sVars::add);

    // Check if variables are external and not blacklisted
    for (Variable sVar : sVars) {
      String varName = sVar.getName();

      boolean iSBlacklisted = false;

      for (String blacklistedClass : Engine.BlacklistedClasses)
        if (varName.contains("<" + blacklistedClass)) {
          iSBlacklisted = true;
          break;
        }

      if (iSBlacklisted)
        continue;

      if (!varName.contains("<" + methodClass.getName() + ": "))
        return true;
    }

    return false;
  }
}




