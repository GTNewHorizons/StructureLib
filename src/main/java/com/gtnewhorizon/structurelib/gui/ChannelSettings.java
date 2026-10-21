package com.gtnewhorizon.structurelib.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** An editing session, independent of the held projector until explicitly applied. */
final class ChannelSettings {

    // SetChannelDataMessage uses a four-byte Forge VarInt.
    static final int MAX_VALUE = 0x0FFFFFFF;
    private final Map<String, Integer> values;

    ChannelSettings(Map<String, Integer> initial) {
        values = new HashMap<>(initial);
    }

    static Integer parse(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 && parsed <= MAX_VALUE ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    boolean set(String channel, String value) {
        Integer parsed = parse(value);
        if (channel == null || channel.trim().isEmpty() || parsed == null) return false;
        values.put(channel, parsed);
        return true;
    }

    Integer get(String channel) {
        return values.get(channel);
    }

    void reset(String channel) {
        values.remove(channel);
    }

    void resetAll() {
        values.clear();
    }

    Map<String, Integer> snapshot() {
        return new HashMap<>(values);
    }

    boolean isValid() {
        return values.values().stream().allMatch(value -> value != null && value > 0 && value <= MAX_VALUE);
    }

    List<String> channels(Collection<String> registered) {
        HashSet<String> union = new HashSet<>(registered);
        union.addAll(values.keySet());
        List<String> result = new ArrayList<>(union);
        result.sort(Comparator.comparing((String key) -> !values.containsKey(key)).thenComparing(key -> key));
        return result;
    }
}
