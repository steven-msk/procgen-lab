package com.seven.procgen.engine.registry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

public final class Registry<T> {
    public static final Identifier ROOT_IDENTIFIER = Identifier.of("root");
    private final RegistryKey<? extends Registry<T>> registryKey;
    private final BiMap<Identifier, T> idToEntry = HashBiMap.create();
    private final BiMap<RegistryKey<T>, T> keyToEntry = HashBiMap.create();
    private final Object2IntMap<T> entryToRawId = Object2IntMaps.emptyMap();
    private final ObjectList<T> rawIdToEntry = new ObjectArrayList<>();
    private int nextRawId;

    private Registry(RegistryKey<? extends Registry<T>> registryKey) {
        this.registryKey = registryKey;
    }

    public static <V, T extends V> T register(Registry<V> registry, Identifier id, T entry) {
        RegistryKey<V> key = RegistryKey.of(registry.getKey(), id);
        return registry.add(key, entry);
    }

    public static <T> T register(Registry<? super T> registry, String id, T entry) {
        return register(registry, Identifier.of(id), entry);
    }

    private <V extends T> V add(RegistryKey<T> key, V entry) {
        Identifier id = key.value();
        this.idToEntry.put(id, entry);
        this.keyToEntry.put(key, entry);
        int rawId = this.nextRawId++;
        this.entryToRawId.put(entry, rawId);
        this.rawIdToEntry.add(rawId, entry);
        return entry;
    }

    public Identifier getId(T entry) {
        return this.idToEntry.inverse().get(entry);
    }

    public T get(Identifier id) {
        return this.idToEntry.get(id);
    }

    public Optional<T> getOrEmpty(Identifier id) {
        return Optional.ofNullable(this.get(id));
    }

    public Optional<T> getOrEmpty(RegistryKey<T> key) {
        return Optional.ofNullable(this.get(key));
    }

    public T getOrThrow(RegistryKey<T> key) {
        return this.getOrEmpty(key).orElseThrow(() -> new NoSuchElementException("Entry not found: " + key));
    }

    public T get(RegistryKey<T> key) {
        return this.keyToEntry.get(key);
    }

    public Set<Identifier> getIds() {
        return this.idToEntry.keySet();
    }

    public boolean containsId(Identifier id) {
        return this.idToEntry.containsKey(id);
    }

    public Optional<RegistryKey<T>> getKey(T entry) {
        return Optional.ofNullable(this.keyToEntry.inverse().get(entry));
    }

    public int getRawId(T entry) {
        return this.entryToRawId.getInt(entry);
    }

    public T get(int rawId) {
        return this.rawIdToEntry.get(rawId);
    }

    public RegistryKey<? extends Registry<T>> getKey() {
        return this.registryKey;
    }
}
