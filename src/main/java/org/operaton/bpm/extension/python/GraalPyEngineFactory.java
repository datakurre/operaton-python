package org.operaton.bpm.extension.python;

import org.graalvm.home.Version;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Language;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

public final class GraalPyEngineFactory implements ScriptEngineFactory {
    private static final String LANGUAGE_ID = "python";

    private final Engine polyglotEngine = Engine.newBuilder().build();
    private final Language language = polyglotEngine.getLanguages().get(LANGUAGE_ID);

    private WeakReference<Engine> defaultEngine;

    private WeakReference<Context> defaultContext;

    private WeakReference<GraalPyPolyglotReader> defaultIn;

    private WeakReference<GraalPyPolyglotWriter> defaultOut;

    private WeakReference<GraalPyPolyglotWriter> defaultErr;

    public GraalPyEngineFactory() {
        this.defaultEngine = null; // lazy
        this.defaultContext = null; // lazy
    }

    @Override
    public String getEngineName() {
        return language.getImplementationName();
    }

    @Override
    public String getEngineVersion() {
        return Version.getCurrent().toString();
    }

    @Override
    public List<String> getExtensions() {
        return List.of(LANGUAGE_ID);
    }

    @Override
    public List<String> getMimeTypes() {
        return List.copyOf(language.getMimeTypes());
    }

    @Override
    public List<String> getNames() {
        return List.of(language.getName(), LANGUAGE_ID, language.getImplementationName());
    }

    @Override
    public String getLanguageName() {
        return language.getName();
    }

    @Override
    public String getLanguageVersion() {
        return language.getVersion();
    }

    @Override
    public Object getParameter(final String key) {
        switch (key) {
            case ScriptEngine.ENGINE:
                return getEngineName();
            case ScriptEngine.ENGINE_VERSION:
                return getEngineVersion();
            case ScriptEngine.LANGUAGE:
                return getLanguageName();
            case ScriptEngine.LANGUAGE_VERSION:
                return getLanguageVersion();
            case ScriptEngine.NAME:
                return LANGUAGE_ID;
        }
        return null;
    }

    @Override
    public String getMethodCallSyntax(final String obj, final String m, final String... args) {
        throw new UnsupportedOperationException("Unimplemented method 'getMethodCallSyntax'");
    }

    @Override
    public String getOutputStatement(final String toDisplay) {
        throw new UnsupportedOperationException("Unimplemented method 'getOutputStatement'");
    }

    @Override
    public String getProgram(final String... statements) {
        throw new UnsupportedOperationException("Unimplemented method 'getProgram'");
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new GraalPyScriptEngine(this);
    }

    private static Engine createDefaultEngine() {
        return Engine.newBuilder().build();
    }

    public Engine getEngine() {
        Engine engine = defaultEngine == null ? null : defaultEngine.get();
        if (engine == null) {
            engine = createDefaultEngine();
            defaultEngine = new WeakReference<>(engine);
        }
        return engine;
    }

    private static Context createDefaultContext(Engine engine, Bindings globalBindings,
                                                GraalPyPolyglotReader in, GraalPyPolyglotWriter out, GraalPyPolyglotWriter err) {
        Context.Builder builder = Context.newBuilder(LANGUAGE_ID)
                .in(in)
                .out(out)
                .err(err)
                .engine(engine)
                .allowExperimentalOptions(true)
                .allowAllAccess(true);
        if (globalBindings != null) {
            for (Map.Entry<String, Object> entry : globalBindings.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    builder.option(entry.getKey(), (String) value);
                }
            }
        }
        builder.option("python.PythonPath", "/home/datakurre/Work/github/datakurre/camunda-python/venv/lib/python3.10/site-packages");
        return builder.build();
    }

    public GraalPyPolyglotReader getIn () {
        GraalPyPolyglotReader in = defaultIn == null ? null : defaultIn.get();
        if (in == null) {
            in = new GraalPyPolyglotReader(new InputStreamReader(System.in));
            defaultIn = new WeakReference<>(in);
        }
        return in;
    }

    public GraalPyPolyglotWriter getOut () {
        GraalPyPolyglotWriter out = defaultOut == null ? null : defaultOut.get();
        if (out == null) {
            out = new GraalPyPolyglotWriter(new OutputStreamWriter(System.out));
            defaultOut = new WeakReference<>(out);
        }
        return out;
    }

    public GraalPyPolyglotWriter getErr () {
        GraalPyPolyglotWriter err = defaultErr == null ? null : defaultErr.get();
        if (err == null) {
            err = new GraalPyPolyglotWriter(new OutputStreamWriter(System.err));
            defaultErr = new WeakReference<>(err);
        }
        return err;
    }

    public Context getContext(Bindings globalBindings) {
        Context context = defaultContext == null ? null : defaultContext.get();
        if (context == null) {
            context = createDefaultContext(getEngine(), globalBindings, getIn(), getOut(), getErr());
            defaultContext = new WeakReference<>(context);
        }
        return context;
    }
}
