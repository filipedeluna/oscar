package oscar.transformers.noisers;

import soot.tagkit.StringConstantValueTag;

public class NoiserTag {
  public static final StringConstantValueTag OSCAR_INSTRUMENTED = new StringConstantValueTag("OSCAR_INSTRUMENTED");
  public static final StringConstantValueTag SHARED_VAR_ACCESS = new StringConstantValueTag("SHARED_VAR_ACCESS");
}
