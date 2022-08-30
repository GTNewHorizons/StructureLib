package com.gtnewhorizon.structurelib.structure;

import static com.gtnewhorizon.structurelib.StructureLib.LOGGER;
import static com.gtnewhorizon.structurelib.StructureLib.PANIC_MODE;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import java.util.function.Consumer;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
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
     * <p>
     * You might want to use {@link StructureUtility#survivalPlaceBlock(Block, int, World, int, int, int, IItemSource, EntityPlayerMP)}
     * or its overloads to implement this.
     *
     * @param trigger trigger item
     * @param s       drain resources from this place
     * @param actor   source of action. for very critical errors you can also just send the error messages here, bypassing
     *                any filter that chatter might have.
     * @param chatter send error messages here. Caller will choose an appropriate way to forward it to player if the
     *                other fallbacks also fails.
     */
    default PlaceResult survivalPlaceBlock(
            T t,
            World world,
            int x,
            int y,
            int z,
            ItemStack trigger,
            IItemSource s,
            EntityPlayerMP actor,
            Consumer<IChatComponent> chatter) {
        if (PANIC_MODE) throw new RuntimeException("Panic Tripwire hit");
        if (StructureLibAPI.isDebugEnabled())
            LOGGER.error(
                    "Default implementation of survivalPlaceBlock hit! Things aren't going to work well! IStructureElement class: {}",
                    getClass().getName());
        if (!StructureLibAPI.isBlockTriviallyReplaceable(world, x, y, z, actor)) return PlaceResult.REJECT;
        return PlaceResult.SKIP;
    }

    /**
     * Forget the messed up class dependency graph for now. this is just so convenient.
     */
    default IStructureElementNoPlacement<T> noPlacement() {
        return new IStructureElementNoPlacement<T>() {

            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return IStructureElement.this.check(t, world, x, y, z);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                return IStructureElement.this.spawnHint(t, world, x, y, z, trigger);
            }
        };
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
         * TODO this definition doesn't seem right. Should we separate SKIP from EXISTS, or SKIP from ERROR?
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
         * <p>
         * Element placed successfully. To proceed further would require stopping the placement and wait for next round.
         */
        ACCEPT_STOP;
    }
}
