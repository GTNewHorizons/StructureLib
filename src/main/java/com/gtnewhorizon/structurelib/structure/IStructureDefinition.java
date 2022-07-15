package com.gtnewhorizon.structurelib.structure;

import static com.gtnewhorizon.structurelib.StructureLib.DEBUG_MODE;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import java.util.Arrays;
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

        // change base position to base offset
        basePositionA = -basePositionA;
        basePositionB = -basePositionB;
        basePositionC = -basePositionC;

        int[] abc = new int[] {basePositionA, basePositionB, basePositionC};
        int[] xyz = new int[3];

        if (checkBlocksIfNotNullForceCheckAllIfTrue != null) {
            if (checkBlocksIfNotNullForceCheckAllIfTrue) {
                for (IStructureElement<T> element : elements) {
                    if (element.isNavigating()) {
                        abc[0] = (element.resetA() ? basePositionA : abc[0]) + element.getStepA();
                        abc[1] = (element.resetB() ? basePositionB : abc[1]) + element.getStepB();
                        abc[2] = (element.resetC() ? basePositionC : abc[2]) + element.getStepC();
                    } else {
                        extendedFacing.getWorldOffset(abc, xyz);
                        xyz[0] += basePositionX;
                        xyz[1] += basePositionY;
                        xyz[2] += basePositionZ;

                        StructureLib.LOGGER.info(
                                "Multi [{}, {}, {}] step @ {} {}",
                                basePositionX,
                                basePositionY,
                                basePositionZ,
                                Arrays.toString(xyz),
                                Arrays.toString(abc));

                        if (world.blockExists(xyz[0], xyz[1], xyz[2])) {
                            if (!element.check(object, world, xyz[0], xyz[1], xyz[2])) {
                                if (DEBUG_MODE) {
                                    StructureLib.LOGGER.info("Multi [" + basePositionX + ", " + basePositionY + ", "
                                            + basePositionZ + "] failed @ " + Arrays.toString(xyz) + " "
                                            + Arrays.toString(abc));
                                }
                                return false;
                            }
                        } else {
                            if (DEBUG_MODE) {
                                StructureLib.LOGGER.info("Multi [" + basePositionX + ", " + basePositionY + ", "
                                        + basePositionZ + "] !blockExists @ " + Arrays.toString(xyz) + " "
                                        + Arrays.toString(abc));
                            }
                            return false;
                        }
                        abc[0] += 1;
                    }
                }
            } else {
                for (IStructureElement<T> element : elements) {
                    if (element.isNavigating()) {
                        abc[0] = (element.resetA() ? basePositionA : abc[0]) + element.getStepA();
                        abc[1] = (element.resetB() ? basePositionB : abc[1]) + element.getStepB();
                        abc[2] = (element.resetC() ? basePositionC : abc[2]) + element.getStepC();
                    } else {
                        extendedFacing.getWorldOffset(abc, xyz);
                        xyz[0] += basePositionX;
                        xyz[1] += basePositionY;
                        xyz[2] += basePositionZ;

                        StructureLib.LOGGER.info(
                                "Multi [{}, {}, {}] step @ {} {}",
                                basePositionX,
                                basePositionY,
                                basePositionZ,
                                Arrays.toString(xyz),
                                Arrays.toString(abc));

                        if (world.blockExists(xyz[0], xyz[1], xyz[2])) {
                            if (!element.check(object, world, xyz[0], xyz[1], xyz[2])) {
                                if (DEBUG_MODE) {
                                    StructureLib.LOGGER.info("Multi [" + basePositionX + ", " + basePositionY + ", "
                                            + basePositionZ + "] failed @ " + Arrays.toString(xyz) + " "
                                            + Arrays.toString(abc));
                                }
                                return false;
                            }
                        } else {
                            if (DEBUG_MODE) {
                                StructureLib.LOGGER.info("Multi [" + basePositionX + ", " + basePositionY + ", "
                                        + basePositionZ + "] !blockExists @ " + Arrays.toString(xyz) + " "
                                        + Arrays.toString(abc));
                            }
                        }
                        abc[0] += 1;
                    }
                }
            }
            if (DEBUG_MODE) {
                StructureLib.LOGGER.info(
                        "Multi [" + basePositionX + ", " + basePositionY + ", " + basePositionZ + "] pass");
            }
        } else {
            if (hintsOnly) {
                for (IStructureElement<T> element : elements) {
                    if (element.isNavigating()) {
                        abc[0] = (element.resetA() ? basePositionA : abc[0]) + element.getStepA();
                        abc[1] = (element.resetB() ? basePositionB : abc[1]) + element.getStepB();
                        abc[2] = (element.resetC() ? basePositionC : abc[2]) + element.getStepC();
                    } else {
                        extendedFacing.getWorldOffset(abc, xyz);
                        xyz[0] += basePositionX;
                        xyz[1] += basePositionY;
                        xyz[2] += basePositionZ;

                        element.spawnHint(object, world, xyz[0], xyz[1], xyz[2], trigger);

                        abc[0] += 1;
                    }
                }
            } else {
                for (IStructureElement<T> element : elements) {
                    if (element.isNavigating()) {
                        abc[0] = (element.resetA() ? basePositionA : abc[0]) + element.getStepA();
                        abc[1] = (element.resetB() ? basePositionB : abc[1]) + element.getStepB();
                        abc[2] = (element.resetC() ? basePositionC : abc[2]) + element.getStepC();
                    } else {
                        extendedFacing.getWorldOffset(abc, xyz);
                        xyz[0] += basePositionX;
                        xyz[1] += basePositionY;
                        xyz[2] += basePositionZ;

                        if (world.blockExists(xyz[0], xyz[1], xyz[2])) {
                            element.placeBlock(object, world, xyz[0], xyz[1], xyz[2], trigger);
                        }
                        abc[0] += 1;
                    }
                }
            }
        }
        return true;
    }

    static <T> StructureDefinition.Builder<T> builder() {
        return StructureDefinition.builder();
    }
}
