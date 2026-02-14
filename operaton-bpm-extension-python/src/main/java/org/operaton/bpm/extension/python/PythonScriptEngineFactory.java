package org.operaton.bpm.extension.python;

import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import org.graalvm.home.Version;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Language;
import org.graalvm.python.embedding.VirtualFileSystem;

/**
 * JSR-223 {@link ScriptEngineFactory} implementation for GraalPy (Python on GraalVM).
 *
 * <p>Registered via {@code META-INF/services/javax.script.ScriptEngineFactory} so that Operaton's
 * {@link javax.script.ScriptEngineManager} can discover and create Python script engines for BPMN
 * script tasks with {@code scriptFormat="python"}.
 *
 * <p>Each call to {@link #getScriptEngine()} creates a new {@link PythonScriptEngine} with its own
 * isolated GraalPy {@link Context}, ensuring thread safety and preventing state leakage between
 * script executions.
 */
public final class PythonScriptEngineFactory implements ScriptEngineFactory {

  private static final String LANGUAGE_ID = "python";

  private final VirtualFileSystem vfs;
  private final Engine engine;
  private final Language language;

  /**
   * Creates a new factory, initializing a temporary GraalPy context to discover language metadata.
   */
  public PythonScriptEngineFactory() {
    this.vfs = createVfsOrNull();
    // Create a temporary context only to discover language metadata.
    // Script engines will each get their own context.
    try (Context tempContext = PythonResources.contextBuilder(vfs).allowAllAccess(true).build()) {
      this.engine = tempContext.getEngine();
      this.language = engine.getLanguages().get(LANGUAGE_ID);
    }
  }

  /**
   * Attempts to create a {@link VirtualFileSystem} from bundled resources. Returns {@code null} if
   * the VFS metadata ({@code fileslist.txt}) is not present, which can happen when no Python
   * packages are configured in the Maven plugin.
   */
  private static VirtualFileSystem createVfsOrNull() {
    try {
      return VirtualFileSystem.create();
    } catch (IllegalStateException e) {
      // VFS metadata not generated (no python packages configured)
      return null;
    }
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
    return List.of("py");
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
    return switch (key) {
      case ScriptEngine.ENGINE -> getEngineName();
      case ScriptEngine.ENGINE_VERSION -> getEngineVersion();
      case ScriptEngine.LANGUAGE -> getLanguageName();
      case ScriptEngine.LANGUAGE_VERSION -> getLanguageVersion();
      case ScriptEngine.NAME -> LANGUAGE_ID;
      default -> null;
    };
  }

  @Override
  public String getMethodCallSyntax(final String obj, final String m, final String... args) {
    StringBuilder sb = new StringBuilder();
    sb.append(obj).append(".").append(m).append("(");
    sb.append(String.join(", ", args));
    sb.append(")");
    return sb.toString();
  }

  @Override
  public String getOutputStatement(final String toDisplay) {
    return "print(" + toDisplay + ")";
  }

  @Override
  public String getProgram(final String... statements) {
    return String.join("\n", statements) + "\n";
  }

  /**
   * Creates a new {@link PythonScriptEngine} with its own isolated GraalPy {@link Context}.
   *
   * <p>Synchronized because the shared {@link VirtualFileSystem} is not thread-safe during context
   * initialization.
   *
   * @return a new script engine instance
   */
  @Override
  public synchronized ScriptEngine getScriptEngine() {
    return new PythonScriptEngine(this, vfs);
  }
}
