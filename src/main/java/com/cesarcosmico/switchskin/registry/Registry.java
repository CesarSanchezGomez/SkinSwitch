package com.cesarcosmico.switchskin.registry;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Registry<K, V> {

    private final Map<K, V> entries = new ConcurrentHashMap<>();

    public void register(@NotNull K key, @NotNull V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        entries.put(key, value);
        onMutated();
    }

    public @NotNull Optional<V> get(@NotNull K key) {
        return Optional.ofNullable(entries.get(key));
    }

    public boolean contains(@NotNull K key) {
        return entries.containsKey(key);
    }

    public @NotNull Set<K> keys() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    public @NotNull Collection<V> values() {
        return Collections.unmodifiableCollection(entries.values());
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void clear() {
        entries.clear();
        onMutated();
    }

    protected void onMutated() {
    }
}
