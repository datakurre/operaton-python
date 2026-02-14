package org.operaton.bpm.extension.python;

import static org.assertj.core.api.Assertions.assertThat;

import javax.script.ScriptException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that sequential Python executions on separate engine instances do not have side effects.
 *
 * <p>Verifies that the GraalPy context-per-engine architecture prevents state leakage between
 * sequential script executions, which is critical for Operaton where multiple process instances may
 * execute Python scripts in sequence.
 */
class PythonSequentialIsolationTest {

  private PythonScriptEngineFactory factory;

  @BeforeEach
  void setUp() {
    factory = new PythonScriptEngineFactory();
  }

  @Test
  void sequentialEnginesDoNotShareGlobals() throws ScriptException {
    // First execution sets a global
    PythonScriptEngine engine1 = (PythonScriptEngine) factory.getScriptEngine();
    try {
      engine1.eval("leaked_global = 'should_not_leak'");
      assertThat(engine1.get("leaked_global")).isEqualTo("should_not_leak");
    } finally {
      engine1.close();
    }

    // Second execution should not see the global
    PythonScriptEngine engine2 = (PythonScriptEngine) factory.getScriptEngine();
    try {
      assertThat(engine2.get("leaked_global")).isNull();
    } finally {
      engine2.close();
    }
  }

  @Test
  void sequentialEnginesDoNotShareImports() throws ScriptException {
    // First execution imports a module and sets state
    PythonScriptEngine engine1 = (PythonScriptEngine) factory.getScriptEngine();
    try {
      engine1.eval(
          """
          import sys
          sys.my_custom_attr = 'from_engine1'
          """);
    } finally {
      engine1.close();
    }

    // Second execution should not see the custom attribute
    PythonScriptEngine engine2 = (PythonScriptEngine) factory.getScriptEngine();
    try {
      Object result = engine2.eval("getattr(__import__('sys'), 'my_custom_attr', 'not_found')");
      assertThat(result).isEqualTo("not_found");
    } finally {
      engine2.close();
    }
  }

  @Test
  void sequentialEnginesDoNotShareFunctions() throws ScriptException {
    // First execution defines a function
    PythonScriptEngine engine1 = (PythonScriptEngine) factory.getScriptEngine();
    try {
      engine1.eval("def my_isolated_func(): return 'engine1'");
      Object result = engine1.eval("my_isolated_func()");
      assertThat(result).isEqualTo("engine1");
    } finally {
      engine1.close();
    }

    // Second execution should not see the function
    PythonScriptEngine engine2 = (PythonScriptEngine) factory.getScriptEngine();
    try {
      assertThat(engine2.get("my_isolated_func")).isNull();
    } finally {
      engine2.close();
    }
  }

  @Test
  void multipleSequentialExecutionsAllIsolated() throws ScriptException {
    for (int i = 0; i < 5; i++) {
      PythonScriptEngine engine = (PythonScriptEngine) factory.getScriptEngine();
      try {
        // Each iteration should start clean
        assertThat(engine.get("counter")).isNull();
        engine.eval("counter = " + i);
        assertThat(engine.get("counter")).isEqualTo(i);
      } finally {
        engine.close();
      }
    }
  }

  @Test
  void sequentialExecutionsOnSameEngineShareState() throws ScriptException {
    PythonScriptEngine engine = (PythonScriptEngine) factory.getScriptEngine();
    try {
      // Sequential evals on same engine DO share state (by design)
      engine.eval("accumulator = 0");
      engine.eval("accumulator += 10");
      engine.eval("accumulator += 20");
      assertThat(engine.get("accumulator")).isEqualTo(30);
    } finally {
      engine.close();
    }
  }
}
