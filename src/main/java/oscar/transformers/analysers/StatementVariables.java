package oscar.transformers.analysers;

import java.util.Set;

public class StatementVariables {
  private final Variable lValue;
  private final Set<Variable> rValues;

  public StatementVariables(Variable lValue, Set<Variable> rValues) {
    this.lValue = lValue;
    this.rValues = rValues;
  }

  public Variable getLValue() {
    return lValue;
  }

  public Set<Variable> getRValues() {
    return rValues;
  }
}
