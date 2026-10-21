package com.gtnewhorizon.structurelib.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ChannelSettingsTest {

    @Test
    void existingOutOfRangeValuesMustBeFixedBeforeApplyingAnySettings() {
        Map<String, Integer> initial = new HashMap<>();
        initial.put("hidden_custom", 268435456);
        ChannelSettings draft = new ChannelSettings(initial);
        draft.set("coil", "2");
        assertFalse(draft.isValid());
        draft.reset("hidden_custom");
        assertTrue(draft.isValid());
    }

    @Test
    void editingAndResettingDoNotMutateOriginalSettings() {
        Map<String, Integer> original = new HashMap<>();
        original.put("coil", 2);
        ChannelSettings draft = new ChannelSettings(original);
        assertTrue(draft.set("coil", "3"));
        assertEquals(Integer.valueOf(2), original.get("coil"));
        draft.reset("coil");
        assertNull(draft.get("coil"));
        assertEquals(Integer.valueOf(2), original.get("coil"));
    }

    @Test
    void invalidNumbersNeverReplaceThePreviousValue() {
        ChannelSettings draft = new ChannelSettings(new HashMap<>());
        assertTrue(draft.set("custom", "268435455"));
        for (String invalid : Arrays.asList("", "0", "-1", "1.5", "268435456", "abc")) {
            assertFalse(draft.set("custom", invalid));
            assertEquals(Integer.valueOf(268435455), draft.get("custom"));
        }
        assertFalse(draft.set(" ", "1"));
    }

    @Test
    void configuredAndCustomChannelsRemainDiscoverableAndSortFirst() {
        ChannelSettings draft = new ChannelSettings(new HashMap<>());
        draft.set("custom", "7");
        draft.set("coil", "2");
        assertEquals(Arrays.asList("coil", "custom", "glass"), draft.channels(Arrays.asList("glass", "coil")));
        draft.resetAll();
        assertEquals(Arrays.asList("coil", "glass"), draft.channels(Arrays.asList("glass", "coil")));
    }

    @Test
    void exportedSettingsAreIndependentSnapshots() {
        ChannelSettings draft = new ChannelSettings(new HashMap<>());
        draft.set("coil", "2");
        Map<String, Integer> saved = draft.snapshot();
        draft.set("coil", "3");
        assertEquals(Integer.valueOf(2), saved.get("coil"));
    }
}
