package com.gtnewhorizon.structurelib.util;

import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;

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
