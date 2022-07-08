package com.gtnewhorizon.structurelib.util;

import net.minecraft.nbt.NBTTagCompound;

import java.util.Set;

/**
 * not related to a certain mod!
 */
public class MiscUtils {
    private MiscUtils() {}

    @SuppressWarnings("unchecked")
    public static Set<String> getTagKeys(NBTTagCompound tag) {
        return tag.func_150296_c();
    }
}
