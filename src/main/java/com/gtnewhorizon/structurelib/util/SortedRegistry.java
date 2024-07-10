package com.gtnewhorizon.structurelib.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.function.Consumer;

public class SortedRegistry<V> implements Iterable<V> {

    private final NavigableMap<String, V> store = new TreeMap<>();
    /**
     * Store stuff in an array for faster iteration access than a red black tree
     */
    private List<V> baked = Collections.emptyList();

    public void register(String key, V val) {
        if (key == null || val == null) throw new NullPointerException();
        V old = store.putIfAbsent(key, val);
        if (old != null) {
            throw new IllegalArgumentException("Duplicate key: " + key);
        }
        // TODO allow a user to edit the order with a configuration file
        baked = new ArrayList<>(store.values());
    }

    public int size() {
        return store.size();
    }

    public boolean isEmpty() {
        return store.isEmpty();
    }

    public boolean containsKey(String key) {
        return store.containsKey(key);
    }

    public boolean containsValue(V value) {
        return store.containsValue(value);
    }

    public V get(String key) {
        return store.get(key);
    }

    @Override
    public Iterator<V> iterator() {
        return baked.iterator();
    }

    @Override
    public void forEach(Consumer<? super V> action) {
        baked.forEach(action);
    }

    @Override
    public Spliterator<V> spliterator() {
        return baked.spliterator();
    }
}
