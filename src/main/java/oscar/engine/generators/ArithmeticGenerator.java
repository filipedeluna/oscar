package oscar.engine.generators;

import soot.Value;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JRemExpr;

public class ArithmeticGenerator {
  private final LocalGenerator localGenerator;

  public ArithmeticGenerator(LocalGenerator localGenerator) {
    this.localGenerator = localGenerator;
  }

  public JAssignStmt modulo(Value valueA, Value valueB) {
    Value result = localGenerator.fromType(valueA.getType());

    return new JAssignStmt(result, new JRemExpr(valueA, valueB));
  }
}
