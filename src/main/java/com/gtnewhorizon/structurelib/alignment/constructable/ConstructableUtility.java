package com.gtnewhorizon.structurelib.alignment.constructable;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

public class ConstructableUtility {
    private ConstructableUtility() {

    }

    public static boolean handle(ItemStack aStack, EntityPlayer aPlayer, World aWorld, int aX, int aY, int aZ, int aSide) {
        StructureLibAPI.startHinting(aWorld);
        boolean ret = handle0(aStack, aPlayer, aWorld, aX, aY, aZ, aSide);
        StructureLibAPI.endHinting(aWorld);
        return ret;
    }

    private static boolean handle0(ItemStack aStack, EntityPlayer aPlayer, World aWorld, int aX, int aY, int aZ, int aSide) {
        TileEntity tTileEntity = aWorld.getTileEntity(aX, aY, aZ);
        if (tTileEntity == null || aPlayer instanceof FakePlayer) {
            return aPlayer instanceof EntityPlayerMP;
        }
        if (aPlayer instanceof EntityPlayerMP) {
            //struct gen
            if (aPlayer.isSneaking() && aPlayer.capabilities.isCreativeMode) {
                if (tTileEntity instanceof IConstructableProvider) {
                    IConstructable constructable = ((IConstructableProvider) tTileEntity).getConstructable();
                    if (constructable != null) {
                        constructable.construct(aStack, false);
                    }
                } else if (tTileEntity instanceof IConstructable) {
                    ((IConstructable) tTileEntity).construct(aStack, false);
                } else if (IMultiblockInfoContainer.contains(tTileEntity.getClass())) {
                    IMultiblockInfoContainer<TileEntity> iMultiblockInfoContainer = IMultiblockInfoContainer.get(tTileEntity.getClass());
                    if (tTileEntity instanceof IAlignment) {
                        iMultiblockInfoContainer.construct(aStack, false, tTileEntity,
                            ((IAlignment) tTileEntity).getExtendedFacing());
                    } else {
                        iMultiblockInfoContainer.construct(aStack, false, tTileEntity,
                            ExtendedFacing.of(ForgeDirection.getOrientation(aSide)));
                    }
                }
            }
            return true;
        } else if (StructureLib.isCurrentPlayer(aPlayer)) {//particles and text client side
            //if ((!aPlayer.isSneaking() || !aPlayer.capabilities.isCreativeMode)) {
            if (tTileEntity instanceof IConstructableProvider) {
                IConstructable constructable = ((IConstructableProvider) tTileEntity).getConstructable();
                if (constructable != null) {
                    constructable.construct(aStack, true);
                    StructureLib.addClientSideChatMessages(constructable.getStructureDescription(aStack));
                }
            } else if (tTileEntity instanceof IConstructable) {
                IConstructable constructable = (IConstructable) tTileEntity;
                constructable.construct(aStack, true);
                StructureLib.addClientSideChatMessages(constructable.getStructureDescription(aStack));
                return false;
            } else if (IMultiblockInfoContainer.contains(tTileEntity.getClass())) {
                IMultiblockInfoContainer<TileEntity> iMultiblockInfoContainer = IMultiblockInfoContainer.get(tTileEntity.getClass());
                if (tTileEntity instanceof IAlignment) {
                    iMultiblockInfoContainer.construct(aStack, true, tTileEntity,
                        ((IAlignment) tTileEntity).getExtendedFacing());
                } else {
                    iMultiblockInfoContainer.construct(aStack, true, tTileEntity,
                        ExtendedFacing.of(ForgeDirection.getOrientation(aSide)));
                }
                StructureLib.addClientSideChatMessages(IMultiblockInfoContainer.get(tTileEntity.getClass()).getDescription(aStack));
                return false;
            }
        }
        return false;
    }
}
