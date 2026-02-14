package org.operaton.bpm.extension.python;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleBindings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive unit tests for {@link PythonScriptContext}. */
class PythonScriptContextTest {

  private PythonScriptEngineFactory factory;
  private PythonScriptEngine engine;
  private ScriptContext context;

  @BeforeEach
  void setUp() {
    factory = new PythonScriptEngineFactory();
    engine = (PythonScriptEngine) factory.getScriptEngine();
    context = engine.getContext();
  }

  @AfterEach
  void tearDown() {
    if (engine != null) {
      engine.close();
    }
  }

  @Nested
  class BindingsManagement {

    @Test
    void getBindings_engineScope() {
      Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
      assertThat(bindings).isNotNull();
      assertThat(bindings).isInstanceOf(PythonBindings.class);
    }

    @Test
    void getBindings_globalScope() {
      Bindings bindings = context.getBindings(ScriptContext.GLOBAL_SCOPE);
      assertThat(bindings).isNotNull();
    }

    @Test
    void getBindings_invalidScope() {
      assertThatThrownBy(() -> context.getBindings(999))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setBindings_engineScope() {
      Bindings newBindings = new SimpleBindings();
      newBindings.put("x", 42);
      context.setBindings(newBindings, ScriptContext.ENGINE_SCOPE);
      assertThat(context.getBindings(ScriptContext.ENGINE_SCOPE)).isSameAs(newBindings);
    }

    @Test
    void setBindings_globalScope() {
      Bindings newBindings = new SimpleBindings();
      newBindings.put("g", "global");
      context.setBindings(newBindings, ScriptContext.GLOBAL_SCOPE);
      assertThat(context.getBindings(ScriptContext.GLOBAL_SCOPE)).isSameAs(newBindings);
    }

    @Test
    void setBindings_invalidScope() {
      assertThatThrownBy(() -> context.setBindings(new SimpleBindings(), 999))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class AttributeOperations {

    @Test
    void setAttribute_andGetAttribute_engineScope() {
      context.setAttribute("attr1", "value1", ScriptContext.ENGINE_SCOPE);
      assertThat(context.getAttribute("attr1", ScriptContext.ENGINE_SCOPE)).isEqualTo("value1");
    }

    @Test
    void setAttribute_andGetAttribute_globalScope() {
      context.setAttribute("attr2", "value2", ScriptContext.GLOBAL_SCOPE);
      assertThat(context.getAttribute("attr2", ScriptContext.GLOBAL_SCOPE)).isEqualTo("value2");
    }

    @Test
    void removeAttribute_engineScope() {
      context.setAttribute("toRemove", "value", ScriptContext.ENGINE_SCOPE);
      Object removed = context.removeAttribute("toRemove", ScriptContext.ENGINE_SCOPE);
      assertThat(removed).isEqualTo("value");
    }

    @Test
    void removeAttribute_globalScope() {
      context.setAttribute("toRemoveG", "valueG", ScriptContext.GLOBAL_SCOPE);
      Object removed = context.removeAttribute("toRemoveG", ScriptContext.GLOBAL_SCOPE);
      assertThat(removed).isEqualTo("valueG");
    }

    @Test
    void getAttribute_searchesEngineThenGlobal() {
      context.setAttribute("lookup", "engine", ScriptContext.ENGINE_SCOPE);
      context.setAttribute("lookupG", "global", ScriptContext.GLOBAL_SCOPE);

      assertThat(context.getAttribute("lookup")).isEqualTo("engine");
      assertThat(context.getAttribute("lookupG")).isEqualTo("global");
    }

    @Test
    void getAttribute_engineScopeTakesPrecedence() {
      context.setAttribute("priority", "global", ScriptContext.GLOBAL_SCOPE);
      context.setAttribute("priority", "engine", ScriptContext.ENGINE_SCOPE);
      assertThat(context.getAttribute("priority")).isEqualTo("engine");
    }

    @Test
    void getAttribute_notFound() {
      assertThat(context.getAttribute("doesNotExist12345")).isNull();
    }

    @Test
    void getAttributesScope_engineScope() {
      context.setAttribute("scopeTest", "value", ScriptContext.ENGINE_SCOPE);
      assertThat(context.getAttributesScope("scopeTest")).isEqualTo(ScriptContext.ENGINE_SCOPE);
    }

    @Test
    void getAttributesScope_globalScope() {
      context.setAttribute("scopeTestG", "value", ScriptContext.GLOBAL_SCOPE);
      assertThat(context.getAttributesScope("scopeTestG")).isEqualTo(ScriptContext.GLOBAL_SCOPE);
    }

    @Test
    void getAttributesScope_notFound() {
      assertThat(context.getAttributesScope("nonExistentScope12345")).isEqualTo(-1);
    }
  }

  @Nested
  class IoStreams {

    @Test
    void getWriter_returnsNonNull() {
      assertThat(context.getWriter()).isNotNull();
    }

    @Test
    void getErrorWriter_returnsNonNull() {
      assertThat(context.getErrorWriter()).isNotNull();
    }

    @Test
    void getReader_returnsNonNull() {
      assertThat(context.getReader()).isNotNull();
    }

    @Test
    void setWriter_changesWriter() {
      Writer writer = new StringWriter();
      context.setWriter(writer);
      assertThat(context.getWriter()).isSameAs(writer);
    }

    @Test
    void setErrorWriter_changesErrorWriter() {
      Writer writer = new StringWriter();
      context.setErrorWriter(writer);
      assertThat(context.getErrorWriter()).isSameAs(writer);
    }

    @Test
    void setReader_changesReader() {
      Reader reader = new StringReader("input");
      context.setReader(reader);
      assertThat(context.getReader()).isSameAs(reader);
    }
  }

  @Nested
  class Scopes {

    @Test
    void getScopes_returnsEngineAndGlobal() {
      List<Integer> scopes = context.getScopes();
      assertThat(scopes).contains(ScriptContext.ENGINE_SCOPE, ScriptContext.GLOBAL_SCOPE);
    }

    @Test
    void getScopes_returnsImmutableList() {
      List<Integer> scopes = context.getScopes();
      assertThatThrownBy(() -> scopes.add(999)).isInstanceOf(UnsupportedOperationException.class);
    }
  }
}
