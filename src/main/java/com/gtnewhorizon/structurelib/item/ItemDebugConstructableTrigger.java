package com.gtnewhorizon.structurelib.item;

import static com.gtnewhorizon.structurelib.StructureLibAPI.MOD_ID;

/**
 * Hologram projector that always performs a creative build, whatever the player's game mode.
 */
public class ItemDebugConstructableTrigger extends ItemConstructableTrigger {

    public ItemDebugConstructableTrigger() {
        setUnlocalizedName("structurelib.debugConstructableTrigger");
        setTextureName(MOD_ID + ":itemDebugConstructableTrigger");
    }
}
