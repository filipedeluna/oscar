package oscar.engine.body;

import oscar.engine.generators.JimpleGenerator;
import soot.jimple.JimpleBody;

import java.util.HashMap;
import java.util.HashSet;

public final class JimpleBodyBox {
  private final JimpleBody body;
  private final JimpleGenerator generator;
  private final HashMap<String, HashSet<String>> metadata;

  public JimpleBodyBox(JimpleBody body) {
    this.body = body;
    this.generator = new JimpleGenerator(body);
    this.metadata = new HashMap<>();
  }

  public JimpleBody body() {
    return body;
  }

  public JimpleGenerator generator() {
    return generator;
  }

  public void addMetadata(Metadata key, String value) {
    metadata.putIfAbsent(key.name(), new HashSet<>());
    metadata.get(key.name()).add(value);
  }

  public boolean hasMetadata(Metadata key, String value) {
    return metadata.get(key.name()).contains(value);
  }
}
