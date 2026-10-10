package com.seven.procgen.engine.registry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.seven.procgen.engine.logging.LogUtil;
import com.seven.procgen.engine.util.collection.IndexedIterable;
import it.unimi.dsi.fastutil.objects.*;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class Registry<T> implements IndexedIterable<T>, RegistryEntryLookup<T> {
    private static final Logger LOGGER = LogUtil.GetLogger();
    private static final LinkedHashMap<Identifier, Supplier<?>> LOADERS = Maps.newLinkedHashMap();
    public static final Identifier ROOT_IDENTIFIER = Identifier.of("root");
    public static final Registry<Registry<?>> REGISTRIES = new Registry<>(RegistryKey.ofRegistry(ROOT_IDENTIFIER));
    private static RegistryEntryLookup.RegistryLookup bootstrapLookup;
    private static final Set<Identifier> LOADED_REGISTRIES = Sets.newHashSet();
    private boolean frozen;
    private final RegistryKey<? extends Registry<T>> registryKey;
    private final BiMap<Identifier, RegistryEntry<T>> idToEntry = HashBiMap.create();
    private final BiMap<RegistryKey<T>, RegistryEntry<T>> keyToEntry = HashBiMap.create();
    private final Reference2IntMap<T> entryToRawId = new Reference2IntOpenHashMap<>();
    private final ObjectList<RegistryEntry<T>> rawIdToEntry = new ObjectArrayList<>();
    private final Reference2ObjectMap<T, RegistryEntry<T>> valueToEntry = new Reference2ObjectOpenHashMap<>();
    private int nextRawId;

    private static <T> Registry<T> create(RegistryKey<? extends Registry<T>> key, Registry.Initializer<T> initializer) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(initializer, "initializer");
        if (LOADERS.containsKey(key.value())) throw new IllegalStateException("Duplicate registry: " + key.value());

        Registry<T> registry = new Registry<>(key);
        Registry.register(REGISTRIES, key.value(), registry);
        LOADERS.put(key.value(), () -> initializer.run(registry, getBootstrapLookup()));
        return registry;
    }

    private static RegistryEntryLookup.RegistryLookup getBootstrapLookup() {
        if (bootstrapLookup != null) return bootstrapLookup;

        return bootstrapLookup = new RegistryEntryLookup.RegistryLookup() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<RegistryEntryLookup<T>> getOptional(RegistryKey<? extends Registry<? extends T>> registryKey) {
                Identifier id = registryKey.value();
                if (!LOADED_REGISTRIES.contains(id)) {
                    LOGGER.error("Attempt to retrieve a registry that has not been loaded yet: {}", id);
                }
                return Optional.ofNullable(REGISTRIES.get(id)).map(r -> (RegistryEntryLookup<T>)r);
            }
        };
    }

    public static void bootstrap() {
        LOADERS.forEach((id, loader) -> {
            LOADED_REGISTRIES.add(id);
            if (loader.get() == null) {
                LOGGER.error("Failed to load registry: {}", id);
            }
            int size = REGISTRIES.get(id).size();
            if (size == 0) {
                LOGGER.warn("Registry is empty after load: {}", id);
            } else {
                LOGGER.info("Registry {} loaded with {} entries", id, size);
            }
        });
        freezeAll();
    }

    private static void freezeAll() {
        LOGGER.info("Freezing {} registries", REGISTRIES.size());
        REGISTRIES.freeze();
        REGISTRIES.forEach(Registry::freeze);
    }

    private Registry(RegistryKey<? extends Registry<T>> registryKey) {
        this.registryKey = registryKey;
        this.entryToRawId.defaultReturnValue(IndexedIterable.ABSENT_RAW_ID);
    }

    public static <V, T extends V> T register(Registry<V> registry, RegistryKey<V> key, T entry) {
        return registry.add(key, entry);
    }

    public static <V, T extends V> T register(Registry<V> registry, Identifier id, T entry) {
        RegistryKey<V> key = RegistryKey.of(registry.getKey(), id);
        return register(registry, key, entry);
    }

    public static <T> T register(Registry<? super T> registry, String id, T entry) {
        return register(registry, Identifier.of(id), entry);
    }

    public Registry<T> freeze() {
        if (this.frozen) throw new IllegalStateException("Registry already frozen: " + this.getIdAsString());
        this.frozen = true;
        return this;
    }

    private <V extends T> V add(RegistryKey<T> key, V entry) {
        Objects.requireNonNull(entry, "entry");
        Objects.requireNonNull(key, "key");
        if (this.frozen) throw new IllegalStateException("Registry is frozen: " + this.getIdAsString());

        Identifier id = key.value();
        if (this.idToEntry.containsKey(id)) throw new IllegalStateException("Duplicate id: " + id);
        if (this.keyToEntry.containsKey(key)) throw new IllegalStateException("Duplicate key: " + key);
        if (this.valueToEntry.containsKey(entry)) throw new IllegalStateException("Value already registered: " + entry);

        RegistryEntry<T> registryEntry = RegistryEntry.of(this, key, entry);
        this.idToEntry.put(id, registryEntry);
        this.keyToEntry.put(key, registryEntry);
        int rawId = this.nextRawId++;
        this.entryToRawId.put(entry, rawId);
        this.rawIdToEntry.add(rawId, registryEntry);
        this.valueToEntry.put(entry, registryEntry);
        return entry;
    }

    public boolean containsId(Identifier id) {
        return this.idToEntry.containsKey(id);
    }

    public boolean contains(RegistryKey<T> key) {
        return this.keyToEntry.containsKey(key);
    }

    public RegistryKey<? extends Registry<T>> getKey() {
        return this.registryKey;
    }

    public Optional<RegistryEntry<T>> getEntry(RegistryKey<T> key) {
        return Optional.ofNullable(this.keyToEntry.get(key));
    }

    public Optional<RegistryEntry<T>> getEntry(Identifier id) {
        return Optional.ofNullable(this.idToEntry.get(id));
    }

    public @Nullable RegistryEntry<T> getEntry(T value) {
        return this.valueToEntry.get(value);
    }

    public Set<Map.Entry<RegistryKey<T>, T>> getEntrySet() {
        return this.rawIdToEntry.stream()
                .map(e -> Map.entry(e.key(), e.value()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public @Nullable Identifier getId(T value) {
        RegistryEntry<T> e = this.valueToEntry.get(value);
        return e == null ? null : e.key().value();
    }

    public Set<Identifier> getIds() {
        return Collections.unmodifiableSet(this.idToEntry.keySet());
    }

    public Optional<RegistryKey<T>> getKey(T value) {
        return Optional.ofNullable(this.valueToEntry.get(value)).map(RegistryEntry::key);
    }

    public Set<RegistryKey<T>> getKeys() {
        return Collections.unmodifiableSet(this.keyToEntry.keySet());
    }

    public Optional<T> getOrEmpty(@Nullable RegistryKey<T> key) {
        return Optional.ofNullable(this.get(key));
    }

    public Optional<T> getOrEmpty(@Nullable Identifier id) {
        return Optional.ofNullable(this.get(id));
    }

    public @Nullable T get(@Nullable RegistryKey<T> key) {
        if (key == null) return null;
        RegistryEntry<T> entry = this.keyToEntry.get(key);
        return entry == null ? null : entry.value();
    }

    public @Nullable T get(@Nullable Identifier id) {
        if (id == null) return null;
        RegistryEntry<T> entry = this.idToEntry.get(id);
        return entry == null ? null : entry.value();
    }

    @Override
    public @Nullable T get(int index) {
        if (index < 0 || index >= this.size()) return null;
        return this.rawIdToEntry.get(index).value();
    }

    public int getRawId(T value) {
        return this.entryToRawId.getInt(value);
    }

    public Optional<RegistryEntry<T>> getEntry(int rawId) {
        if (rawId < 0 || rawId >= this.size()) return Optional.empty();
        return Optional.of(this.rawIdToEntry.get(rawId));
    }

    @Override
    public Iterator<T> iterator() {
        return this.rawIdToEntry.stream().map(RegistryEntry::value).iterator();
    }

    @Override
    public int size() {
        return this.rawIdToEntry.size();
    }

    @Override
    public String toString() {
        return "Registry[" + this.registryKey.value() + "]";
    }

    public String getIdAsString() {
        return this.registryKey.value().toString();
    }

    @Override
    public Optional<RegistryEntry<T>> getOptional(RegistryKey<T> key) {
        return this.getOrEmpty(key).map(this::getEntry);
    }

    private interface Initializer<T> {
        Object run(Registry<T> registry, RegistryEntryLookup.RegistryLookup lookup);
    }
}
