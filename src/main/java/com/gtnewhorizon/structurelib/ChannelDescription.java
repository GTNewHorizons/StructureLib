package com.gtnewhorizon.structurelib;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;

import com.gtnewhorizon.gtnhlib.util.map.ItemStackMap;

import cpw.mods.fml.common.Loader;

// this is not an API! go check StructureLibAPI instead!
public class ChannelDescription {

    private static final ItemStackMap<Set<ChannelDescription>> itemToChannels = new ItemStackMap<>(true);
    private static final Map<String, ChannelDescription> registry = new HashMap<>();
    private final String channel;
    private final Map<String, String> descriptions = new HashMap<>();
    private final ItemStackMap<Integer> items = new ItemStackMap<>(true);

    public static ChannelDescription get(final String channel) {
        if (StructureLibAPI.isDebugEnabled() && !channel.toLowerCase(Locale.ROOT).equals(channel))
            throw new IllegalArgumentException("Channel name can be lower case ONLY");
        return registry.computeIfAbsent(channel, ChannelDescription::new);
    }

    public static boolean has(final String channel) {
        if (StructureLibAPI.isDebugEnabled() && !channel.toLowerCase(Locale.ROOT).equals(channel))
            throw new IllegalArgumentException("Channel name can be lower case ONLY");
        return registry.containsKey(channel);
    }

    public static Collection<Map.Entry<String, Integer>> iterate(final ItemStack block) {
        Set<ChannelDescription> channels = itemToChannels.get(block);
        if (channels == null) return Collections.emptySet();
        return channels.stream().map(d -> new AbstractMap.SimpleImmutableEntry<>(d.channel, d.items.get(block)))
                .collect(Collectors.toList());
    }

    public static void set(final String channel, final String modid, final String description) {
        if (StructureLibAPI.isDebugEnabled() && !channel.toLowerCase(Locale.ROOT).equals(channel))
            throw new IllegalArgumentException("Channel name can be lower case ONLY");
        registry.computeIfAbsent(channel, ChannelDescription::new).add(modid, description);
    }

    public static void item(final String channel, int channelValue, ItemStack stack) {
        if (StructureLibAPI.isDebugEnabled()) {
            if (!channel.toLowerCase(Locale.ROOT).equals(channel)) {
                throw new IllegalArgumentException("Channel name can be lower case ONLY");
            }
            if (channelValue <= 0) {
                throw new IllegalArgumentException("Channel value must be greater than 0");
            }
        }
        ChannelDescription inst = registry.computeIfAbsent(channel, ChannelDescription::new);
        inst.items.put(stack, channelValue);
        itemToChannels.computeIfAbsent(stack, s -> new HashSet<>()).add(inst);
    }

    public static Map<String, ChannelDescription> getAll() {
        return Collections.unmodifiableMap(registry);
    }

    private ChannelDescription(String channel) {
        this.channel = channel;
    }

    public String getChannel() {
        return channel;
    }

    public ChannelDescription add(String modid, String description) {
        if (Loader.instance().getIndexedModList().get(modid) == null) {
            throw new IllegalArgumentException("Mod not found: " + modid);
        }
        descriptions.put(modid, description);
        return this;
    }

    public Map<String, String> getDescriptions() {
        return Collections.unmodifiableMap(descriptions);
    }

    public Map<ItemStack, Integer> getItems() {
        return Collections.unmodifiableMap(items);
    }
}
