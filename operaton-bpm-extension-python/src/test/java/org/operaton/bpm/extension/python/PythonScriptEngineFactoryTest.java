package org.operaton.bpm.extension.python;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.script.ScriptEngine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PythonScriptEngineFactory}. */
class PythonScriptEngineFactoryTest {

  private static PythonScriptEngineFactory factory;

  @BeforeAll
  static void setUp() {
    factory = new PythonScriptEngineFactory();
  }

  @Test
  void getEngineName_returnsNonEmpty() {
    assertThat(factory.getEngineName()).isNotBlank();
  }

  @Test
  void getEngineVersion_returnsNonEmpty() {
    assertThat(factory.getEngineVersion()).isNotBlank();
  }

  @Test
  void getLanguageName_returnsPython() {
    assertThat(factory.getLanguageName()).isEqualToIgnoringCase("python");
  }

  @Test
  void getLanguageVersion_returnsNonEmpty() {
    assertThat(factory.getLanguageVersion()).isNotBlank();
  }

  @Test
  void getExtensions_containsPy() {
    assertThat(factory.getExtensions()).contains("py");
  }

  @Test
  void getMimeTypes_returnsNonEmpty() {
    assertThat(factory.getMimeTypes()).isNotEmpty();
  }

  @Test
  void getNames_containsPython() {
    List<String> names = factory.getNames();
    assertThat(names).isNotEmpty();
    assertThat(names).anyMatch(n -> n.toLowerCase().contains("python"));
  }

  @Test
  void getParameter_engine() {
    assertThat(factory.getParameter(ScriptEngine.ENGINE)).isNotNull();
  }

  @Test
  void getParameter_engineVersion() {
    assertThat(factory.getParameter(ScriptEngine.ENGINE_VERSION)).isNotNull();
  }

  @Test
  void getParameter_language() {
    assertThat(factory.getParameter(ScriptEngine.LANGUAGE)).isNotNull();
  }

  @Test
  void getParameter_name() {
    assertThat(factory.getParameter(ScriptEngine.NAME)).isEqualTo("python");
  }

  @Test
  void getParameter_unknown_returnsNull() {
    assertThat(factory.getParameter("nonexistent")).isNull();
  }

  @Test
  void getMethodCallSyntax_noArgs() {
    assertThat(factory.getMethodCallSyntax("obj", "method")).isEqualTo("obj.method()");
  }

  @Test
  void getMethodCallSyntax_withArgs() {
    assertThat(factory.getMethodCallSyntax("obj", "method", "a", "b"))
        .isEqualTo("obj.method(a, b)");
  }

  @Test
  void getOutputStatement() {
    assertThat(factory.getOutputStatement("\"hello\"")).isEqualTo("print(\"hello\")");
  }

  @Test
  void getProgram_singleStatement() {
    assertThat(factory.getProgram("x = 1")).isEqualTo("x = 1\n");
  }

  @Test
  void getProgram_multipleStatements() {
    assertThat(factory.getProgram("x = 1", "y = 2")).isEqualTo("x = 1\ny = 2\n");
  }

  @Test
  void getScriptEngine_returnsNonNull() {
    ScriptEngine engine = factory.getScriptEngine();
    assertThat(engine).isNotNull();
    assertThat(engine).isInstanceOf(PythonScriptEngine.class);
    ((PythonScriptEngine) engine).close();
  }

  @Test
  void getScriptEngine_returnsDistinctInstances() {
    ScriptEngine engine1 = factory.getScriptEngine();
    ScriptEngine engine2 = factory.getScriptEngine();
    assertThat(engine1).isNotSameAs(engine2);
    ((PythonScriptEngine) engine1).close();
    ((PythonScriptEngine) engine2).close();
  }
}
