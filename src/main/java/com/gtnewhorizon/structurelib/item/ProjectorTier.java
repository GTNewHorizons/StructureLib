package com.gtnewhorizon.structurelib.item;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants.NBT;

/** V2's configured structure tier, independent of the physical item count. */
public final class ProjectorTier {

    private static final String TAG = "structurelib.projectorTier";

    private ProjectorTier() {}

    public static int getTier(ItemStack stack) {
        if (stack == null) return 1;
        if (!(stack.getItem() instanceof ItemConstructableTriggerV2)) return Math.max(1, stack.stackSize);
        if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey(TAG, NBT.TAG_INT)) return 1;
        return Math.max(1, stack.getTagCompound().getInteger(TAG));
    }

    public static void setTier(ItemStack stack, int tier) {
        if (stack == null || !(stack.getItem() instanceof ItemConstructableTriggerV2) || tier < 1)
            throw new IllegalArgumentException("V2 projector tier must be positive");
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setInteger(TAG, tier);
    }

    /** Controller implementations still expect their selected tier in ItemStack.stackSize. */
    public static ItemStack forStructure(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemConstructableTriggerV2)) return stack;
        ItemStack effective = stack.copy();
        effective.stackSize = getTier(stack);
        return effective;
    }
}
