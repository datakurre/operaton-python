package org.operaton.bpm.extension.python;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import javax.script.*;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.python.embedding.VirtualFileSystem;

/**
 * JSR-223 {@link ScriptEngine} implementation backed by a GraalPy {@link Context}.
 *
 * <p>Each engine instance owns its own GraalPy {@link Context}, providing isolation between
 * concurrent script executions. Implements {@link Compilable} for pre-parsed scripts, {@link
 * Invocable} for calling Python functions/methods from Java, and {@link AutoCloseable} for proper
 * resource cleanup.
 *
 * <p>When {@code eval} methods are called with a {@link ScriptContext}, the bindings from that
 * context are injected into the GraalPy evaluation scope before execution, making Operaton process
 * variables (execution, task, etc.) accessible from Python scripts.
 */
public class PythonScriptEngine implements ScriptEngine, Compilable, Invocable, AutoCloseable {

  private static final String LANGUAGE_ID = "python";
  private final PythonScriptEngineFactory factory;
  private final Context context;
  private final ByteArrayOutputStream out;
  private final ByteArrayOutputStream err;

  /**
   * Creates a new script engine with its own isolated GraalPy context.
   *
   * @param factory the factory that created this engine
   * @param vfs the virtual filesystem for Python resources
   */
  PythonScriptEngine(PythonScriptEngineFactory factory, VirtualFileSystem vfs) {
    this.factory = factory;
    this.out = new ByteArrayOutputStream();
    this.err = new ByteArrayOutputStream();
    this.context =
        PythonResources.contextBuilder(vfs).allowAllAccess(true).out(out).err(err).build();
  }

  /** Closes the GraalPy context and releases all resources. */
  @Override
  public void close() {
    context.close();
  }

  @Override
  public CompiledScript compile(String script) throws ScriptException {
    try {
      return compile(Source.newBuilder(LANGUAGE_ID, script, "<string>").build());
    } catch (IOException e) {
      throw new ScriptException(e);
    }
  }

  @Override
  public CompiledScript compile(Reader reader) throws ScriptException {
    try {
      return compile(Source.newBuilder(LANGUAGE_ID, reader, "<reader>").build());
    } catch (IOException e) {
      throw new ScriptException(e);
    }
  }

  /**
   * Compiles a GraalPy {@link Source} into a {@link CompiledScript}.
   *
   * @param source the GraalPy source to compile
   * @return a compiled script that can be evaluated repeatedly
   * @throws ScriptException if parsing fails
   */
  public CompiledScript compile(Source source) throws ScriptException {
    return new PythonCompiledScript(source, context, this, out, err);
  }

  @Override
  public Object eval(String script, ScriptContext scriptContext) throws ScriptException {
    applyBindings(scriptContext);
    return eval(script);
  }

  @Override
  public Object eval(Reader reader, ScriptContext scriptContext) throws ScriptException {
    applyBindings(scriptContext);
    return eval(reader);
  }

  @Override
  public Object eval(String script) throws ScriptException {
    Source source;
    try {
      source = Source.newBuilder(LANGUAGE_ID, script, "<string>").build();
    } catch (IOException e) {
      throw new ScriptException(e);
    }
    return evalSource(source);
  }

  @Override
  public Object eval(Reader reader) throws ScriptException {
    Source source;
    try {
      source = Source.newBuilder(LANGUAGE_ID, reader, "<reader>").build();
    } catch (IOException e) {
      throw new ScriptException(e);
    }
    return evalSource(source);
  }

  @Override
  public Object eval(String script, Bindings n) throws ScriptException {
    applyBindingsMap(n);
    return eval(script);
  }

  @Override
  public Object eval(Reader reader, Bindings n) throws ScriptException {
    applyBindingsMap(n);
    return eval(reader);
  }

  @Override
  public void put(String key, Object value) {
    context.getBindings(LANGUAGE_ID).putMember(key, value);
  }

  @Override
  public Object get(String key) {
    Value value = context.getBindings(LANGUAGE_ID).getMember(key);
    return value != null ? value.as(Object.class) : null;
  }

  @Override
  public Bindings getBindings(int scope) {
    return new PythonBindings(context);
  }

  @Override
  public void setBindings(Bindings bindings, int scope) {
    if (bindings != null) {
      for (String key : bindings.keySet()) {
        put(key, bindings.get(key));
      }
    }
  }

  @Override
  public Bindings createBindings() {
    return new SimpleBindings();
  }

  @Override
  public ScriptContext getContext() {
    return new PythonScriptContext(context);
  }

  @Override
  public void setContext(ScriptContext context) {
    throw new UnsupportedOperationException(
        "The context of a Polyglot ScriptEngine cannot be modified.");
  }

  @Override
  public PythonScriptEngineFactory getFactory() {
    return factory;
  }

  @Override
  public Object invokeMethod(Object thiz, String name, Object... args)
      throws ScriptException, NoSuchMethodException {
    try {
      Value receiver = context.asValue(thiz);
      if (receiver.canInvokeMember(name)) {
        return receiver.invokeMember(name, args).as(Object.class);
      } else {
        throw new NoSuchMethodException(name);
      }
    } catch (PolyglotException e) {
      throw new ScriptException(e);
    }
  }

  @Override
  public Object invokeFunction(String name, Object... args)
      throws ScriptException, NoSuchMethodException {
    try {
      Value function = context.getBindings(LANGUAGE_ID).getMember(name);
      if (function == null) {
        throw new NoSuchMethodException(name);
      }
      if (!function.canExecute()) {
        throw new NoSuchMethodException("'" + name + "' is not callable");
      }
      return function.execute(args).as(Object.class);
    } catch (PolyglotException e) {
      throw new ScriptException(e);
    }
  }

  @Override
  public <T> T getInterface(Class<T> interfaceClass) {
    Value bindings = context.getBindings(LANGUAGE_ID);
    return bindings.as(interfaceClass);
  }

  @Override
  public <T> T getInterface(Object thiz, Class<T> interfaceClass) {
    return context.asValue(thiz).as(interfaceClass);
  }

  /**
   * Evaluates a GraalPy source and returns the result.
   *
   * @param source the source to evaluate
   * @return the result of evaluation
   * @throws ScriptException if evaluation fails
   */
  private Object evalSource(Source source) throws ScriptException {
    try {
      Value result = context.eval(source);
      return result.as(Object.class);
    } catch (PolyglotException e) {
      throw new ScriptException(e);
    } finally {
      out.reset();
      err.reset();
    }
  }

  /**
   * Injects bindings from a {@link ScriptContext} into the GraalPy context. This is critical for
   * Operaton integration — process variables (execution, task, etc.) are passed through the
   * ScriptContext's ENGINE_SCOPE bindings.
   */
  private void applyBindings(ScriptContext scriptContext) {
    if (scriptContext == null) {
      return;
    }
    // Apply GLOBAL_SCOPE first, then ENGINE_SCOPE (so engine scope takes precedence)
    Bindings globalBindings = scriptContext.getBindings(ScriptContext.GLOBAL_SCOPE);
    if (globalBindings != null) {
      applyBindingsMap(globalBindings);
    }
    Bindings engineBindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
    if (engineBindings != null) {
      applyBindingsMap(engineBindings);
    }
  }

  /** Injects all entries from a Bindings map into the GraalPy context's Python bindings. */
  private void applyBindingsMap(Bindings bindings) {
    if (bindings == null) {
      return;
    }
    Value pyBindings = context.getBindings(LANGUAGE_ID);
    for (var entry : bindings.entrySet()) {
      pyBindings.putMember(entry.getKey(), entry.getValue());
    }
  }
}
