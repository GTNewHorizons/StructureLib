package com.gtnewhorizon.structurelib.item;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Only V2 projectors support mode and wireless tags; absence still means Build. */
public enum ProjectorMode {

    BUILD,
    DISMANTLE;

    private static final String TAG = "structurelib.projectorDismantle";
    private static final String WIRELESS_TAG = "structurelib.projectorWireless";

    public static ProjectorMode get(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof ItemConstructableTriggerV2
                && stack.hasTagCompound()
                && stack.getTagCompound().getBoolean(TAG))
            return DISMANTLE;
        return BUILD;
    }

    public static boolean isWireless(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemConstructableTriggerV2
                && stack.hasTagCompound()
                && stack.getTagCompound().getBoolean(WIRELESS_TAG);
    }

    /** The link is independent of the selected Build/Dismantle mode. */
    public static ProjectorMode toggle(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemConstructableTriggerV2))
            throw new IllegalArgumentException("Not a projector");
        ProjectorMode next = get(stack) == BUILD ? DISMANTLE : BUILD;
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().removeTag(TAG);
        if (next == DISMANTLE) stack.getTagCompound().setBoolean(TAG, true);
        if (stack.getTagCompound().hasNoTags()) stack.setTagCompound(null);
        return next;
    }

    /** A wireless bind temporarily overlays the selected mode without overwriting it. */
    public static void setWireless(ItemStack stack, boolean linked) {
        if (stack == null || !(stack.getItem() instanceof ItemConstructableTriggerV2)) return;
        if (linked) {
            if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
            stack.getTagCompound().setBoolean(WIRELESS_TAG, true);
        } else if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag(WIRELESS_TAG);
            if (stack.getTagCompound().hasNoTags()) stack.setTagCompound(null);
        }
    }
}
