package org.operaton.bpm.extension.python;

import org.graalvm.polyglot.Context;

import javax.script.Bindings;
import javax.script.ScriptContext;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Map.Entry;

public class GraalPyContext implements ScriptContext {

    private static final String LANGUAGE_ID = "python";
    private Context context;
    private final GraalPyEngineFactory factory;
    private final GraalPyPolyglotReader in;
    private final GraalPyPolyglotWriter out;
    private final GraalPyPolyglotWriter err;
    private Bindings globalBindings;

    GraalPyContext(GraalPyEngineFactory factory) {
        this.factory = factory;
        this.context = factory.getContext(getBindings(ScriptContext.GLOBAL_SCOPE));
        this.in = new GraalPyPolyglotReader(new InputStreamReader(System.in));
        this.out = new GraalPyPolyglotWriter(new OutputStreamWriter(System.out));
        this.err = new GraalPyPolyglotWriter(new OutputStreamWriter(System.err));
    }

    Context getContext() {
        if (context == null) {
            Context.Builder builder = Context.newBuilder(LANGUAGE_ID)
                    .in(this.in)
                    .out(this.out)
                    .err(this.err)
                    .engine(factory.getEngine())
                    .allowAllAccess(true);
            Bindings globalBindings = getBindings(ScriptContext.GLOBAL_SCOPE);
            if (globalBindings != null) {
                for (Entry<String, Object> entry : globalBindings.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        builder.option(entry.getKey(), (String) value);
                    }
                }
            }
            context = builder.build();
        }
        return context;
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {
        if (scope == ScriptContext.GLOBAL_SCOPE) {
            if (context == null) {
                globalBindings = bindings;
            } else if (!bindings.isEmpty()){
                throw new UnsupportedOperationException(
                        "Global bindings for Polyglot language can only be set before the context is initialized.");
            }
        } else {
            GraalPyBindings graalPyBindings = (GraalPyBindings) getBindings(scope);
            for (String key : bindings.keySet()) {
                if (!graalPyBindings.containsKey(key)) {
                    graalPyBindings.put(key, bindings.get(key));
                }
            }
        }
    }

    @Override
    public Bindings getBindings(int scope) {
        if (scope == ScriptContext.ENGINE_SCOPE) {
            return new GraalPyBindings(getContext().getBindings(LANGUAGE_ID));
        } else if (scope == ScriptContext.GLOBAL_SCOPE) {
            return globalBindings;
        } else {
            return null;
        }
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {
        if (scope == ScriptContext.ENGINE_SCOPE) {
            getBindings(scope).put(name, value);
        } else if (scope == ScriptContext.GLOBAL_SCOPE) {
            if (context == null) {
                globalBindings.put(name, value);
            } else {
                throw new IllegalStateException("Cannot modify global bindings after context creation.");
            }
        }
    }

    @Override
    public Object getAttribute(String name, int scope) {
        if (scope == ScriptContext.ENGINE_SCOPE) {
            return getBindings(scope).get(name);
        } else if (scope == ScriptContext.GLOBAL_SCOPE) {
            return globalBindings.get(name);
        }
        return null;
    }

    @Override
    public Object removeAttribute(String name, int scope) {
        Object prev = getAttribute(name, scope);
        if (prev != null) {
            if (scope == ScriptContext.ENGINE_SCOPE) {
                getBindings(scope).remove(name);
            } else if (scope == ScriptContext.GLOBAL_SCOPE) {
                if (context == null) {
                    globalBindings.remove(name);
                } else {
                    throw new IllegalStateException("Cannot modify global bindings after context creation.");
                }
            }
        }
        return prev;
    }

    @Override
    public Object getAttribute(String name) {
        return getAttribute(name, ScriptContext.ENGINE_SCOPE);
    }

    @Override
    public int getAttributesScope(String name) {
        if (getAttribute(name, ScriptContext.ENGINE_SCOPE) != null) {
            return ScriptContext.ENGINE_SCOPE;
        } else if (getAttribute(name, ScriptContext.GLOBAL_SCOPE) != null) {
            return ScriptContext.GLOBAL_SCOPE;
        }
        return -1;
    }

    @Override
    public Writer getWriter() {
        return this.out.getWriter();
    }

    @Override
    public Writer getErrorWriter() {
        return this.err.getWriter();
    }

    @Override
    public void setWriter(Writer writer) {
        this.out.setWriter(writer);
    }

    @Override
    public void setErrorWriter(Writer writer) {
        this.err.setWriter(writer);
    }

    @Override
    public Reader getReader() {
        return this.in.getReader();
    }

    @Override
    public void setReader(Reader reader) {
        this.in.setReader(reader);
    }

    @Override
    public List<Integer> getScopes() {
        return List.of(ScriptContext.ENGINE_SCOPE, ScriptContext.GLOBAL_SCOPE);
    }

}
