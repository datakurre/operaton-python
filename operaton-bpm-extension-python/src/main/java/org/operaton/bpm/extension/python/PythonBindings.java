package org.operaton.bpm.extension.python;

import java.util.*;
import javax.script.Bindings;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 * JSR-223 {@link Bindings} implementation backed by GraalPy's polyglot bindings.
 *
 * <p>Delegates all key-value operations to the Python language bindings of a GraalPy {@link
 * Context}, allowing Java code to read and write Python globals.
 */
public class PythonBindings implements Bindings {

  private static final String LANGUAGE_ID = "python";
  private final Context context;

  PythonBindings(Context context) {
    this.context = context;
  }

  @Override
  public int size() {
    return context.getBindings(LANGUAGE_ID).getMemberKeys().size();
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public boolean containsValue(Object value) {
    for (String s : keySet()) {
      if (Objects.equals(get(s), value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void clear() {
    for (String key : new ArrayList<>(keySet())) {
      remove(key);
    }
  }

  @Override
  public Set<String> keySet() {
    return context.getBindings(LANGUAGE_ID).getMemberKeys();
  }

  @Override
  public Collection<Object> values() {
    List<Object> values = new ArrayList<>();
    for (String s : keySet()) {
      values.add(get(s));
    }
    return values;
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    Set<Entry<String, Object>> values = new LinkedHashSet<>();
    for (String s : keySet()) {
      values.add(new AbstractMap.SimpleImmutableEntry<>(s, get(s)));
    }
    return values;
  }

  @Override
  public Object put(String name, Object value) {
    Object previous = get(name);
    context.getBindings(LANGUAGE_ID).putMember(name, value);
    return previous;
  }

  @Override
  public void putAll(Map<? extends String, ?> toMerge) {
    for (Entry<? extends String, ?> e : toMerge.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }

  @Override
  public boolean containsKey(Object key) {
    if (key instanceof String s) {
      return context.getBindings(LANGUAGE_ID).hasMember(s);
    }
    return false;
  }

  @Override
  public Object get(Object key) {
    if (key instanceof String s) {
      Value value = context.getBindings(LANGUAGE_ID).getMember(s);
      if (value != null) {
        return value.as(Object.class);
      }
    }
    return null;
  }

  @Override
  public Object remove(Object key) {
    Object prev = get(key);
    if (prev != null) {
      context.getBindings(LANGUAGE_ID).removeMember((String) key);
      return prev;
    }
    return null;
  }
}
