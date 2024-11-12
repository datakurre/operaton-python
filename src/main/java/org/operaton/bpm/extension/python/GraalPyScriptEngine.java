package org.operaton.bpm.extension.python;

import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import javax.script.*;
import java.io.IOException;
import java.io.Reader;


public final class GraalPyScriptEngine implements ScriptEngine, Compilable, Invocable, AutoCloseable {

    private static final String LANGUAGE_ID = "python";
    private final GraalPyEngineFactory factory;
    private GraalPyContext defaultContext;

    GraalPyScriptEngine(GraalPyEngineFactory factory) {
        this.factory = factory;
        this.defaultContext = new GraalPyContext(factory);
    }

    @Override
    public void close() {
        defaultContext.getContext().close();
    }

    @Override
    public CompiledScript compile(String script) throws ScriptException {
        Source src = Source.create(LANGUAGE_ID, script);
        try {
            defaultContext.getContext().parse(src); // only for the side-effect of validating the source
        } catch (PolyglotException e) {
            throw new ScriptException(e);
        }
        return new GraalPyCompiledScript(src, this);
    }

    @Override
    public CompiledScript compile(Reader script) throws ScriptException {
        Source src;
        try {
            src = Source.newBuilder(LANGUAGE_ID, script, "sourcefromreader").build();
            defaultContext.getContext().parse(src); // only for the side-effect of validating the source
        } catch (PolyglotException | IOException e) {
            throw new ScriptException(e);
        }
        return new GraalPyCompiledScript(src, this);
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        if (context instanceof GraalPyContext c) {
            try {
                return c.getContext().eval(LANGUAGE_ID, script).as(Object.class);
            } catch (PolyglotException e) {
                throw new ScriptException(e);
            }
        } else {
            throw new ClassCastException("invalid context");
        }
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        Source src;
        try {
            src = Source.newBuilder(LANGUAGE_ID, reader, "sourcefromreader").build();
        } catch (IOException e) {
            throw new ScriptException(e);
        }
        if (context instanceof GraalPyContext) {
            GraalPyContext c = (GraalPyContext) context;
            try {
                return c.getContext().eval(src).as(Object.class);
            } catch (PolyglotException e) {
                throw new ScriptException(e);
            }
        } else {
            throw new ScriptException("invalid context");
        }
    }

    @Override
    public Object eval(String script) throws ScriptException {
        return eval(script, defaultContext);
    }

    @Override
    public Object eval(Reader reader) throws ScriptException {
        return eval(reader, defaultContext);
    }

    @Override
    public Object eval(String script, Bindings n) throws ScriptException {
        throw new UnsupportedOperationException("Bindings for Polyglot language cannot be created explicitly");
    }

    @Override
    public Object eval(Reader reader, Bindings n) throws ScriptException {
        throw new UnsupportedOperationException("Bindings for Polyglot language cannot be created explicitly");
    }

    @Override
    public void put(String key, Object value) {
        defaultContext.getBindings(ScriptContext.ENGINE_SCOPE).put(key, value);
    }

    @Override
    public Object get(String key) {
        return defaultContext.getBindings(ScriptContext.ENGINE_SCOPE).get(key);
    }

    @Override
    public Bindings getBindings(int scope) {
        return defaultContext.getBindings(scope);
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {
        defaultContext.setBindings(bindings, scope);
    }

    @Override
    public Bindings createBindings() {
        GraalPyBindings bindings = (GraalPyBindings) defaultContext.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.clear();
        return new SimpleBindings();
    }

    @Override
    public ScriptContext getContext() {
        return defaultContext;
    }

    @Override
    public void setContext(ScriptContext context) {
        throw new UnsupportedOperationException("The context of a Polyglot ScriptEngine cannot be modified.");
    }

    @Override
    public GraalPyEngineFactory getFactory() {
        return factory;
    }

    @Override
    public Object invokeMethod(Object thiz, String name, Object... args)
            throws ScriptException, NoSuchMethodException {
        try {
            Value receiver = defaultContext.getContext().asValue(thiz);
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
    public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getInterface(Class<T> interfaceClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getInterface(Object thiz, Class<T> interfaceClass) {
        return defaultContext.getContext().asValue(thiz).as(interfaceClass);
    }
}
