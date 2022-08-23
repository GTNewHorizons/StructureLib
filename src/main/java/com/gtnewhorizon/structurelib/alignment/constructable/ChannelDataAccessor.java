package com.gtnewhorizon.structurelib.alignment.constructable;

import static com.gtnewhorizon.structurelib.util.MiscUtils.getTagKeys;
import static net.minecraftforge.common.util.Constants.NBT.TAG_COMPOUND;

import com.gtnewhorizon.structurelib.StructureLib;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants.NBT;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class ChannelDataAccessor {
    private static final String SECONDARY_HINT_TAG = "channels";

    private ChannelDataAccessor() {}

    public static ItemStack withChannel(ItemStack masterStack, String channel) {
        if (channel == null || channel.isEmpty()) throw new IllegalArgumentException();
        if (StructureLib.DEBUG_MODE && !channel.toLowerCase(Locale.ROOT).equals(channel))
            throw new IllegalArgumentException("Channel name can be lower case ONLY");
        if (!masterStack.hasTagCompound()
                || !masterStack.stackTagCompound.hasKey(SECONDARY_HINT_TAG, TAG_COMPOUND)
                || !masterStack
                        .stackTagCompound
                        .getCompoundTag(SECONDARY_HINT_TAG)
                        .hasKey(channel, NBT.TAG_INT)) return masterStack;
        ItemStack ret = new ItemStack(
                masterStack.getItem(),
                masterStack.stackTagCompound.getCompoundTag(SECONDARY_HINT_TAG).getInteger(channel),
                Items.feather.getDamage(masterStack));
        ret.setTagCompound(masterStack.stackTagCompound);
        return ret;
    }

    public static boolean hasSubChannel(ItemStack masterStack) {
        return masterStack.hasTagCompound() && masterStack.stackTagCompound.hasKey(SECONDARY_HINT_TAG, TAG_COMPOUND);
    }

    public static boolean hasSubChannel(ItemStack masterStack, String channel) {
        if (StructureLib.DEBUG_MODE && !channel.toLowerCase(Locale.ROOT).equals(channel))
            throw new IllegalArgumentException("Channel name can be lower case ONLY");
        return !channel.isEmpty()
                && masterStack.hasTagCompound()
                && masterStack.stackTagCompound.hasKey(SECONDARY_HINT_TAG, TAG_COMPOUND)
                && masterStack
                        .stackTagCompound
                        .getCompoundTag(SECONDARY_HINT_TAG)
                        .hasKey(channel, NBT.TAG_INT);
    }

    public static int getChannelData(ItemStack masterStack, String channel) {
        if (channel == null || channel.isEmpty()) throw new IllegalArgumentException();
        if (StructureLib.DEBUG_MODE && !channel.toLowerCase(Locale.ROOT).equals(channel))
            throw new IllegalArgumentException("Channel name can be lower case ONLY");
        if (!masterStack.hasTagCompound()
                || !masterStack.stackTagCompound.hasKey(SECONDARY_HINT_TAG, TAG_COMPOUND)
                || !masterStack
                        .stackTagCompound
                        .getCompoundTag(SECONDARY_HINT_TAG)
                        .hasKey(channel, NBT.TAG_INT)) return masterStack.stackSize;
        return masterStack.stackTagCompound.getCompoundTag(SECONDARY_HINT_TAG).getInteger(channel);
    }

    public static void setChannelData(ItemStack masterStack, String channel, int data) {
        if (channel == null || channel.isEmpty()) throw new IllegalArgumentException();
        if (StructureLib.DEBUG_MODE && !channel.toLowerCase(Locale.ROOT).equals(channel))
            throw new IllegalArgumentException("Channel name can be lower case ONLY");
        if (data <= 0) throw new IllegalArgumentException();
        if (masterStack.stackTagCompound == null) masterStack.stackTagCompound = new NBTTagCompound();
        NBTTagCompound main = masterStack.stackTagCompound;
        if (!main.hasKey(SECONDARY_HINT_TAG, TAG_COMPOUND)) main.setTag(SECONDARY_HINT_TAG, new NBTTagCompound());
        main.getCompoundTag(SECONDARY_HINT_TAG).setInteger(channel, data);
    }

    public static void unsetChannelData(ItemStack masterStack, String channel) {
        if (channel == null || channel.isEmpty()) throw new IllegalArgumentException();
        if (StructureLib.DEBUG_MODE && !channel.toLowerCase(Locale.ROOT).equals(channel))
            throw new IllegalArgumentException("Channel name can be lower case ONLY");
        if (masterStack.stackTagCompound == null) masterStack.stackTagCompound = new NBTTagCompound();
        NBTTagCompound main = masterStack.stackTagCompound;
        if (!main.hasKey(SECONDARY_HINT_TAG, TAG_COMPOUND)) main.setTag(SECONDARY_HINT_TAG, new NBTTagCompound());
        NBTTagCompound tag = main.getCompoundTag(SECONDARY_HINT_TAG);
        tag.removeTag(channel);
        if (tag.hasNoTags()) main.removeTag(SECONDARY_HINT_TAG);
        if (main.hasNoTags()) masterStack.stackTagCompound = null;
    }

    public static void wipeChannelData(ItemStack masterStack) {
        if (masterStack.stackTagCompound != null) masterStack.stackTagCompound.removeTag(SECONDARY_HINT_TAG);
    }

    public static Stream<Entry<String, Integer>> iterateChannelData(ItemStack masterStack) {
        if (!hasSubChannel(masterStack)) return Stream.empty();
        NBTTagCompound tag = masterStack.stackTagCompound.getCompoundTag(SECONDARY_HINT_TAG);
        return getTagKeys(tag).stream().map(s -> new ImmutablePair<>(s, tag.getInteger(s)));
    }

    public static int countChannelData(ItemStack masterStack) {
        if (!hasSubChannel(masterStack)) return 0;
        NBTTagCompound tag = masterStack.stackTagCompound.getCompoundTag(SECONDARY_HINT_TAG);
        return tag.func_150296_c().size();
    }
}
