package org.operaton.bpm.extension.python;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleBindings;
import org.graalvm.polyglot.Context;

/**
 * JSR-223 {@link ScriptContext} implementation for GraalPy.
 *
 * <p>Maps ENGINE_SCOPE to the GraalPy context's Python language bindings and GLOBAL_SCOPE to a
 * separate {@link SimpleBindings} instance. This provides proper scope differentiation per the
 * JSR-223 specification.
 */
public class PythonScriptContext implements ScriptContext {

  private static final List<Integer> SCOPES =
      List.of(ScriptContext.ENGINE_SCOPE, ScriptContext.GLOBAL_SCOPE);

  private final Context context;
  private Bindings engineBindings;
  private Bindings globalBindings;

  private Reader in;
  private Writer out;
  private Writer err;

  PythonScriptContext(Context context) {
    this.context = context;
    this.engineBindings = new PythonBindings(context);
    this.globalBindings = new SimpleBindings();
    this.in = new InputStreamReader(System.in);
    this.out = new OutputStreamWriter(System.out);
    this.err = new OutputStreamWriter(System.err);
  }

  @Override
  public void setBindings(Bindings bindings, int scope) {
    switch (scope) {
      case ScriptContext.ENGINE_SCOPE -> this.engineBindings = bindings;
      case ScriptContext.GLOBAL_SCOPE -> this.globalBindings = bindings;
      default -> throw new IllegalArgumentException("Invalid scope: " + scope);
    }
  }

  @Override
  public Bindings getBindings(int scope) {
    return switch (scope) {
      case ScriptContext.ENGINE_SCOPE -> engineBindings;
      case ScriptContext.GLOBAL_SCOPE -> globalBindings;
      default -> throw new IllegalArgumentException("Invalid scope: " + scope);
    };
  }

  @Override
  public void setAttribute(String name, Object value, int scope) {
    getBindings(scope).put(name, value);
  }

  @Override
  public Object getAttribute(String name, int scope) {
    return getBindings(scope).get(name);
  }

  @Override
  public Object removeAttribute(String name, int scope) {
    return getBindings(scope).remove(name);
  }

  @Override
  public Object getAttribute(String name) {
    Object value = getAttribute(name, ScriptContext.ENGINE_SCOPE);
    if (value != null) {
      return value;
    }
    return getAttribute(name, ScriptContext.GLOBAL_SCOPE);
  }

  @Override
  public int getAttributesScope(String name) {
    if (engineBindings.containsKey(name)) {
      return ScriptContext.ENGINE_SCOPE;
    } else if (globalBindings != null && globalBindings.containsKey(name)) {
      return ScriptContext.GLOBAL_SCOPE;
    }
    return -1;
  }

  @Override
  public Writer getWriter() {
    return this.out;
  }

  @Override
  public void setWriter(Writer writer) {
    this.out = writer;
  }

  @Override
  public Writer getErrorWriter() {
    return this.err;
  }

  @Override
  public void setErrorWriter(Writer writer) {
    this.err = writer;
  }

  @Override
  public Reader getReader() {
    return this.in;
  }

  @Override
  public void setReader(Reader reader) {
    this.in = reader;
  }

  @Override
  public List<Integer> getScopes() {
    return SCOPES;
  }
}
