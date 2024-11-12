package org.operaton.bpm.extension.python;

import org.graalvm.polyglot.Value;

import javax.script.Bindings;
import java.util.*;

public final class GraalPyBindings implements Bindings {
    private Value languageBindings;

    GraalPyBindings(Value languageBindings) {
        this.languageBindings = languageBindings;
    }

    @Override
    public int size() {
        return keySet().size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsValue(Object value) {
        for (String s : keySet()) {
            if (get(s) == value) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void clear() {
        Set<String> builtins = (Set<String>) get("__graalpy_builtins__");
        if (builtins == null) {
            put("__graalpy_builtins__", new HashSet<>(keySet()));
        } else {
            Set<String> difference = new HashSet<>(keySet());
            difference.removeAll(builtins);
            for (String s : difference) {
               remove(s);
            }
        }
    }

    @Override
    public Set<String> keySet() {
        return languageBindings.getMemberKeys();
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
        Set<Entry<String, Object>> values = new HashSet<>();
        for (String s : keySet()) {
            values.add(new Entry<String, Object>() {
                @Override
                public String getKey() {
                    return s;
                }

                @Override
                public Object getValue() {
                    return get(s);
                }

                @Override
                public Object setValue(Object value) {
                    return put(s, value);
                }
            });
        }
        return values;
    }

    @Override
    public Object put(String name, Object value) {
        Object previous = get(name);
        languageBindings.putMember(name, value);
        return previous;
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> toMerge) {
        for (Entry<? extends String, ? extends Object> e : toMerge.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public boolean containsKey(Object key) {
        if (key instanceof String) {
            return languageBindings.hasMember((String) key);
        } else {
            return false;
        }
    }

    @Override
    public Object get(Object key) {
        if (key instanceof String) {
            Value value = languageBindings.getMember((String) key);
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
            languageBindings.removeMember((String) key);
            return prev;
        } else {
            return null;
        }
    }
}
