package com.money.mimi.wallet;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class TestSupport {
    private TestSupport() {
    }

    static Context newContext() {
        return new TestContext();
    }

    private static final class TestContext extends ContextWrapper {
        private final Map<String, SharedPreferences> preferencesByName = new HashMap<>();

        private TestContext() {
            super(null);
        }

        @Override
        public Context getApplicationContext() {
            return this;
        }

        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
            SharedPreferences preferences = preferencesByName.get(name);
            if (preferences == null) {
                preferences = new InMemorySharedPreferences();
                preferencesByName.put(name, preferences);
            }
            return preferences;
        }

        @Override
        public String getPackageName() {
            return "com.money.mimi.test";
        }
    }

    private static final class InMemorySharedPreferences implements SharedPreferences {
        private final Map<String, Object> values = new HashMap<>();
        private final Set<OnSharedPreferenceChangeListener> listeners = new HashSet<>();

        @Override public Map<String, ?> getAll() { return new HashMap<>(values); }
        @Override public String getString(String key, String defValue) { Object value = values.get(key); return value instanceof String ? (String) value : defValue; }
        @SuppressWarnings("unchecked")
        @Override public Set<String> getStringSet(String key, Set<String> defValues) { Object value = values.get(key); return value instanceof Set ? new HashSet<>((Set<String>) value) : defValues; }
        @Override public int getInt(String key, int defValue) { Object value = values.get(key); return value instanceof Number ? ((Number) value).intValue() : defValue; }
        @Override public long getLong(String key, long defValue) { Object value = values.get(key); return value instanceof Number ? ((Number) value).longValue() : defValue; }
        @Override public float getFloat(String key, float defValue) { Object value = values.get(key); return value instanceof Number ? ((Number) value).floatValue() : defValue; }
        @Override public boolean getBoolean(String key, boolean defValue) { Object value = values.get(key); return value instanceof Boolean ? (Boolean) value : defValue; }
        @Override public boolean contains(String key) { return values.containsKey(key); }
        @Override public Editor edit() { return new EditorImpl(); }
        @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) { if (listener != null) listeners.add(listener); }
        @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) { listeners.remove(listener); }

        private final class EditorImpl implements Editor {
            private final Map<String, Object> pending = new HashMap<>();
            private final Set<String> removals = new HashSet<>();
            private boolean clearAll;

            @Override public Editor putString(String key, String value) { return value == null ? remove(key) : putValue(key, value); }
            @Override public Editor putStringSet(String key, Set<String> value) { return putValue(key, value == null ? null : new HashSet<>(value)); }
            @Override public Editor putInt(String key, int value) { return putValue(key, value); }
            @Override public Editor putLong(String key, long value) { return putValue(key, value); }
            @Override public Editor putFloat(String key, float value) { return putValue(key, value); }
            @Override public Editor putBoolean(String key, boolean value) { return putValue(key, value); }

            @Override
            public Editor remove(String key) {
                removals.add(key);
                pending.remove(key);
                return this;
            }

            @Override
            public Editor clear() {
                clearAll = true;
                pending.clear();
                removals.clear();
                return this;
            }

            @Override public boolean commit() { applyChanges(); return true; }
            @Override public void apply() { applyChanges(); }

            private Editor putValue(String key, Object value) {
                removals.remove(key);
                pending.put(key, value);
                return this;
            }

            private void applyChanges() {
                Set<String> changedKeys = new HashSet<>();
                if (clearAll) {
                    changedKeys.addAll(values.keySet());
                    values.clear();
                }
                for (String key : removals) {
                    if (values.containsKey(key)) {
                        changedKeys.add(key);
                    }
                    values.remove(key);
                }
                for (Map.Entry<String, Object> entry : pending.entrySet()) {
                    values.put(entry.getKey(), entry.getValue());
                    changedKeys.add(entry.getKey());
                }
                for (OnSharedPreferenceChangeListener listener : listeners) {
                    for (String key : changedKeys) {
                        listener.onSharedPreferenceChanged(InMemorySharedPreferences.this, key);
                    }
                }
            }
        }
    }
}