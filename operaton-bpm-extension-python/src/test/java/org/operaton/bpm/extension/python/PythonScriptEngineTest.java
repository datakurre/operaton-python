package org.operaton.bpm.extension.python;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.StringReader;
import javax.script.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PythonScriptEngine}. */
class PythonScriptEngineTest {

  private PythonScriptEngineFactory factory;
  private PythonScriptEngine engine;

  @BeforeEach
  void setUp() {
    factory = new PythonScriptEngineFactory();
    engine = (PythonScriptEngine) factory.getScriptEngine();
  }

  @AfterEach
  void tearDown() {
    if (engine != null) {
      engine.close();
    }
  }

  @Nested
  class Eval {

    @Test
    void evalSimpleExpression() throws ScriptException {
      Object result = engine.eval("1 + 2");
      assertThat(result).isEqualTo(3);
    }

    @Test
    void evalStringExpression() throws ScriptException {
      Object result = engine.eval("'hello ' + 'world'");
      assertThat(result).isEqualTo("hello world");
    }

    @Test
    void evalFString() throws ScriptException {
      engine.put("name", "Operaton");
      Object result = engine.eval("f'Hello {name}'");
      assertThat(result).isEqualTo("Hello Operaton");
    }

    @Test
    void evalNoneReturnsNullOrEmpty() throws ScriptException {
      Object result = engine.eval("None");
      // GraalPy maps Python None to an empty map via as(Object.class)
      if (result != null) {
        assertThat(result).isInstanceOf(java.util.Map.class);
        assertThat(((java.util.Map<?, ?>) result)).isEmpty();
      }
    }

    @Test
    void evalSyntaxError() {
      assertThatThrownBy(() -> engine.eval("def !!!")).isInstanceOf(ScriptException.class);
    }

    @Test
    void evalRuntimeError() {
      assertThatThrownBy(() -> engine.eval("1 / 0")).isInstanceOf(ScriptException.class);
    }

    @Test
    void evalMultilineScript() throws ScriptException {
      String script =
          """
          x = 10
          y = 20
          x + y
          """;
      Object result = engine.eval(script);
      assertThat(result).isEqualTo(30);
    }

    @Test
    void evalReaderString() throws ScriptException {
      StringReader reader = new StringReader("1 + 2");
      Object result = engine.eval(reader);
      assertThat(result).isEqualTo(3);
    }

    @Test
    void evalReaderWithScriptContext() throws ScriptException {
      SimpleScriptContext ctx = new SimpleScriptContext();
      ctx.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
      ctx.getBindings(ScriptContext.ENGINE_SCOPE).put("x", 5);

      StringReader reader = new StringReader("x * 3");
      Object result = engine.eval(reader, ctx);
      assertThat(result).isEqualTo(15);
    }

    @Test
    void evalReaderWithBindings() throws ScriptException {
      Bindings bindings = new SimpleBindings();
      bindings.put("y", 7);

      StringReader reader = new StringReader("y + 3");
      Object result = engine.eval(reader, bindings);
      assertThat(result).isEqualTo(10);
    }

    @Test
    void evalWithNullScriptContext() throws ScriptException {
      Object result = engine.eval("42", (ScriptContext) null);
      assertThat(result).isEqualTo(42);
    }

    @Test
    void evalWithNullBindings() throws ScriptException {
      engine.put("z", 10);
      Object result = engine.eval("z", (Bindings) null);
      assertThat(result).isEqualTo(10);
    }
  }

  @Nested
  class ScriptContextHandling {

    @Test
    void evalWithScriptContext_injectsBindings() throws ScriptException {
      SimpleScriptContext ctx = new SimpleScriptContext();
      ctx.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
      ctx.getBindings(ScriptContext.ENGINE_SCOPE).put("greeting", "Hello");
      ctx.getBindings(ScriptContext.ENGINE_SCOPE).put("target", "World");

      Object result = engine.eval("greeting + ' ' + target", ctx);
      assertThat(result).isEqualTo("Hello World");
    }

    @Test
    void evalWithBindings_injectsVariables() throws ScriptException {
      Bindings bindings = new SimpleBindings();
      bindings.put("x", 42);

      Object result = engine.eval("x * 2", bindings);
      assertThat(result).isEqualTo(84);
    }

    @Test
    void evalWithScriptContext_globalAndEngineScope() throws ScriptException {
      SimpleScriptContext ctx = new SimpleScriptContext();
      Bindings globalBindings = new SimpleBindings();
      globalBindings.put("fromGlobal", "global_value");
      ctx.setBindings(globalBindings, ScriptContext.GLOBAL_SCOPE);

      Bindings engineBindings = engine.createBindings();
      engineBindings.put("fromEngine", "engine_value");
      ctx.setBindings(engineBindings, ScriptContext.ENGINE_SCOPE);

      Object result = engine.eval("fromGlobal + ' ' + fromEngine", ctx);
      assertThat(result).isEqualTo("global_value engine_value");
    }

    @Test
    void evalWithScriptContext_engineScopeOverridesGlobal() throws ScriptException {
      SimpleScriptContext ctx = new SimpleScriptContext();
      Bindings globalBindings = new SimpleBindings();
      globalBindings.put("var", "global");
      ctx.setBindings(globalBindings, ScriptContext.GLOBAL_SCOPE);

      Bindings engineBindings = engine.createBindings();
      engineBindings.put("var", "engine");
      ctx.setBindings(engineBindings, ScriptContext.ENGINE_SCOPE);

      Object result = engine.eval("var", ctx);
      assertThat(result).isEqualTo("engine");
    }
  }

  @Nested
  class BindingsOperations {

    @Test
    void putAndGet() {
      engine.put("myVar", "myValue");
      assertThat(engine.get("myVar")).isEqualTo("myValue");
    }

    @Test
    void getBindings_returnsNonNull() {
      Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
      assertThat(bindings).isNotNull();
    }

    @Test
    void createBindings_returnsNonNull() {
      Bindings bindings = engine.createBindings();
      assertThat(bindings).isNotNull();
    }

    @Test
    void setBindings_appliesValues() throws ScriptException {
      SimpleBindings bindings = new SimpleBindings();
      bindings.put("val", 99);
      engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

      Object result = engine.eval("val");
      assertThat(result).isEqualTo(99);
    }

    @Test
    void setBindings_withNull() {
      // Setting null bindings should not throw
      engine.setBindings(null, ScriptContext.ENGINE_SCOPE);
    }

    @Test
    void getBindings_globalScope() {
      Bindings bindings = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
      assertThat(bindings).isNotNull();
    }

    @Test
    void getNonExistentKey_returnsNull() {
      assertThat(engine.get("nonexistent_key_xyz")).isNull();
    }
  }

  @Nested
  class Compilation {

    @Test
    void compileAndEval() throws ScriptException {
      CompiledScript compiled = engine.compile("2 ** 10");
      Object result = compiled.eval(engine.getContext());
      assertThat(result).isEqualTo(1024);
    }

    @Test
    void compiledScriptReuse() throws ScriptException {
      CompiledScript compiled = engine.compile("x + 1");

      engine.put("x", 10);
      Object result1 = compiled.eval(engine.getContext());
      assertThat(result1).isEqualTo(11);

      engine.put("x", 20);
      Object result2 = compiled.eval(engine.getContext());
      assertThat(result2).isEqualTo(21);
    }

    @Test
    void compileSyntaxError() {
      assertThatThrownBy(() -> engine.compile("def !!!")).isInstanceOf(ScriptException.class);
    }

    @Test
    void compileFromReader() throws ScriptException {
      StringReader reader = new StringReader("3 * 7");
      CompiledScript compiled = engine.compile(reader);
      Object result = compiled.eval(engine.getContext());
      assertThat(result).isEqualTo(21);
    }

    @Test
    void compiledScriptEvalWithNullContext() throws ScriptException {
      CompiledScript compiled = engine.compile("100");
      Object result = compiled.eval((ScriptContext) null);
      assertThat(result).isEqualTo(100);
    }

    @Test
    void compiledScriptEvalWithBindings() throws ScriptException {
      CompiledScript compiled = engine.compile("a + b");
      SimpleScriptContext ctx = new SimpleScriptContext();
      ctx.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
      ctx.getBindings(ScriptContext.ENGINE_SCOPE).put("a", 10);
      ctx.getBindings(ScriptContext.ENGINE_SCOPE).put("b", 20);
      Object result = compiled.eval(ctx);
      assertThat(result).isEqualTo(30);
    }

    @Test
    void compiledScriptEvalWithGlobalBindings() throws ScriptException {
      CompiledScript compiled = engine.compile("g_var + 1");
      SimpleScriptContext ctx = new SimpleScriptContext();
      Bindings globalBindings = new SimpleBindings();
      globalBindings.put("g_var", 100);
      ctx.setBindings(globalBindings, ScriptContext.GLOBAL_SCOPE);
      ctx.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
      Object result = compiled.eval(ctx);
      assertThat(result).isEqualTo(101);
    }

    @Test
    void compiledScriptEvalRuntimeError() throws ScriptException {
      CompiledScript compiled = engine.compile("1 / 0");
      assertThatThrownBy(() -> compiled.eval(engine.getContext()))
          .isInstanceOf(ScriptException.class);
    }

    @Test
    void compiledScriptGetEngine() throws ScriptException {
      CompiledScript compiled = engine.compile("1");
      assertThat(compiled.getEngine()).isSameAs(engine);
    }
  }

  @Nested
  class Invocation {

    @Test
    void invokeFunction() throws Exception {
      engine.eval("def add(a, b): return a + b");
      Object result = engine.invokeFunction("add", 3, 4);
      assertThat(result).isEqualTo(7);
    }

    @Test
    void invokeFunction_notFound() {
      assertThatThrownBy(() -> engine.invokeFunction("nonexistent"))
          .isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void invokeMethod_onPythonObject() throws Exception {
      engine.eval(
          """
          class Greeter:
              def greet(self, name):
                  return f"Hello {name}"
          obj = Greeter()
          """);
      Object obj = engine.get("obj");
      Object result = engine.invokeMethod(obj, "greet", "World");
      assertThat(result).isEqualTo("Hello World");
    }

    @Test
    void invokeMethod_noSuchMethod() throws Exception {
      engine.eval("class Foo:\n    pass\nfoo = Foo()");
      Object foo = engine.get("foo");
      assertThatThrownBy(() -> engine.invokeMethod(foo, "nonexistent"))
          .isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void invokeFunction_notCallable() throws Exception {
      engine.eval("not_func = 42");
      assertThatThrownBy(() -> engine.invokeFunction("not_func"))
          .isInstanceOf(NoSuchMethodException.class)
          .hasMessageContaining("not callable");
    }

    @Test
    void getInterface_classLevel() throws Exception {
      engine.eval(
          """
          def run():
              return "ran"
          """);
      Runnable r = engine.getInterface(Runnable.class);
      assertThat(r).isNotNull();
    }

    @Test
    void getInterface_objectLevel() throws Exception {
      engine.eval(
          """
          class MyRunnable:
              def run(self):
                  pass
          obj = MyRunnable()
          """);
      Object obj = engine.get("obj");
      Runnable r = engine.getInterface(obj, Runnable.class);
      assertThat(r).isNotNull();
    }
  }

  @Nested
  class Isolation {

    @Test
    void separateEnginesHaveIsolatedState() throws ScriptException {
      PythonScriptEngine engine2 = (PythonScriptEngine) factory.getScriptEngine();
      try {
        engine.eval("shared_var = 'engine1'");
        engine2.eval("shared_var = 'engine2'");

        assertThat(engine.get("shared_var")).isEqualTo("engine1");
        assertThat(engine2.get("shared_var")).isEqualTo("engine2");
      } finally {
        engine2.close();
      }
    }
  }

  @Nested
  class ContextAndFactory {

    @Test
    void getFactory_returnsSameFactory() {
      assertThat(engine.getFactory()).isSameAs(factory);
    }

    @Test
    void getContext_returnsNonNull() {
      assertThat(engine.getContext()).isNotNull();
    }

    @Test
    void setContext_throwsUnsupported() {
      assertThatThrownBy(() -> engine.setContext(new SimpleScriptContext()))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Nested
  class Close {

    @Test
    void closeReleasesResources() {
      PythonScriptEngine eng = (PythonScriptEngine) factory.getScriptEngine();
      eng.close();
      // After close, eval should throw since context is closed
      assertThatThrownBy(() -> eng.eval("1 + 1")).isInstanceOf(Exception.class);
    }
  }
}
