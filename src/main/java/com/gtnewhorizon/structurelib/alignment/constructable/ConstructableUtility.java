package com.gtnewhorizon.structurelib.alignment.constructable;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

public class ConstructableUtility {

    // TODO make these configurable and expose this to external world
    public static final int COOLDOWN = 5;
    private static final int BUDGET = 25;
    public static boolean redBrokenOne = true; // TODO this is horrible

    private ConstructableUtility() {}

    public static boolean handle(
            ItemStack aStack, EntityPlayer aPlayer, World aWorld, int aX, int aY, int aZ, int aSide) {
        StructureLibAPI.startHinting(aWorld);
        boolean ret = handle0(aStack, aPlayer, aWorld, aX, aY, aZ, aSide);
        StructureLibAPI.endHinting(aWorld);
        return ret;
    }

    private static boolean handle0(
            ItemStack aStack, EntityPlayer aPlayer, World aWorld, int aX, int aY, int aZ, int aSide) {
        TileEntity tTileEntity = aWorld.getTileEntity(aX, aY, aZ);
        if (tTileEntity == null || aPlayer instanceof FakePlayer) {
            return aPlayer instanceof EntityPlayerMP;
        }
        if (aPlayer instanceof EntityPlayerMP) {
            // not sneaking - client side will generate hologram. nothing to do on server side
            if (!aPlayer.isSneaking()) return true;

            long timePast = StructureLib.getOverworldTime() - getLastUseTick(aStack);
            if (timePast < COOLDOWN) {
                aPlayer.addChatComponentMessage(new ChatComponentTranslation(
                        "item.structurelib.constructableTrigger.too_fast", COOLDOWN - timePast));
                return true;
            }
        } else if (!StructureLib.isCurrentPlayer(aPlayer)) {
            return false;
        }

        IConstructable constructable = null;
        if (tTileEntity instanceof IConstructableProvider)
            constructable = ((IConstructableProvider) tTileEntity).getConstructable();
        else if (tTileEntity instanceof IConstructable) {
            constructable = (IConstructable) tTileEntity;
        } else if (IMultiblockInfoContainer.contains(tTileEntity.getClass())) {
            ExtendedFacing facing = tTileEntity instanceof IAlignment
                    ? ((IAlignment) tTileEntity).getExtendedFacing()
                    : ExtendedFacing.of(ForgeDirection.getOrientation(aSide));
            constructable = IMultiblockInfoContainer.<TileEntity>get(tTileEntity.getClass())
                    .toConstructable(tTileEntity, facing);
        }

        if (constructable == null) return false;

        if (aPlayer instanceof EntityPlayerMP) {
            // server side and sneaking (already checked above)
            // do construct
            if (aPlayer.capabilities.isCreativeMode) {
                constructable.construct(aStack, false);
            } else if (constructable instanceof ISurvivalConstructable) {
                EntityPlayerMP playerMP = (EntityPlayerMP) aPlayer;
                if (((ISurvivalConstructable) constructable)
                                .survivalConstruct(aStack, BUDGET, IItemSource.fromPlayer(playerMP), playerMP)
                        > 0)
                    // TODO somehow notify extensions that their inventory might have been modified and need to be
                    // synced to client or saved
                    playerMP.inventory.markDirty();
                setLastUseTickToStackTag(aStack);
            }
            return true;
        }
        // client side
        // particles and text
        constructable.construct(aStack, true);
        if (getLastUseTick(aStack) == 0)
            StructureLib.addClientSideChatMessages(constructable.getStructureDescription(aStack));
        return false;
    }

    private static long getLastUseTick(ItemStack aStack) {
        return aStack.hasTagCompound() ? aStack.getTagCompound().getLong("LastUse") : 0;
    }

    private static void setLastUseTickToStackTag(ItemStack aStack) {
        NBTTagCompound tag = aStack.stackTagCompound;
        if (tag == null) tag = aStack.stackTagCompound = new NBTTagCompound();
        // here we force use the overworld tick time, in case the current world is over
        tag.setLong("LastUse", StructureLib.getOverworldTime());
    }
}
