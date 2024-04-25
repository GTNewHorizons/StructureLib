package com.gtnewhorizon.structurelib;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import org.apache.commons.lang3.tuple.Pair;

import com.gtnewhorizon.structurelib.net.RegistryOrderSyncMessage;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

public class SortedRegistry<V> implements Iterable<V> {

    static final ConcurrentHashMap<String, WeakReference<SortedRegistry<?>>> ALL_REGISTRIES = new ConcurrentHashMap<>();
    static {
        RegistryOrderSyncMessage.Handler.setAllRegistries(ALL_REGISTRIES);
    }
    private final NavigableMap<String, V> store = new TreeMap<>();
    /**
     * Store stuff in an array for faster iteration access than a red black tree
     */
    private List<V> baked = Collections.emptyList();

    private final Map<UUID, Pair<List<String>, List<String>>> playerOrdering = new HashMap<>();
    private final Map<UUID, List<V>> playerBaked = new HashMap<>();

    /**
     * Construct an unsynchronized registry
     */
    public SortedRegistry() {}

    /**
     * Construct a synchronized registry whose ordering will be synced to the server
     *
     * @param name network name
     */

    public SortedRegistry(String name) {
        WeakReference<SortedRegistry<?>> oldValue = ALL_REGISTRIES.put(name, new WeakReference<>(this));
        if (oldValue != null && oldValue.get() != null) {
            throw new IllegalArgumentException("duplicate name");
        }
        FMLCommonHandler.instance().bus().register(this);
    }

    public static Set<String> getRegistryNames() {
        return ALL_REGISTRIES.keySet();
    }

    public static SortedRegistry<?> getRegistry(String name) {
        WeakReference<SortedRegistry<?>> ref = ALL_REGISTRIES.get(name);
        return ref != null ? ref.get() : null;
    }

    static void cleanup(MinecraftServer server) {
        @SuppressWarnings("unchecked")
        List<EntityPlayerMP> players = server.getConfigurationManager().playerEntityList;
        Set<UUID> onlinePlayerIDs = players.stream().map(EntityPlayerMP::getUniqueID).collect(Collectors.toSet());
        int removed = 0;
        for (WeakReference<SortedRegistry<?>> ref : ALL_REGISTRIES.values()) {
            SortedRegistry<?> registry = ref.get();
            if (registry == null) continue;
            for (Iterator<UUID> iterator = registry.playerOrdering.keySet().iterator(); iterator.hasNext();) {
                UUID uuid = iterator.next();
                if (!onlinePlayerIDs.contains(uuid)) {
                    iterator.remove();
                    registry.playerBaked.remove(uuid);
                    removed++;
                }
            }
        }
        if (removed > 0) {
            StructureLib.LOGGER.debug("Removed {} registry record for logged out players", removed);
        }
    }

    @SubscribeEvent
    public void onPlayerLogOut(PlayerEvent.PlayerLoggedOutEvent e) {
        playerOrdering.remove(e.player.getUniqueID());
        playerBaked.remove(e.player.getUniqueID());
    }

    public void register(String key, V val) {
        if (key == null || val == null) throw new NullPointerException();
        V old = store.putIfAbsent(key, val);
        if (old != null) {
            throw new IllegalArgumentException("Duplicate key: " + key);
        }
        baked = new ArrayList<>(store.values());
        for (Map.Entry<UUID, Pair<List<String>, List<String>>> e : playerOrdering.entrySet()) {
            makePlayerBaked(e.getKey(), e.getValue().getKey(), e.getValue().getValue());
        }
    }

    public void registerOrdering(EntityPlayerMP player, List<String> ordering, List<String> disabled) {
        registerOrdering(player.getUniqueID(), ordering, disabled);
    }

    public void registerOrdering(UUID playerID, List<String> ordering, List<String> disabled) {
        playerOrdering.put(playerID, Pair.of(ordering, disabled));
        makePlayerBaked(playerID, ordering, disabled);
    }

    private void makePlayerBaked(UUID player, List<String> ordering, List<String> disabled) {
        TreeMap<String, V> copy = new TreeMap<>(store);
        for (String s : disabled) {
            copy.remove(s);
        }
        List<V> b = new ArrayList<>();
        for (String k : ordering) {
            V v = copy.remove(k);
            if (v != null) {
                b.add(v);
            }
        }
        b.addAll(copy.values());
        playerBaked.put(player, b);
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

    public Iterable<V> getPlayerOrdering(@Nullable EntityPlayerMP player) {
        if (player == null) return baked;
        List<V> playerPreference = playerBaked.get(player.getUniqueID());
        if (playerPreference == null) return baked;
        return playerPreference;
    }

    public Stream<String> getPlayerOrderingKeys(@Nullable EntityPlayerMP player) {
        if (player == null) return store.keySet().stream();
        List<V> playerOrder = playerBaked.get(player.getUniqueID());
        if (playerOrder == null) return store.keySet().stream();

        // rebuild the order from the baked map
        Map<V, String> reverseMap = new IdentityHashMap<>();
        store.forEach((k, v) -> reverseMap.put(v, k));
        return playerOrder.stream().map(reverseMap::get);
    }

    Iterable<String> getCurrentOrdering() {
        return store.keySet();
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
