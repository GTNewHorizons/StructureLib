package com.gtnewhorizon.structurelib.alignment.constructable;

import static com.gtnewhorizon.structurelib.util.MiscUtils.getTagKeys;
import static net.minecraftforge.common.util.Constants.NBT.TAG_COMPOUND;

import java.util.Locale;
import java.util.Map.Entry;
import java.util.stream.Stream;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants.NBT;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import com.gtnewhorizon.structurelib.StructureLibAPI;

/**
 * Provide accessor methods for channel data from trigger item.
 *
 * <a href="{@docRoot}/overview-summary.html#channels">Channels section on overview</a>
 */
public class ChannelDataAccessor {

    private static final String SECONDARY_HINT_TAG = "channels";
    private static final String MASTER_OVERRIDE_TAG = "master";

    private ChannelDataAccessor() {}

    /**
     * Return a trigger item that
     * <ul>
     * <li>Still carry all the subchannel in some unspecified ways</li>
     * <li>Have its master channel data replaced with master channel override</li>
     * <li>No longer carry the master channel override</li>
     * </ul>
     *
     * If given trigger item has no master channel override, masterStack may or may not be returned to save performance.
     * Code within StructureLib can depend on this behavior, you cannot.
     * <p>
     * Should be called once on the raw trigger item before any {@link #withChannel(ItemStack, String)} call.
     * StructureLib already does this for hologram projector. If you call
     * {@link IConstructable#construct(ItemStack, boolean)} or similar methods with a raw trigger item yourself, call
     * this first.
     *
     * @param masterStack raw trigger item
     * @return a trigger item with new master channel
     */
    public static ItemStack withMasterData(ItemStack masterStack) {
        if (masterStack == null) throw new IllegalArgumentException();
        if (!hasMasterData(masterStack)) return masterStack;
        NBTTagCompound tag = (NBTTagCompound) masterStack.stackTagCompound.copy();
        ItemStack ret = new ItemStack(
                masterStack.getItem(),
                tag.getInteger(MASTER_OVERRIDE_TAG),
                Items.feather.getDamage(masterStack));
        tag.removeTag(MASTER_OVERRIDE_TAG);
        if (!tag.hasNoTags()) ret.setTagCompound(tag);
        return ret;
    }

    /**
     * Check if given trigger item contains master channel override
     * 
     * @param masterStack trigger stack to check
     * @return true if contains master channel override
     */
    public static boolean hasMasterData(ItemStack masterStack) {
        if (masterStack == null) throw new IllegalArgumentException();
        return masterStack.hasTagCompound() && masterStack.stackTagCompound.hasKey(MASTER_OVERRIDE_TAG, NBT.TAG_INT);
    }

    /**
     * Get the master channel data from given trigger item. Will use stack size instead if not overridden.
     * 
     * @param masterStack trigger stack to query from
     * @return master channel data
     */
    public static int getMasterData(ItemStack masterStack) {
        if (!hasMasterData(masterStack)) return masterStack.stackSize;
        return masterStack.stackTagCompound.getInteger(MASTER_OVERRIDE_TAG);
    }

    /**
     * Set the master channel override on given trigger item
     * 
     * @param masterStack trigger stack to set
     * @param data        positive master channel data
     */
    public static void setMasterData(ItemStack masterStack, int data) {
        if (masterStack == null) throw new IllegalArgumentException();
        if (data < 1) throw new IllegalArgumentException("Channel data must be positive");
        if (masterStack.stackTagCompound == null) masterStack.stackTagCompound = new NBTTagCompound();
        masterStack.stackTagCompound.setInteger(MASTER_OVERRIDE_TAG, data);
    }

    /**
     * Clear the master channel override from given trigger item, if it exists
     * 
     * @param masterStack trigger stack to unset
     */
    public static void unsetMasterData(ItemStack masterStack) {
        if (masterStack == null) throw new IllegalArgumentException();
        if (masterStack.stackTagCompound == null) return;
        masterStack.stackTagCompound.removeTag(MASTER_OVERRIDE_TAG);
        if (masterStack.stackTagCompound.hasNoTags()) masterStack.stackTagCompound = null;
    }

    /**
     * Return a trigger item that
     * <ul>
     * <li>Still carry all the subchannel in some unspecified ways</li>
     * <li>Have its master channel data replaced with specified subchannel's data</li>
     * </ul>
     *
     * If given subchannel does not exist, masterStack may or may not be returned to save performance. Code within
     * StructureLib can depend on this behavior, you cannot.
     *
     * @param masterStack trigger item. can be the return value of another
     *                    {@link com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor#withChannel(net.minecraft.item.ItemStack, String)}
     *                    call
     * @param channel     subchannel to use. will use current master channel data instead if this channel does not exist
     *                    Note: all channel identifiers are supposed to be lower case and not empty.
     * @return a trigger item with new master channel
     */
    public static ItemStack withChannel(ItemStack masterStack, String channel) {
        if (StringUtils.isEmpty(channel) || masterStack == null) throw new IllegalArgumentException();
        if (StructureLibAPI.isDebugEnabled() && !channel.toLowerCase(Locale.ROOT).equals(channel))
            throw new IllegalArgumentException("Channel name can be lower case ONLY");
        if (!masterStack.hasTagCompound() || !masterStack.stackTagCompound.hasKey(SECONDARY_HINT_TAG, TAG_COMPOUND)
                || !masterStack.stackTagCompound.getCompoundTag(SECONDARY_HINT_TAG).hasKey(channel, NBT.TAG_INT))
            return masterStack;
        ItemStack ret = new ItemStack(
                masterStack.getItem(),
                masterStack.stackTagCompound.getCompoundTag(SECONDARY_HINT_TAG).getInteger(channel),
                Items.feather.getDamage(masterStack));
        ret.setTagCompound(masterStack.stackTagCompound);
        return ret;
    }

    /**
     * Check if given trigger item contains any subchannel
     * 
     * @param masterStack trigger stack to check
     * @return true if contains any subchannel
     */
    public static boolean hasSubChannel(ItemStack masterStack) {
        if (masterStack == null) throw new IllegalArgumentException();
        return masterStack.hasTagCompound() && masterStack.stackTagCompound.hasKey(SECONDARY_HINT_TAG, TAG_COMPOUND);
    }

    /**
     * Check if given trigger item contains specified subchannel
     * 
     * @param masterStack trigger stack to check
     * @param channel     channel identifier. Note: all channel identifiers are supposed to be lower case and not empty.
     * @return true if contains specified subchannel
     */
    public static boolean hasSubChannel(ItemStack masterStack, String channel) {
        if (StringUtils.isEmpty(channel) || masterStack == null) throw new IllegalArgumentException();
        if (StructureLibAPI.isDebugEnabled() && !channel.toLowerCase(Locale.ROOT).equals(channel))
            throw new IllegalArgumentException("Channel name can be lower case ONLY");
        return !channel.isEmpty() && masterStack.hasTagCompound()
                && masterStack.stackTagCompound.hasKey(SECONDARY_HINT_TAG, TAG_COMPOUND)
                && masterStack.stackTagCompound.getCompoundTag(SECONDARY_HINT_TAG).hasKey(channel, NBT.TAG_INT);
    }

    /**
     * Get the subchannel data from given trigger item. Will use master channel instead if not present.
     * 
     * @param masterStack trigger stack to query from
     * @param channel     channel identifier. Note: all channel identifiers are supposed to be lower case and not empty.
     * @return channel data
     */
    public static int getChannelData(ItemStack masterStack, String channel) {
        if (StringUtils.isEmpty(channel) || masterStack == null) throw new IllegalArgumentException();
        if (StructureLibAPI.isDebugEnabled() && !channel.toLowerCase(Locale.ROOT).equals(channel))
            throw new IllegalArgumentException("Channel name can be lower case ONLY");
        if (!masterStack.hasTagCompound() || !masterStack.stackTagCompound.hasKey(SECONDARY_HINT_TAG, TAG_COMPOUND)
                || !masterStack.stackTagCompound.getCompoundTag(SECONDARY_HINT_TAG).hasKey(channel, NBT.TAG_INT))
            return masterStack.stackSize;
        return masterStack.stackTagCompound.getCompoundTag(SECONDARY_HINT_TAG).getInteger(channel);
    }

    /**
     * Set the subchannel data on given trigger item
     * 
     * @param masterStack trigger stack to check
     * @param channel     channel identifier. Note: all channel identifiers are supposed to be lower case and not empty.
     * @param data        positive subchannel data
     */
    public static void setChannelData(ItemStack masterStack, String channel, int data) {
        if (StringUtils.isEmpty(channel) || masterStack == null) throw new IllegalArgumentException();
        if (data < 1) throw new IllegalArgumentException("Channel data must be positive");
        if (StructureLibAPI.isDebugEnabled() && !channel.toLowerCase(Locale.ROOT).equals(channel))
            throw new IllegalArgumentException("Channel name can be lower case ONLY");
        if (masterStack.stackTagCompound == null) masterStack.stackTagCompound = new NBTTagCompound();
        NBTTagCompound main = masterStack.stackTagCompound;
        if (!main.hasKey(SECONDARY_HINT_TAG, TAG_COMPOUND)) main.setTag(SECONDARY_HINT_TAG, new NBTTagCompound());
        main.getCompoundTag(SECONDARY_HINT_TAG).setInteger(channel, data);
    }

    /**
     * Clear the given subchannel from given trigger item, if it exists
     * 
     * @param masterStack trigger stack to unset
     * @param channel     channel identifier. Note: all channel identifiers are supposed to be lower case and not empty.
     */
    public static void unsetChannelData(ItemStack masterStack, String channel) {
        if (StringUtils.isEmpty(channel) || masterStack == null) throw new IllegalArgumentException();
        if (StructureLibAPI.isDebugEnabled() && !channel.toLowerCase(Locale.ROOT).equals(channel))
            throw new IllegalArgumentException("Channel name can be lower case ONLY");
        if (masterStack.stackTagCompound == null) masterStack.stackTagCompound = new NBTTagCompound();
        NBTTagCompound main = masterStack.stackTagCompound;
        if (!main.hasKey(SECONDARY_HINT_TAG, TAG_COMPOUND)) main.setTag(SECONDARY_HINT_TAG, new NBTTagCompound());
        NBTTagCompound tag = main.getCompoundTag(SECONDARY_HINT_TAG);
        tag.removeTag(channel);
        if (tag.hasNoTags()) main.removeTag(SECONDARY_HINT_TAG);
        if (main.hasNoTags()) masterStack.stackTagCompound = null;
    }

    /**
     * Wipe all subchannel data and master channel override on given trigger item
     *
     * @param masterStack trigger stack to wipe
     */
    public static void wipeChannelData(ItemStack masterStack) {
        if (masterStack == null) throw new IllegalArgumentException();
        if (masterStack.stackTagCompound != null) {
            masterStack.stackTagCompound.removeTag(SECONDARY_HINT_TAG);
            masterStack.stackTagCompound.removeTag(MASTER_OVERRIDE_TAG);
            if (masterStack.stackTagCompound.hasNoTags()) {
                masterStack.stackTagCompound = null;
            }
        }
    }

    /**
     * Iterate over all subchannel data on trigger stack. Does not include master channel!!
     * 
     * @param masterStack trigger stack to check
     * @return A java8 stream of pairs. Key is channel identifier and value is channel data. Pairs do not support
     *         mutation nor removal. Can still cause {@link java.util.ConcurrentModificationException} if underlying
     *         channel data is modified while stream is working.
     */
    public static Stream<Entry<String, Integer>> iterateChannelData(ItemStack masterStack) {
        if (!hasSubChannel(masterStack)) return Stream.empty();
        NBTTagCompound tag = masterStack.stackTagCompound.getCompoundTag(SECONDARY_HINT_TAG);
        return getTagKeys(tag).stream().map(s -> new ImmutablePair<>(s, tag.getInteger(s)));
    }

    /**
     * Get the number of subchannels present on given trigger item. Does not include master channel.
     * 
     * @param masterStack trigger stack to query from
     * @return subchannel count
     */
    public static int countChannelData(ItemStack masterStack) {
        if (!hasSubChannel(masterStack)) return 0;
        NBTTagCompound tag = masterStack.stackTagCompound.getCompoundTag(SECONDARY_HINT_TAG);
        return tag.func_150296_c().size();
    }
}
