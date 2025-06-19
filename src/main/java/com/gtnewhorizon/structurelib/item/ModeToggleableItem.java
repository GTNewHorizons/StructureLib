package com.gtnewhorizon.structurelib.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;

public class ModeToggleableItem extends Item {

    public enum TriggerMode {
        BUILDING,
        UPDATING,
        REMOVING
    }

    public static TriggerMode getMode(ItemStack held) {
        if (held == null || !held.hasTagCompound() || !held.getTagCompound().hasKey(TAG_MODE)) {
            // default behavior
            return TriggerMode.BUILDING;
        }
        // get user-defined mode
        return TriggerMode.values()[held.getTagCompound().getInteger(TAG_MODE)];
    }

    public static void cycleMode(ItemStack held) {
        if (held.getTagCompound() == null) held.setTagCompound(new NBTTagCompound());
        if (!held.getTagCompound().hasKey(TAG_MODE)) held.getTagCompound().setTag(TAG_MODE, new NBTTagInt(0));
        held.getTagCompound().setInteger(TAG_MODE, (held.getTagCompound().getInteger(TAG_MODE) + 1) % 3);
    }

    protected static final String TAG_MODE = "mode";
}
