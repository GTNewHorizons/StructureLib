package com.gtnewhorizon.structurelib.alignment.constructable;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.UUID;

/**
 * To implement IConstructable on not own TileEntities
 */
public interface IMultiblockInfoContainer<T> {
    HashMap<String, IMultiblockInfoContainer<?>> MULTIBLOCK_MAP = new HashMap<>();

    /**
     * There is no specific loading phase restriction, but you should generally not call it before the tile entity is
     * properly registered.
     */
    static <T extends TileEntity> void registerTileClass(Class<T> clazz, IMultiblockInfoContainer<?> info) {
        MULTIBLOCK_MAP.put(clazz.getCanonicalName(), info);
    }

    @SuppressWarnings("unchecked")
    static <T> IMultiblockInfoContainer<T> get(Class<?> tClass) {
        return (IMultiblockInfoContainer<T>) MULTIBLOCK_MAP.get(tClass.getCanonicalName());
    }

    static boolean contains(Class<?> tClass) {
        return MULTIBLOCK_MAP.containsKey(tClass.getCanonicalName());
    }

    void construct(ItemStack stackSize, boolean hintsOnly, T tileEntity, ExtendedFacing aSide);

    /**
     * Construct the structure using {@link com.gtnewhorizon.structurelib.structure.IStructureElement#survivalPlaceBlock(Object, World, int, int, int, ItemStack, IItemSource, net.minecraft.entity.player.EntityPlayerMP)}
     * @return -1 if done, a helping pointer
     */
    default int survivalConstruct(ItemStack stackSize, int elementBudge, IItemSource source, UUID actorProfile, T tileEntity, ExtendedFacing aSide) {
        return -1;
    }

    @SideOnly(Side.CLIENT)
    String[] getDescription(ItemStack stackSize);

    /**
     * Override this and return {@link #toConstructable(IMultiblockInfoContainer, Object, ExtendedFacing)} if
     * {@link #survivalConstruct(ItemStack, int, IItemSource, UUID, Object, ExtendedFacing)} isn't a stub
     */
    default IConstructable toConstructable(T tileEntity, ExtendedFacing aSide) {
        return new IConstructable() {
            @Override
            public void construct(ItemStack stackSize, boolean hintsOnly) {
                IMultiblockInfoContainer.this.construct(stackSize, hintsOnly, tileEntity, aSide);
            }

            @Override
            public String[] getStructureDescription(ItemStack stackSize) {
                return IMultiblockInfoContainer.this.getDescription(stackSize);
            }
        };
    }


    static <T> ISurvivalConstructable toConstructable(IMultiblockInfoContainer<T> thiz, T tileEntity, ExtendedFacing aSide) {
        return new ISurvivalConstructable() {
            @Override
            public int survivalConstruct(ItemStack stackSize, int elementBudget, IItemSource source, UUID actorProfile) {
                return thiz.survivalConstruct(stackSize, elementBudget, source, actorProfile, tileEntity, aSide);
            }

            @Override
            public void construct(ItemStack stackSize, boolean hintsOnly) {
                thiz.construct(stackSize, hintsOnly, tileEntity, aSide);
            }

            @Override
            public String[] getStructureDescription(ItemStack stackSize) {
                return thiz.getDescription(stackSize);
            }
        };
    }
}
