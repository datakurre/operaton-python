package org.operaton.bpm.extension.python;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive unit tests for {@link PythonBindings}. */
class PythonBindingsTest {

  private PythonScriptEngineFactory factory;
  private PythonScriptEngine engine;
  private Bindings bindings;

  @BeforeEach
  void setUp() {
    factory = new PythonScriptEngineFactory();
    engine = (PythonScriptEngine) factory.getScriptEngine();
    bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
  }

  @AfterEach
  void tearDown() {
    if (engine != null) {
      engine.close();
    }
  }

  @Nested
  class SizeAndEmpty {

    @Test
    void sizeReflectsPythonGlobals() {
      int initialSize = bindings.size();
      bindings.put("testVar", "value");
      assertThat(bindings.size()).isEqualTo(initialSize + 1);
    }

    @Test
    void isEmptyReturnsFalseWithGlobals() {
      // Python always has some built-in globals
      assertThat(bindings.isEmpty()).isFalse();
    }
  }

  @Nested
  class ContainsOperations {

    @Test
    void containsKey_existingKey() {
      bindings.put("myKey", "myValue");
      assertThat(bindings.containsKey("myKey")).isTrue();
    }

    @Test
    void containsKey_nonExistentKey() {
      assertThat(bindings.containsKey("nonExistentKey12345")).isFalse();
    }

    @Test
    void containsKey_nonStringKey() {
      assertThat(bindings.containsKey(42)).isFalse();
    }

    @Test
    void containsValue_existingValue() throws ScriptException {
      engine.eval("test_contains = 'findme'");
      assertThat(bindings.containsValue("findme")).isTrue();
    }

    @Test
    void containsValue_nonExistentValue() {
      assertThat(bindings.containsValue("impossiblevalue12345")).isFalse();
    }
  }

  @Nested
  class GetOperations {

    @Test
    void get_existingKey() {
      bindings.put("getKey", "getValue");
      assertThat(bindings.get("getKey")).isEqualTo("getValue");
    }

    @Test
    void get_nonExistentKey() {
      assertThat(bindings.get("nonExistentGetKey")).isNull();
    }

    @Test
    void get_nonStringKey() {
      assertThat(bindings.get(42)).isNull();
    }
  }

  @Nested
  class PutOperations {

    @Test
    void put_returnsOldValue() {
      bindings.put("putKey", "first");
      Object old = bindings.put("putKey", "second");
      assertThat(old).isEqualTo("first");
      assertThat(bindings.get("putKey")).isEqualTo("second");
    }

    @Test
    void put_returnsNullForNewKey() {
      Object old = bindings.put("brandNewKey", "value");
      assertThat(old).isNull();
    }

    @Test
    void putAll_mergesMap() {
      Map<String, Object> map = Map.of("key1", "val1", "key2", "val2");
      bindings.putAll(map);
      assertThat(bindings.get("key1")).isEqualTo("val1");
      assertThat(bindings.get("key2")).isEqualTo("val2");
    }
  }

  @Nested
  class RemoveOperations {

    @Test
    void remove_existingKey() {
      bindings.put("removeKey", "removeValue");
      Object removed = bindings.remove("removeKey");
      assertThat(removed).isEqualTo("removeValue");
      assertThat(bindings.containsKey("removeKey")).isFalse();
    }

    @Test
    void remove_nonExistentKey() {
      Object removed = bindings.remove("nonExistentRemoveKey");
      assertThat(removed).isNull();
    }
  }

  @Nested
  class CollectionViews {

    @Test
    void keySet_containsSetVariable() {
      bindings.put("keySetVar", "value");
      Set<String> keys = bindings.keySet();
      assertThat(keys).contains("keySetVar");
    }

    @Test
    void values_containsSetValue() throws ScriptException {
      engine.eval("values_var = 'uniqueValue12345'");
      Collection<Object> values = bindings.values();
      assertThat(values).contains("uniqueValue12345");
    }

    @Test
    void entrySet_containsSetEntry() {
      bindings.put("entryVar", "entryValue");
      Set<Map.Entry<String, Object>> entries = bindings.entrySet();
      assertThat(entries).anyMatch(e -> "entryVar".equals(e.getKey()));
    }
  }
}
