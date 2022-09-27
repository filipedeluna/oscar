package oscar.engine.generators;

import soot.jimple.JimpleBody;

public final class JimpleGenerator {
  public final StatementGenerator Statement;
  public final ConversionGenerator Conversion;
  public final ArithmeticGenerator Arithmetic;
  public final LocalGenerator Local;

  public JimpleGenerator(JimpleBody body) {
    this.Local = new LocalGenerator(body);
    this.Statement = new StatementGenerator(this.Local, body);
    this.Conversion = new ConversionGenerator(this.Local);
    this.Arithmetic = new ArithmeticGenerator(this.Local);
  }
}
