package oscar.engine.generators;

import soot.*;
import soot.javaToJimple.DefaultLocalGenerator;
import soot.jimple.JimpleBody;
import soot.jimple.internal.JimpleLocal;

public class LocalGenerator {
  private final DefaultLocalGenerator generator;
  private final JimpleBody body;

  public LocalGenerator(JimpleBody body) {
    this.generator = new DefaultLocalGenerator(body);
    this.body = body;
  }

  public JimpleLocal fromType(Type type) {
    return (JimpleLocal) generator.generateLocal(type);
  }

  public JimpleLocal arrayFromType(Type type, int dimensions) {
    return fromType(ArrayType.v(type, dimensions));
  }

  public JimpleLocal fromClass(String className) {
    SootClass sootClass = Scene.v().getSootClass(className);
    return fromType(RefType.v(sootClass));
  }

  public JimpleLocal fromClass(SootClass sootClass) {
    return fromType(RefType.v(sootClass));
  }

  public void addExceptionToBodyMethod(String exceptionClassName) {
    SootClass exceptionClass = Scene.v().getSootClass(exceptionClassName);
    body.getMethod().addExceptionIfAbsent(exceptionClass);
  }

}
