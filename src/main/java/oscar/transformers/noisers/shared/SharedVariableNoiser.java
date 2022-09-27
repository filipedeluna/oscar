package oscar.transformers.noisers.shared;

import oscar.controller.noise.NoiseLocation;
import oscar.engine.body.JimpleBodyBox;
import oscar.transformers.JimpleTransformer;
import oscar.transformers.analysers.StatementVariables;
import oscar.transformers.analysers.Variable;
import soot.jimple.internal.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class SharedVariableNoiser extends JimpleTransformer {

  private final HashMap<String, HashSet<String>> variableDependencies;

  public SharedVariableNoiser(HashMap<String, HashSet<String>> variableDependencies) {
    super("svn", SharedVariableNoiser.class);
    this.routine = this::routine;
    this.variableDependencies = variableDependencies;

  }

  private void routine(JimpleBodyBox bodyBox) {
    // Get all assignments
    List<JAssignStmt> assignments = bodyBox.body()
                                           .getUnits()
                                           .stream()
                                           .filter(JAssignStmt.class::isInstance)
                                           .map(JAssignStmt.class::cast)
                                           .collect(Collectors.toList());

    // Sequentially process every assign statement
    for (JAssignStmt assignment : assignments) {
      // Get lvalue and rvalues
      StatementVariables statementVariables = Variable.getVariablesFromAssignment(assignment, bodyBox);

      // Get lvalue var and dependencies
      Variable lValueVar = statementVariables.getLValue();
      HashSet<String> lValueDependencies = variableDependencies.get(lValueVar.getName());

      // Continue if lvalue has no dependencies
      if (lValueDependencies == null)
        continue;

      // Check if a variable depends on itself, always noise if so
      boolean dependencyClash = false;

      if (lValueVar.isField() && lValueDependencies.contains(lValueVar.getName()))
        dependencyClash = true;

      // Check if there is a dependency clash between lValue and rValues
      for (Variable rValueVar : statementVariables.getRValues()) {
        HashSet<String> rValueDependencies = variableDependencies.get(rValueVar.getName());

        // If a dependency clash is found, break and exit loop
        if (dependencyClash)
          break;

        if (rValueDependencies != null) {
          // Check if rvalue depends on itself
          if (rValueVar.isField() && rValueDependencies.contains(rValueVar.getName()))
            dependencyClash = true;

          // Check for matching dependencies
          if (lValueDependencies.stream().anyMatch(rValueDependencies::contains))
            dependencyClash = true;
        }
      }

      // Noise this assignment, if a dependency clash was found
      if (!dependencyClash)
        continue;

      // Make sure it is not a return statement
      if (lValueVar.isReturn() || statementVariables.getRValues().stream().anyMatch(Variable::isReturn))
        continue;

      // Check noise placement type
      NoiseLocation beforeNoiseHeuristic;
      NoiseLocation afterNoiseHeuristic;

      if (lValueVar.isField() || statementVariables.getRValues().stream().anyMatch(Variable::isField)) {
        beforeNoiseHeuristic = NoiseLocation.BEFORE_SHARED_FIELD_ACCESS;
        afterNoiseHeuristic = NoiseLocation.AFTER_SHARED_FIELD_ACCESS;
      } else {
        beforeNoiseHeuristic = NoiseLocation.BEFORE_SHARED_LOCAL_ACCESS;
        afterNoiseHeuristic = NoiseLocation.AFTER_SHARED_LOCAL_ACCESS;
      }

      bodyBox.body()
             .getUnits()
             .insertBefore(bodyBox.generator().Statement.noise(beforeNoiseHeuristic), assignment);

      bodyBox.body()
             .getUnits()
             .insertAfter(bodyBox.generator().Statement.noise(afterNoiseHeuristic), assignment);
    }

  }
}
