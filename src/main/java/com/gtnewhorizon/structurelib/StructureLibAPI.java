package com.gtnewhorizon.structurelib;

import static com.gtnewhorizon.structurelib.StructureLib.proxy;

import com.gtnewhorizon.structurelib.alignment.IAlignmentProvider;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.net.AlignmentMessage;
import cpw.mods.fml.common.network.NetworkRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

/**
 * A stable interface into the structure lib's internals. Backwards compatibility is maintained to the maximum extend possible.
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

    /**
     * Start a batch of hinting. All hints particles generated during one batch will be considered to belong to one hologram.
     * <p>
     * You don't need to call this unless your constructable tool didn't call {@link com.gtnewhorizon.structurelib.alignment.constructable.ConstructableUtility#handle(ItemStack, EntityPlayer, World, int, int, int, int)}
     */
    public static void startHinting(World w) {
        proxy.startHinting(w);
    }

    /**
     * Start current batch of hinting. All hints particles generated during one batch will be considered to belong to one hologram.
     * <p>
     * You don't need to call this unless your constructable tool didn't call {@link com.gtnewhorizon.structurelib.alignment.constructable.ConstructableUtility#handle(ItemStack, EntityPlayer, World, int, int, int, int)}
     */
    public static void endHinting(World w) {
        proxy.endHinting(w);
    }

    public static void hintParticleTinted(World w, int x, int y, int z, IIcon[] icons, short[] RGBa) {
        proxy.hintParticleTinted(w, x, y, z, icons, RGBa);
    }

    public static void hintParticleTinted(World w, int x, int y, int z, Block block, int meta, short[] RGBa) {
        proxy.hintParticleTinted(w, x, y, z, block, meta, RGBa);
    }

    public static void hintParticle(World w, int x, int y, int z, IIcon[] icons) {
        proxy.hintParticle(w, x, y, z, icons);
    }

    public static void hintParticle(World w, int x, int y, int z, Block block, int meta) {
        proxy.hintParticle(w, x, y, z, block, meta);
    }

    /**
     * Query the ExtendedFacing of this tile entity.
     * The ExtendedFacing will later be set onto given tile entity via {@link com.gtnewhorizon.structurelib.alignment.IAlignment#setExtendedFacing(ExtendedFacing)}
     * upon arrival of server reply.
     *
     * @throws IllegalArgumentException if is not tile entity or provided a null alignment
     */
    public static void queryAlignment(IAlignmentProvider provider) {
        StructureLib.net.sendToServer(new AlignmentMessage.AlignmentQuery(provider));
    }

    /**
     * Send the ExtendedFacing of this Tile Entity to all players. Can be called on server side only.
     *
     * @throws IllegalArgumentException if is not tile entity or provided a null alignment
     */
    public static void sendAlignment(IAlignmentProvider provider) {
        StructureLib.net.sendToAll(new AlignmentMessage.AlignmentData(provider));
    }

    /**
     * Send the ExtendedFacing of this Tile Entity to given player. Can be called on server side only.
     *
     * @throws IllegalArgumentException if is not tile entity or provided a null alignment
     */
    public static void sendAlignment(IAlignmentProvider provider, EntityPlayerMP player) {
        StructureLib.net.sendTo(new AlignmentMessage.AlignmentData(provider), player);
    }

    /**
     * Send the ExtendedFacing of this Tile Entity to all players around target point. Can be called on server side only.
     *
     * @throws IllegalArgumentException if is not tile entity or provided a null alignment
     */
    public static void sendAlignment(IAlignmentProvider provider, NetworkRegistry.TargetPoint targetPoint) {
        StructureLib.net.sendToAllAround(new AlignmentMessage.AlignmentData(provider), targetPoint);
    }

    /**
     * Send the ExtendedFacing of this Tile Entity to all players in that dimension. Can be called on server side only.
     *
     * @throws IllegalArgumentException if is not tile entity or provided a null alignment
     */
    public static void sendAlignment(IAlignmentProvider provider, World dimension) {
        StructureLib.net.sendToDimension(new AlignmentMessage.AlignmentData(provider), dimension.provider.dimensionId);
    }

    public static Block getBlockHint() {
        return StructureLib.blockHint;
    }

    public static Item getItemBlockHint() {
        return StructureLib.itemBlockHint;
    }

    public static boolean isDebugEnabled() {
        return StructureLib.DEBUG_MODE;
    }

    public static void setDebugEnabled(boolean enabled) {
        StructureLib.DEBUG_MODE = enabled;
    }
}
