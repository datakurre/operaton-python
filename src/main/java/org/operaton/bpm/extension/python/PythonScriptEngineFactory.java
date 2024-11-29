package org.operaton.bpm.extension.python;

import org.graalvm.home.Version;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;
import org.graalvm.python.embedding.utils.VirtualFileSystem;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PythonScriptEngineFactory implements ScriptEngineFactory {
    private static final String LANGUAGE_ID = "python";

    private final Engine polyglotEngine = Engine.newBuilder().build();
    private final Language language = polyglotEngine.getLanguages().get(LANGUAGE_ID);

    private WeakReference<Engine> defaultEngine;

    private WeakReference<Context> defaultContext;

    private WeakReference<GraalPyPolyglotReader> defaultIn;

    private WeakReference<GraalPyPolyglotWriter> defaultOut;

    private WeakReference<GraalPyPolyglotWriter> defaultErr;

    public PythonScriptEngineFactory() {
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
        return new PythonScriptEngine(this);
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

    private static Context.Builder createContextBuilder() {
        return Context.newBuilder(LANGUAGE_ID).
            // set true to allow experimental options
            allowExperimentalOptions(false).
            // setting false will deny all privileges unless configured below
            allowAllAccess(false).
            // allows python to access the java language
            allowHostAccess(HostAccess.ALL).
            // allow creating python threads
            allowCreateThread(true).
            // allow running Python native extensions
            allowNativeAccess(true).
            // allow exporting Python values to polyglot bindings and accessing Java
            // from Python
            allowPolyglotAccess(PolyglotAccess.ALL).
            // choose the backend for the POSIX module
            option("python.PosixModuleBackend", "java").
            // equivalent to the Python -B flag
            option("python.DontWriteBytecodeFlag", "true").
            // equivalent to the Python -v flag
            option("python.VerboseFlag", System.getenv("PYTHONVERBOSE") != null ? "true" : "false").
            // log level
            option("log.python.level", System.getenv("PYTHONVERBOSE") != null ? "FINE" : "SEVERE").
            // equivalent to setting the PYTHONWARNINGS environment variable
            option("python.WarnOptions", System.getenv("PYTHONWARNINGS") == null ? "" : System.getenv("PYTHONWARNINGS")).
            // print Python exceptions directly
            option("python.AlwaysRunExcepthook", "true").
            // Force to automatically import site.py module, to make Python packages
            // available
            option("python.ForceImportSite", "true").
            // Do not warn if running without JIT. This can be desirable for short
            // running scripts to reduce memory footprint.
            // option("engine.WarnInterpreterOnly", "false").
            // causes the interpreter to always assume hash-based pycs are valid
            option("python.CheckHashPycsMode", "never");
    }

    // These are copied from VirtualFileSystem to be available in this scope
    static final String VFS_ROOT = "/org.graalvm.python.vfs";
    static final String VFS_HOME = "home";
    static final String VFS_VENV = "venv";
    static final String VFS_PROJ = "proj";
    static final String VFS_SRC = "src";
    private static final String VENV_PREFIX = VFS_ROOT + "/" + VFS_VENV;
    private static final String HOME_PREFIX = VFS_ROOT + "/" + VFS_HOME;
    // TODO see GR-54915, deprecated and should be removed after 24.2.0
    private static final String PROJ_PREFIX = VFS_ROOT + "/" + VFS_PROJ;
    private static final String SRC_PREFIX = VFS_ROOT + "/" + VFS_SRC;
    //

    public static Context.Builder contextBuilder(VirtualFileSystem vfs) {
        final String vfsVenvPath = vfs.resourcePathToPlatformPath(VENV_PREFIX);
        final String vfsHomePath = vfs.resourcePathToPlatformPath(HOME_PREFIX);
        final String vfsProjPath = vfs.resourcePathToPlatformPath(PROJ_PREFIX);
        final String vfsSrcPath = vfs.resourcePathToPlatformPath(SRC_PREFIX);
        final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows");

        return createContextBuilder().
            // allow access to the virtual and the host filesystem, as well as sockets
            allowIO(IOAccess.newBuilder().allowHostSocketAccess(true).fileSystem(vfs).build()).
            // The sys.executable path, a virtual path that is used by the interpreter
            // to discover packages
            option("python.Executable", vfsVenvPath + (isWindows ? "\\Scripts\\python.exe" : "/bin/python")).
            // Set the python home to be read from the embedded resources
            option("python.PythonHome", vfsHomePath).
            // Set python path to point to sources stored in
            // src/main/resources/org.graalvm.python.vfs/src
            option("python.PythonPath", vfsSrcPath + File.pathSeparator + vfsProjPath).
            // pass the path to be executed
            option("python.InputFilePath", vfsSrcPath);
    }

    private static Context createDefaultContext(Engine engine, Bindings globalBindings,
                                                GraalPyPolyglotReader in, GraalPyPolyglotWriter out, GraalPyPolyglotWriter err) {
        VirtualFileSystem vfs = VirtualFileSystem.create();
        Context.Builder builder = contextBuilder(vfs)
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
