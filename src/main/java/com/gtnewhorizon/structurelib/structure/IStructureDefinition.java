package com.gtnewhorizon.structurelib.structure;

import static com.gtnewhorizon.structurelib.StructureLib.DEBUG_MODE;
import static com.gtnewhorizon.structurelib.structure.IStructureWalker.ignoreBlockUnloaded;
import static com.gtnewhorizon.structurelib.structure.IStructureWalker.skipBlockUnloaded;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public interface IStructureDefinition<T> {
    /**
     * Used internally
     * <p>
     * Might, but is not required, to throw {@link java.util.NoSuchElementException} if the given structure piece is not found.
     *
     * @param name same name as for other methods here
     * @return the array of elements to process
     * @throws java.util.NoSuchElementException if the given structure piece is not found and the moon phase perfectly matches
     */
    IStructureElement<T>[] getStructureFor(String name);

    default boolean check(
            T object,
            String piece,
            World world,
            ExtendedFacing extendedFacing,
            int basePositionX,
            int basePositionY,
            int basePositionZ,
            int basePositionA,
            int basePositionB,
            int basePositionC,
            boolean forceCheckAllBlocks) {
        return iterate(
                object,
                null,
                getStructureFor(piece),
                world,
                extendedFacing,
                basePositionX,
                basePositionY,
                basePositionZ,
                basePositionA,
                basePositionB,
                basePositionC,
                false,
                forceCheckAllBlocks);
    }

    default boolean hints(
            T object,
            ItemStack trigger,
            String piece,
            World world,
            ExtendedFacing extendedFacing,
            int basePositionX,
            int basePositionY,
            int basePositionZ,
            int basePositionA,
            int basePositionB,
            int basePositionC) {
        return iterate(
                object,
                trigger,
                getStructureFor(piece),
                world,
                extendedFacing,
                basePositionX,
                basePositionY,
                basePositionZ,
                basePositionA,
                basePositionB,
                basePositionC,
                true,
                null);
    }

    default boolean build(
            T object,
            ItemStack trigger,
            String piece,
            World world,
            ExtendedFacing extendedFacing,
            int basePositionX,
            int basePositionY,
            int basePositionZ,
            int basePositionA,
            int basePositionB,
            int basePositionC) {
        return iterate(
                object,
                trigger,
                getStructureFor(piece),
                world,
                extendedFacing,
                basePositionX,
                basePositionY,
                basePositionZ,
                basePositionA,
                basePositionB,
                basePositionC,
                false,
                null);
    }

    default boolean buildOrHints(
            T object,
            ItemStack trigger,
            String piece,
            World world,
            ExtendedFacing extendedFacing,
            int basePositionX,
            int basePositionY,
            int basePositionZ,
            int basePositionA,
            int basePositionB,
            int basePositionC,
            boolean hintsOnly) {
        return iterate(
                object,
                trigger,
                getStructureFor(piece),
                world,
                extendedFacing,
                basePositionX,
                basePositionY,
                basePositionZ,
                basePositionA,
                basePositionB,
                basePositionC,
                hintsOnly,
                null);
    }

    /**
     * @param elementBudget build up to this many elements
     * @param source        from where to drain resource
     * @param actor         source of action
     * @param check         whether {@link IStructureElement#check(Object, World, int, int, int)} should be called if anything
     *                      would be placed. use with caution.
     *                      if in doubt, call {@link #check(Object, String, World, ExtendedFacing, int, int, int, int, int, int, boolean)}
     *                      after this call and set this parameter to false
     * @return number of elements built, or -1 if structure done
     * @see #build(Object, ItemStack, String, World, ExtendedFacing, int, int, int, int, int, int)
     */
    default int survivalBuild(
            T object,
            ItemStack trigger,
            String piece,
            World world,
            ExtendedFacing extendedFacing,
            int basePositionX,
            int basePositionY,
            int basePositionZ,
            int basePositionA,
            int basePositionB,
            int basePositionC,
            int elementBudget,
            IItemSource source,
            EntityPlayerMP actor,
            boolean check) {
        SurvivalBuildStructureWalker<T> walker =
                new SurvivalBuildStructureWalker<>(object, trigger, source, actor, elementBudget, check);
        StructureUtility.iterateV2(
                getStructureFor(piece),
                world,
                extendedFacing,
                basePositionX,
                basePositionY,
                basePositionZ,
                basePositionA,
                basePositionB,
                basePositionC,
                walker,
                "survivalBuild");
        return walker.getBuilt();
    }

    static <T> boolean iterate(
            T object,
            ItemStack trigger,
            IStructureElement<T>[] elements,
            World world,
            ExtendedFacing extendedFacing,
            int basePositionX,
            int basePositionY,
            int basePositionZ,
            int basePositionA,
            int basePositionB,
            int basePositionC,
            boolean hintsOnly,
            Boolean checkBlocksIfNotNullForceCheckAllIfTrue) {
        if (world.isRemote ^ hintsOnly) {
            return false;
        }

        if (checkBlocksIfNotNullForceCheckAllIfTrue != null) {
            boolean success;
            if (checkBlocksIfNotNullForceCheckAllIfTrue) {
                success = StructureUtility.iterateV2(
                        elements,
                        world,
                        extendedFacing,
                        basePositionX,
                        basePositionY,
                        basePositionZ,
                        basePositionA,
                        basePositionB,
                        basePositionC,
                        (e, w, x, y, z) -> e.check(object, w, x, y, z),
                        "check");
            } else {
                success = StructureUtility.iterateV2(
                        elements,
                        world,
                        extendedFacing,
                        basePositionX,
                        basePositionY,
                        basePositionZ,
                        basePositionA,
                        basePositionB,
                        basePositionC,
                        skipBlockUnloaded((e, w, x, y, z) -> e.check(object, w, x, y, z)),
                        "check force");
            }
            if (DEBUG_MODE && success) {
                StructureLib.LOGGER.info(
                        "Multi [" + basePositionX + ", " + basePositionY + ", " + basePositionZ + "] pass");
            }
            return success;
        } else {
            if (hintsOnly) {
                StructureUtility.iterateV2(
                        elements,
                        world,
                        extendedFacing,
                        basePositionX,
                        basePositionY,
                        basePositionZ,
                        basePositionA,
                        basePositionB,
                        basePositionC,
                        ignoreBlockUnloaded((e, w, x, y, z) -> {
                            e.spawnHint(object, world, x, y, z, trigger);
                            return true;
                        }),
                        "spawnHint");
            } else {
                StructureUtility.iterateV2(
                        elements,
                        world,
                        extendedFacing,
                        basePositionX,
                        basePositionY,
                        basePositionZ,
                        basePositionA,
                        basePositionB,
                        basePositionC,
                        ignoreBlockUnloaded((e, w, x, y, z) -> {
                            e.placeBlock(object, world, x, y, z, trigger);
                            return true;
                        }),
                        "placeBlock");
            }
        }
        return true;
    }

    static <T> StructureDefinition.Builder<T> builder() {
        return StructureDefinition.builder();
    }
}
