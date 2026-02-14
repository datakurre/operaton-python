package org.operaton.bpm.extension.python;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

/**
 * A pre-parsed Python script that can be evaluated repeatedly without re-parsing.
 *
 * <p>Created via {@link PythonScriptEngine#compile(String)} or {@link
 * PythonScriptEngine#compile(java.io.Reader)}. The script source is parsed once at construction
 * time and evaluated against the engine's GraalPy {@link Context} on each call to {@link #eval}.
 */
public class PythonCompiledScript extends CompiledScript {

  private final PythonScriptEngine engine;
  private final Context context;
  private final Source source;
  private final OutputStream out;
  private final OutputStream err;

  PythonCompiledScript(
      Source source, Context context, PythonScriptEngine engine, OutputStream out, OutputStream err)
      throws ScriptException {
    try {
      context.parse(source);
    } catch (PolyglotException e) {
      throw new ScriptException(e);
    }
    this.context = context;
    this.source = source;
    this.engine = engine;
    this.out = out;
    this.err = err;
  }

  @Override
  public Object eval(ScriptContext scriptContext) throws ScriptException {
    // Inject bindings from the ScriptContext if provided
    if (scriptContext != null) {
      Value pyBindings = context.getBindings("python");
      Bindings globalBindings = scriptContext.getBindings(ScriptContext.GLOBAL_SCOPE);
      if (globalBindings != null) {
        for (var entry : globalBindings.entrySet()) {
          pyBindings.putMember(entry.getKey(), entry.getValue());
        }
      }
      Bindings engineBindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
      if (engineBindings != null) {
        for (var entry : engineBindings.entrySet()) {
          pyBindings.putMember(entry.getKey(), entry.getValue());
        }
      }
    }

    try {
      return this.context.eval(source).as(Object.class);
    } catch (PolyglotException e) {
      throw new ScriptException(e);
    } finally {
      if (this.out instanceof ByteArrayOutputStream baos) {
        baos.reset();
      }
      if (this.err instanceof ByteArrayOutputStream baos) {
        baos.reset();
      }
    }
  }

  @Override
  public ScriptEngine getEngine() {
    return engine;
  }
}
