package com.gtnewhorizon.structurelib;

import static com.gtnewhorizon.structurelib.StructureLib.proxy;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.util.AnimatedTooltipHandler;
import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.IAlignmentProvider;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.net.AlignmentMessage;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;

import cpw.mods.fml.common.network.NetworkRegistry;

/**
 * A stable interface into the structure lib's internals. Backwards compatibility is maintained to the maximum extend
 * possible.
 */
public class StructureLibAPI {

    public static final String MOD_ID = "structurelib";
    public static final int HINT_BLOCK_META_GENERIC_0 = 0;
    public static final int HINT_BLOCK_META_GENERIC_1 = 1;
    public static final int HINT_BLOCK_META_GENERIC_2 = 2;
    public static final int HINT_BLOCK_META_GENERIC_3 = 3;
    public static final int HINT_BLOCK_META_GENERIC_4 = 4;
    public static final int HINT_BLOCK_META_GENERIC_5 = 5;
    public static final int HINT_BLOCK_META_GENERIC_6 = 6;
    public static final int HINT_BLOCK_META_GENERIC_7 = 7;
    public static final int HINT_BLOCK_META_GENERIC_8 = 8;
    public static final int HINT_BLOCK_META_GENERIC_9 = 9;
    public static final int HINT_BLOCK_META_GENERIC_10 = 10;
    public static final int HINT_BLOCK_META_GENERIC_11 = 11;
    public static final int HINT_BLOCK_META_DEFAULT = 12;
    public static final int HINT_BLOCK_META_AIR = 13;
    public static final int HINT_BLOCK_META_NOT_AIR = 14;
    public static final int HINT_BLOCK_META_ERROR = 15;
    public static final String CHANNEL_SHOW_ERROR = "show_error";
    static final ThreadLocal<Object> instrument = new ThreadLocal<>();

    /**
     * Start instrumenting <b>for the current thread only</b>. When instrumentation is enabled,
     * {@link IStructureDefinition}'s builtin iteration methods will send {@link StructureEvent} and its subclasses when
     * appropriate. Otherwise, these events will not be sent at all, regardless of whether a listener for these events
     * are registered.
     * <p>
     * Identifiers are required to be value-comparable, i.e. overrides {@link Object#equals(Object)}. Suggested
     * identifier includes
     * <ul>
     * <li>Mod Instance or {@link cpw.mods.fml.common.ModContainer}</li>
     * <li>{@link com.gtnewhorizon.structurelib.alignment.constructable.IConstructable} and its friends, if you are
     * triggering structure check via this interface.</li>
     * <li>{@link net.minecraft.util.ResourceLocation}</li>
     * </ul>
     * <p>
     * It's an API abuse for normal {@link IStructureDefinition} users to depend on this feature. They have full control
     * and full knowledge of the {@link IStructureDefinition} they are using. Using events to instrument the structure
     * check/autoplace is completely unnecessary and does nothing but add overhead.
     * <p>
     * Instruments are required to call {@link #disableInstrument()} whenever it is done with its business, as there can
     * be only one instrument for any given thread.
     *
     * @param identifier an identifier sent along the actual event.
     * @throws IllegalStateException if instrument has already been enabled.
     */
    public static void enableInstrument(Object identifier) {
        if (isInstrumentEnabled()) throw new IllegalStateException();
        instrument.set(identifier);
    }

    /**
     * Stop instrumenting.
     *
     * @throws IllegalStateException if instrumenting hasn't been enabled.
     */
    public static void disableInstrument() {
        if (!isInstrumentEnabled()) throw new IllegalStateException();
        instrument.set(null);
    }

    /**
     * Check if instrumenting is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public static boolean isInstrumentEnabled() {
        return instrument.get() != null;
    }

    /**
     * Start a batch of hinting. All hints particles generated during one batch will be considered to belong to one
     * hologram.
     * <p>
     * You don't need to call this unless your constructable tool didn't call
     * {@link com.gtnewhorizon.structurelib.alignment.constructable.ConstructableUtility#handle(ItemStack, EntityPlayer, World, int, int, int, int)}
     */
    public static void startHinting(World w) {
        proxy.startHinting(w);
    }

    /**
     * End current batch of hinting. All hints particles generated during one batch will be considered to belong to one
     * hologram.
     * <p>
     * You don't need to call this unless your constructable tool didn't call
     * {@link com.gtnewhorizon.structurelib.alignment.constructable.ConstructableUtility#handle(ItemStack, EntityPlayer, World, int, int, int, int)}
     */
    public static void endHinting(World w) {
        proxy.endHinting(w);
    }

    /**
     * Generate a new hint particle on client side at given location using given textures with a tint.
     *
     * @param w     World to spawn. Usually the client world.
     * @param x     x coord
     * @param y     y coord
     * @param z     z coord
     * @param icons 6 texture. in forge direction order.
     * @param RGBa  a 4 short array. tint in rgba form. currently alpha channel is ignored, but we might change this
     *              later on.
     */
    public static void hintParticleTinted(World w, int x, int y, int z, IIcon[] icons, short[] RGBa) {
        proxy.hintParticleTinted(w, x, y, z, icons, RGBa);
    }

    /**
     * Generate a new hint particle on client side at given location using textures from given block with a tint.
     *
     * @param w     World to spawn. Usually the client world.
     * @param x     x coord
     * @param y     y coord
     * @param z     z coord
     * @param block block to take texture from
     * @param meta  the meta of block to take texture from
     * @param RGBa  a 4 short array. tint in rgba form. currently alpha channel is ignored, but we might change this
     *              later on.
     */
    public static void hintParticleTinted(World w, int x, int y, int z, Block block, int meta, short[] RGBa) {
        proxy.hintParticleTinted(w, x, y, z, block, meta, RGBa);
    }

    /**
     * Generate a new hint particle on client side at given location using given textures.
     *
     * @param w     World to spawn. Usually the client world.
     * @param x     x coord
     * @param y     y coord
     * @param z     z coord
     * @param icons 6 texture. in forge direction order.
     */
    public static void hintParticle(World w, int x, int y, int z, IIcon[] icons) {
        proxy.hintParticle(w, x, y, z, icons);
    }

    /**
     * Generate a new hint particle on client side at given location using textures from given block.
     *
     * @param w     World to spawn. Usually the client world.
     * @param x     x coord
     * @param y     y coord
     * @param z     z coord
     * @param block block to take texture from
     * @param meta  the meta of block to take texture from
     */
    public static void hintParticle(World w, int x, int y, int z, Block block, int meta) {
        proxy.hintParticle(w, x, y, z, block, meta);
    }

    /**
     * Update the tint of given hint particle. Do nothing if particle not found.
     * <p>
     * Can be called on either client side or server side. Server side will schedule a network message to update it for
     * this player only. Will do nothing if no hint particle is found at given location.
     *
     * @return false if nothing updated. true if updated or update instruction sent.
     */
    public static boolean markHintParticleError(EntityPlayer player, World w, int x, int y, int z) {
        return proxy.markHintParticleError(player, w, x, y, z);
    }

    /**
     * Update the tint of given hint particle. Do nothing if particle not found.
     *
     * @return false if nothing updated. true if updated.
     */
    public static boolean updateHintParticleTint(EntityPlayer player, World w, int x, int y, int z, short[] RGBa) {
        return proxy.updateHintParticleTint(player, w, x, y, z, RGBa);
    }

    /**
     * Query the ExtendedFacing of this tile entity from client side. Can be sent only on client side. The
     * ExtendedFacing will later be set onto given tile entity via
     * {@link com.gtnewhorizon.structurelib.alignment.IAlignment#setExtendedFacing(ExtendedFacing)} upon arrival of
     * server reply.
     * <p>
     * The server side will query the {@link ExtendedFacing} of this provider using
     * {@link IAlignment#getExtendedFacing()}
     *
     * @throws IllegalArgumentException if is not tile entity or provided a null alignment
     */
    public static void queryAlignment(IAlignmentProvider provider) {
        StructureLib.net.sendToServer(new AlignmentMessage.AlignmentQuery(provider));
    }

    /**
     * Send the ExtendedFacing of this Tile Entity to all players. Can be called on server side only.
     * <p>
     * The receiving tile entity will receive the {@link ExtendedFacing} via its
     * {@link com.gtnewhorizon.structurelib.alignment.IAlignment#setExtendedFacing(ExtendedFacing)} method.
     *
     * @throws IllegalArgumentException if is not tile entity or provided a null alignment
     */
    public static void sendAlignment(IAlignmentProvider provider) {
        StructureLib.net.sendToAll(new AlignmentMessage.AlignmentData(provider));
    }

    /**
     * Send the ExtendedFacing of this Tile Entity to given player. Can be called on server side only.
     * <p>
     * The receiving tile entity will receive the {@link ExtendedFacing} via its
     * {@link com.gtnewhorizon.structurelib.alignment.IAlignment#setExtendedFacing(ExtendedFacing)} method.
     *
     * @throws IllegalArgumentException if is not tile entity or provided a null alignment
     */
    public static void sendAlignment(IAlignmentProvider provider, EntityPlayerMP player) {
        StructureLib.net.sendTo(new AlignmentMessage.AlignmentData(provider), player);
    }

    /**
     * Send the ExtendedFacing of this Tile Entity to all players around target point. Can be called on server side
     * only.
     * <p>
     * The receiving tile entity will receive the {@link ExtendedFacing} via its
     * {@link com.gtnewhorizon.structurelib.alignment.IAlignment#setExtendedFacing(ExtendedFacing)} method.
     *
     * @throws IllegalArgumentException if is not tile entity or provided a null alignment
     */
    public static void sendAlignment(IAlignmentProvider provider, NetworkRegistry.TargetPoint targetPoint) {
        StructureLib.net.sendToAllAround(new AlignmentMessage.AlignmentData(provider), targetPoint);
    }

    /**
     * Send the ExtendedFacing of this Tile Entity to all players in that dimension. Can be called on server side only.
     * <p>
     * The receiving tile entity will receive the {@link ExtendedFacing} via its
     * {@link com.gtnewhorizon.structurelib.alignment.IAlignment#setExtendedFacing(ExtendedFacing)} method.
     *
     * @throws IllegalArgumentException if is not tile entity or provided a null alignment
     */
    public static void sendAlignment(IAlignmentProvider provider, World dimension) {
        StructureLib.net.sendToDimension(new AlignmentMessage.AlignmentData(provider), dimension.provider.dimensionId);
    }

    /**
     * Get the Block for StructureLib supplied hint block.
     */
    public static Block getBlockHint() {
        return StructureLib.blockHint;
    }

    /**
     * Get the ItemBlock for StructureLib supplied hint block.
     */
    public static Item getItemBlockHint() {
        return StructureLib.itemBlockHint;
    }

    /**
     * Get the hologram item.
     */
    public static Item getDefaultHologramItem() {
        return StructureLib.itemConstructableTrigger;
    }

    /**
     * Check if structure debug mode is on.
     *
     * @return true if debug mode is on
     */
    public static boolean isDebugEnabled() {
        return StructureLib.DEBUG_MODE;
    }

    public static void setDebugEnabled(boolean enabled) {
        StructureLib.DEBUG_MODE = enabled;
    }

    /**
     * Determines if given block can be replaced without much effort. The exact predicate clauses is not stable and will
     * be changed, but the general idea will always stay the same.
     * <p>
     * Use this in your
     * {@link com.gtnewhorizon.structurelib.structure.IStructureElement#survivalPlaceBlock(Object, World, int, int, int, ItemStack, AutoPlaceEnvironment)}
     */
    @Deprecated
    public static boolean isBlockTriviallyReplaceable(World w, int x, int y, int z, EntityPlayerMP actor) {
        return isBlockTriviallyReplaceable(w, x, y, z, (EntityPlayer) actor);
    }

    /**
     * Determines if given block can be replaced without much effort. The exact predicate clauses is not stable and will
     * be changed, but the general idea will always stay the same.
     * <p>
     * Use this in your
     * {@link com.gtnewhorizon.structurelib.structure.IStructureElement#survivalPlaceBlock(Object, World, int, int, int, ItemStack, AutoPlaceEnvironment)}
     */
    public static boolean isBlockTriviallyReplaceable(World w, int x, int y, int z, EntityPlayer actor) {
        // TODO extend this function a bit
        Block block = w.getBlock(x, y, z);
        return block.isAir(w, x, y, z) || block.isReplaceable(w, x, y, z);
    }

    /**
     * Send chat to player, but throttled.
     *
     * @param throttleKey      throttle key. Must properly implement {@link Object#hashCode()} and
     *                         {@link Object#equals(Object)} to handle value equality.
     * @param player           player to send chat to
     * @param text             chat to send
     * @param intervalRequired interval required before last recorded time to actually send the message. unit in
     *                         millisecond. we purposefully chose to not use a bigger data type to limit how long this
     *                         interval can be
     */
    public static void addThrottledChat(Object throttleKey, EntityPlayer player, IChatComponent text,
            short intervalRequired) {
        addThrottledChat(throttleKey, player, text, intervalRequired, false);
    }

    /**
     * Send chat to player, but throttled.
     *
     * @param throttleKey         throttle key. Must properly implement {@link Object#hashCode()} and
     *                            {@link Object#equals(Object)} to handle value equality.
     * @param player              player to send chat to
     * @param text                chat to send
     * @param intervalRequired    interval required before last recorded time to actually send the message. unit in
     *                            millisecond. we purposefully chose to not use a bigger data type to limit how long
     *                            this interval can be
     * @param forceUpdateLastSend if true, always update the last send time, even if the message isn't actually sent.
     */
    public static void addThrottledChat(Object throttleKey, EntityPlayer player, IChatComponent text,
            short intervalRequired, boolean forceUpdateLastSend) {
        proxy.addThrottledChat(throttleKey, player, text, intervalRequired, forceUpdateLastSend);
    }

    /**
     * Register a channel description here. The description should describe how a channel will be used inside your mod.
     *
     * @param channel     channel key
     * @param modid       your modid
     * @param description localization key for your description
     */
    public static void registerChannelDescription(final String channel, final String modid, final String description) {
        ChannelDescription.set(channel, modid, description);
    }

    /**
     * Register an item stack as an indicator for a (channel, value) pair.
     *
     * @param channel      channel key
     * @param modid        your modid
     * @param channelValue value
     * @param stack        item stack. having a metadata of
     *                     {@link net.minecraftforge.oredict.OreDictionary#WILDCARD_VALUE} will match without comparing
     *                     NBT or metadata value. having a tag with {@code tag.getBoolean("*") == true} will match
     *                     without comparing NBT.
     */
    public static void registerChannelItem(final String channel, final String modid, final int channelValue,
            final ItemStack stack) {
        ChannelDescription.item(channel, channelValue, stack);
        AnimatedTooltipHandler.addItemTooltip(
                stack,
                AnimatedTooltipHandler.translatedText("structurelib.tooltip.channelvalue", channelValue, channel));
        AnimatedTooltipHandler.addItemTooltip(
                stack,
                AnimatedTooltipHandler.translatedText("structurelib.tooltip.indicator_dnd", channelValue, channel));
    }
}
