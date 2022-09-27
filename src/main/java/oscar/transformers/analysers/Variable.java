package oscar.transformers.analysers;

import jas.Var;
import oscar.engine.body.JimpleBodyBox;
import soot.SootMethod;
import soot.Value;
import soot.jimple.BinopExpr;
import soot.jimple.ConditionExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.internal.*;

import java.util.HashSet;
import java.util.Set;

public class Variable {
  private final VariableType type;
  private final String name;

  public Variable(String name, VariableType type) {
    this.name = name;
    this.type = type;
  }

  public VariableType getType() {
    return type;
  }

  public boolean isField() {
    return type == VariableType.FIELD;
  }

  public boolean isLocal() {
    return type == VariableType.LOCAL;
  }

  public boolean isReturn() {
    return type == VariableType.METHOD_RETURN;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Variable))
      return false;

    return this.name.equals(((Variable) obj).getName());
  }

  @Override
  public int hashCode() {
    return this.getName().hashCode();
  }

  public static StatementVariables getVariablesFromAssignment(JAssignStmt stmt, JimpleBodyBox bodyBox) {
    String methodName = bodyBox.body().getMethod().getName();

    Variable lValue = getVariable(stmt.getLeftOp(), methodName);
    Set<Variable> rValues = getVariablesFromRValue(stmt.getRightOp(), methodName);

    return new StatementVariables(lValue, rValues);
  }

  public static StatementVariables getVariablesFromReturn(JReturnStmt stmt, JimpleBodyBox bodyBox) {
    // Get local to be returned
    Variable rValueVar = getVariable(stmt.getOp(), bodyBox.body().getMethod().getName());

    // Create ref for return stmt
    Variable lValueVar = getReturnsVariableFromMethod(bodyBox.body().getMethod());

    // Check if rvalue is empty
    if (rValueVar == null)
      return new StatementVariables(lValueVar, Set.of());
    else
      return new StatementVariables(lValueVar, Set.of(rValueVar));
  }

  private static Variable getReturnsVariableFromMethod(SootMethod method) {
    // Special case for predicates
    if (method.getName().equals("bootstrap$") && method.getDeclaringClass()
                                                       .implementsInterface("java.util.function.Predicate")) {
      SootMethod predicateMethod = getPredicateMethod(method);

      return new Variable(predicateMethod.getSignature() + ":returns", VariableType.METHOD_RETURN);
    }
    return new Variable(method.getSignature() + ":returns", VariableType.METHOD_RETURN);
  }

  private static Set<Variable> getVariablesFromRValue(Value value, String methodName) {
    HashSet<Variable> variables = new HashSet<>();

    // Check if it is a basic variable
    Variable basicVar = getVariable(value, methodName);

    if (basicVar != null)
      variables.add(basicVar);
    else {
      // Not a basic variable, try and extract all
      if (value instanceof JCastExpr) {
        Variable var = getVariable(((JCastExpr) value).getOp(), methodName);
        if (var != null)
          variables.add(var);
      }

      if (value instanceof InvokeExpr) {
        // Get all arguments as variables
        for (Value arg : ((InvokeExpr) value).getArgs()) {
          Variable var = getVariable(arg, methodName);
          if (var != null)
            variables.add(var);
        }

        //  Get variable relative to method returns
        Variable returnsVar = getReturnsVariableFromMethod(((InvokeExpr) value).getMethod());
        variables.add(returnsVar);
      }
    }

    if (value instanceof BinopExpr) {
      Variable var1 = getVariable(((BinopExpr) value).getOp1(), methodName);
      Variable var2 = getVariable(((BinopExpr) value).getOp2(), methodName);

      if (var1 != null)
        variables.add(var1);

      if (var2 != null)
        variables.add(var2);

    }

    return variables;
  }

  private static Variable getVariable(Value value, String methodName) {
    if (value instanceof StaticFieldRef)
      return new Variable(((StaticFieldRef) value).getFieldRef().getSignature(), VariableType.FIELD);

    if (value instanceof JInstanceFieldRef)
      return new Variable(((JInstanceFieldRef) value).getFieldRef().getSignature(), VariableType.FIELD);

    if (value instanceof JArrayRef) {
      JimpleLocal local = (JimpleLocal) ((JArrayRef) value).getBase();
      return new Variable(methodName + ":" + local.getName(), VariableType.LOCAL);
    }

    if (value instanceof JimpleLocal)
      return new Variable(methodName + ":" + ((JimpleLocal) value).getName(), VariableType.LOCAL);

    if (value instanceof JCastExpr)
      return getVariable(((JCastExpr) value).getOp(), methodName);

    return null;
  }

  /**
   * Extract test method from predicate function
   *
   * @param baseMethod
   * @return the method that contains the predicate function
   */
  public static SootMethod getPredicateMethod(SootMethod baseMethod) {
    return baseMethod.getDeclaringClass().getMethodByName("test")
                     .getActiveBody()
                     .getUnits()
                     .stream()
                     .filter(JAssignStmt.class::isInstance)
                     .map(JAssignStmt.class::cast)
                     .map(JAssignStmt::getRightOp)
                     .filter(JStaticInvokeExpr.class::isInstance)
                     .map(JStaticInvokeExpr.class::cast)
                     .map(AbstractInvokeExpr::getMethod)
                     .findFirst().get();
  }
}
