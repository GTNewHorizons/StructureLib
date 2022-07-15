package com.gtnewhorizon.structurelib.structure;

import static com.gtnewhorizon.structurelib.StructureLib.DEBUG_MODE;
import static com.gtnewhorizon.structurelib.StructureLib.LOGGER;
import static com.gtnewhorizon.structurelib.StructureLib.PANIC_MODE;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Use StructureUtility to instantiate
 */
public interface IStructureElement<T> {
    boolean check(T t, World world, int x, int y, int z);

    boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger);

    boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger);

    /**
     * Try place the block by taking resource from given IItemSource.
     *
     * @param s     drain resources from this place
     * @param actor source of action.
     */
    default PlaceResult survivalPlaceBlock(
            T t, World world, int x, int y, int z, ItemStack trigger, IItemSource s, EntityPlayerMP actor) {
        if (PANIC_MODE) throw new RuntimeException("Panic Tripwire hit");
        if (DEBUG_MODE)
            LOGGER.error(
                    "Default implementation of survivalPlaceBlock hit! Things aren't going to work well! IStructureElement class: {}",
                    getClass().getName());
        if (!StructureLibAPI.isBlockTriviallyReplaceable(world, x, y, z, actor)) return PlaceResult.REJECT;
        return PlaceResult.SKIP;
    }

    default int getStepA() {
        return 1;
    }

    default int getStepB() {
        return 0;
    }

    default int getStepC() {
        return 0;
    }

    default boolean resetA() {
        return false;
    }

    default boolean resetB() {
        return false;
    }

    default boolean resetC() {
        return false;
    }

    default boolean isNavigating() {
        return false;
    }

    enum PlaceResult {
        /**
         * This element either exists already, or does not yet have an implementation for survivalPlaceBlock, or
         * some other unforeseen situations.
         */
        SKIP,
        /**
         * This element's space is occupied by other stuff and require player attention, or player missing required resource,
         * or the block cannot be placed due to obscure mechanisms, or some other unforeseen situations.
         */
        REJECT,
        /**
         * Autoplace cannot proceed within this tick. To proceed further would require stopping the placement and wait for next round.
         */
        STOP,
        /**
         * Element placed successfully.
         */
        ACCEPT,
        /**
         * Combination of ACCEPT and STOP.
         *
         * Element placed successfully. To proceed further would require stopping the placement and wait for next round.
         */
        ACCEPT_STOP;
    }
}
