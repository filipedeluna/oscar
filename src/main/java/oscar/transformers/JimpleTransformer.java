package oscar.transformers;

import oscar.engine.body.JimpleBodyBox;
import oscar.engine.Engine;
import soot.Body;
import soot.BodyTransformer;
import soot.jimple.JimpleBody;

import java.util.Map;
import java.util.function.Consumer;

public abstract class JimpleTransformer extends BodyTransformer implements IJimpleTransformer {
  private final String phase = "jtp";
  private final String subphase;

  private final Class<? extends JimpleTransformer> clazz;
  protected Consumer<JimpleBodyBox> routine;

  public JimpleTransformer(String subPhase, Class<? extends JimpleTransformer> clazz) {
    this.subphase = phase + "." + subPhase;
    this.clazz = clazz;
  }

  public JimpleTransformer(String subPhase, Class<? extends JimpleTransformer> clazz, Consumer<JimpleBodyBox> routine) {
    this.subphase = phase + "." + subPhase;
    this.clazz = clazz;
    this.routine = routine;
  }

  @Override
  protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
    if (!(body instanceof JimpleBody))
      throw new RuntimeException("Expected a Jimple body.");

    // First we filter out blacklisted methods
    if (Engine.isClassBlacklisted(body.getMethod()))
      return;

    JimpleBodyBox customBody = new JimpleBodyBox((JimpleBody) body);

    Engine.startTransformer(clazz, customBody.body());

    routine.accept(customBody);
    customBody.body().validate();

    Engine.endTransformer(clazz, customBody.body());
  }

  public String getPhase() {
    return phase;
  }

  public String getSubPhase() {
    return subphase;
  }
}
