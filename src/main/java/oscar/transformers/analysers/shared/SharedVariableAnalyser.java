package oscar.transformers.analysers.shared;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.DepthFirstIterator;
import oscar.engine.body.JimpleBodyBox;
import oscar.transformers.JimpleSceneTransformer;
import oscar.transformers.analysers.StatementVariables;
import oscar.transformers.analysers.Variable;
import soot.jimple.internal.*;

import java.util.*;
import java.util.stream.Collectors;

public class SharedVariableAnalyser extends JimpleSceneTransformer {

  private final DefaultDirectedGraph<Variable, DefaultEdge> graph;

  public SharedVariableAnalyser(DefaultDirectedGraph<Variable, DefaultEdge> graph) {
    super("sva", SharedVariableAnalyser.class);
    this.routine = this::routine;
    this.graph = graph;
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
      // Get lvalue and rvalue ref
      StatementVariables statementVariables = Variable.getVariablesFromAssignment(assignment, bodyBox);

      Variable lValueVar = statementVariables.getLValue();
      Set<Variable> rValueVars = statementVariables.getRValues();

      // Add all directed edges to the graph
      if (lValueVar == null || rValueVars.isEmpty())
        continue;

      graph.addVertex(lValueVar);
      for (Variable rValueVar : rValueVars) {
        graph.addVertex(rValueVar);

        graph.addEdge(lValueVar, rValueVar);
      }
    }

    // Get all return statements
    List<JReturnStmt> returnStmts = bodyBox.body()
                                           .getUnits()
                                           .stream()
                                           .filter(JReturnStmt.class::isInstance)
                                           .map(JReturnStmt.class::cast)
                                           .collect(Collectors.toList());

    // Sequentially process every return statement
    for (JReturnStmt returnStmt : returnStmts) {
      StatementVariables returnVars = Variable.getVariablesFromReturn(returnStmt, bodyBox);

      graph.addVertex(returnVars.getLValue());
      for (Variable rValueVar : returnVars.getRValues()) {
        graph.addVertex(rValueVar);

        graph.addEdge(returnVars.getLValue(), rValueVar);
      }
    }
  }
}
