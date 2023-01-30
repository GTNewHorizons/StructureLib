package com.gtnewhorizon.structurelib.alignment.constructable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;

/**
 * An extension to {@link IConstructable}. Tile Entities (or delegates returned by
 * {@link IConstructableProvider#getConstructable()} should implement this interface to allow third party tools and
 * StructureLib builtin constructable tools to trigger autoplace in survival mode.
 * <p>
 * While not originally planned, a creative player can also use this interface to trigger autoplace by using an
 * {@link IItemSource} with every possible items in the game.
 */
public interface ISurvivalConstructable extends IConstructable {

    /**
     * End player might want to disable this hack if it turns out to be too crashy
     */
    boolean DISABLE_HACKY_MIGRATION_CODE = Boolean.getBoolean("structurelib.disable_isc_migration_hack");

    /**
     * Construct the structure using
     * {@link com.gtnewhorizon.structurelib.structure.IStructureElement#survivalPlaceBlock(Object, World, int, int, int, ItemStack, IItemSource, EntityPlayerMP, java.util.function.Consumer)}
     * or
     * {@link com.gtnewhorizon.structurelib.structure.IStructureDefinition#survivalBuild(Object, ItemStack, String, World, ExtendedFacing, int, int, int, int, int, int, int, IItemSource, EntityPlayerMP, boolean)}
     *
     * @param elementBudget The server configured element budget. The implementor can choose to tune this up a bit if
     *                      the structure is too big, but generally should not be a 4 digits number to not overwhelm the
     *                      server
     * @return -1 if done, otherwise number of elements placed this round
     */
    @Deprecated
    default int survivalConstruct(ItemStack stackSize, int elementBudget, IItemSource source, EntityPlayerMP actor) {
        return survivalConstruct(stackSize, elementBudget, ISurvivalBuildEnvironment.create(source, actor));
    }

    /**
     * Construct the structure using
     * {@link com.gtnewhorizon.structurelib.structure.IStructureElement#survivalPlaceBlock(Object, World, int, int, int, ItemStack, AutoPlaceEnvironment)}
     * or
     * {@link com.gtnewhorizon.structurelib.structure.IStructureDefinition#survivalBuild(Object, ItemStack, String, World, ExtendedFacing, int, int, int, int, int, int, int, ISurvivalBuildEnvironment, boolean)}
     *
     * @param elementBudget The server configured element budget. The implementor can choose to tune this up a bit if
     *                      the structure is too big, but generally should not be a 4 digits number to not overwhelm the
     *                      server
     * @return -1 if done, -2 if not supported (e.g. due to an incompatible change in the API of env, otherwise number
     *         of elements placed this round
     */
    default int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        EntityPlayer actor = env.getActor();
        if (actor instanceof EntityPlayerMP)
            return survivalConstruct(stackSize, elementBudget, env.getSource(), (EntityPlayerMP) actor);
        else if (!DISABLE_HACKY_MIGRATION_CODE && __get_player() == null) {
            // as far as I know, no implementor would actually do anything to actor beyond passing it down to
            // IStructureDefinition, so it's probably fine to proceed like this
            GlobalStates.entityPlayer.set(env.getActor());
            try {
                return survivalConstruct(stackSize, elementBudget, env.getSource(), null);
            } finally {
                GlobalStates.entityPlayer.remove();
            }
        }
        return -2;
    }

    /**
     * Internal hack. Do not use.
     */
    @Deprecated
    static EntityPlayer __get_player() {
        return GlobalStates.entityPlayer.get();
    }
}

class GlobalStates {

    public static final ThreadLocal<EntityPlayer> entityPlayer = new ThreadLocal<>();
}
