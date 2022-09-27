package oscar.transformers.analysers.shared;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import oscar.engine.Engine;
import oscar.transformers.IJimpleTransformer;
import oscar.transformers.analysers.Variable;
import oscar.transformers.analysers.VariableType;
import soot.SceneTransformer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SharedVariableParser extends SceneTransformer implements IJimpleTransformer  {
  private final String phase = "wjtp";
  private final String subphase;
  private final DefaultDirectedGraph<Variable, DefaultEdge> graph;
  private final HashMap<String, HashSet<String>> variableDependencies;

  public SharedVariableParser(DefaultDirectedGraph<Variable, DefaultEdge> graph, HashMap<String, HashSet<String>> variableDependencies) {
    this.subphase = phase + ".svp";
    this.graph = graph;
    this.variableDependencies = variableDependencies;
  }

  @Override
  protected void internalTransform(String phaseName, Map<String, String> options) {
    Engine.startSceneTransformer(SharedVariableParser.class);

    // Process variable dependencies
    for (Variable var : graph.iterables().vertices()) {
      variableDependencies.putIfAbsent(var.getName(), new HashSet<>());

      HashSet<Variable> dependencies = getAllDependencies(var, graph, new HashSet<>());
      for (Variable dependency : dependencies)
        // Only field dependencies should matter
        if (dependency.getType() == VariableType.FIELD)
          variableDependencies.get(var.getName()).add(dependency.getName());
    }

    Engine.endSceneTransformer(SharedVariableParser.class);
  }

  /**
   * Recursively obtain all dependencies in graph for a vertex
   * @param source source vertex
   * @param graph source graph
   * @param vertices set of dependency vertices found up to now
   * @return set of all dependency vertices
   */
  private HashSet<Variable> getAllDependencies(Variable source, Graph<Variable, DefaultEdge> graph, HashSet<Variable> vertices) {
    Set<DefaultEdge> outgoingEdges = graph.outgoingEdgesOf(source);

    for (DefaultEdge edge : outgoingEdges) {
      Variable target = graph.getEdgeTarget(edge);

      if (!vertices.contains(target)) {
        vertices.add(target);
        vertices.addAll(getAllDependencies(target, graph, vertices));
      }
    }

    return vertices;
  }

  public String getPhase() {
    return phase;
  }

  public String getSubPhase() {
    return subphase;
  }
}
