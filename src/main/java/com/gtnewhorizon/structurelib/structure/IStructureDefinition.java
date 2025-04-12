package com.gtnewhorizon.structurelib.structure;

import static com.gtnewhorizon.structurelib.structure.IStructureWalker.ignoreBlockUnloaded;
import static com.gtnewhorizon.structurelib.structure.IStructureWalker.skipBlockUnloaded;

import java.util.function.Function;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;

/**
 * This is the structure definition of your multi. You will have one of these for each multi.
 *
 * <h2>Cache the instances of this class</h2>
 * <p>
 * You will construct this definition once per multi, either at class load or at first call to your variant of
 * {@code getStructureDefinition()}. You can use a {@link ClassValue ClassValue&lt;IStructureDefinition&lt;?>>} if you
 * have a template for subclass. You can also use {@link StructureUtility#defer(Function) defer()} or its overloads for
 * simpler cases, but it is slightly less performant as it will allocate new objects on each structure check, whereas
 * {@link ClassValue} will allocate once and cache it onwards.
 * <p>
 * <h2>Constructing</h2>
 * <p>
 * {@linkplain IStructureDefinition} is constructed using {@link IStructureDefinition#builder()}
 * <p>
 * Builder has two main methods:
 * <ul>
 * <li>{@link com.gtnewhorizon.structurelib.structure.StructureDefinition.Builder#addShape(String, String[][])
 * addShape()} is a 2d string array describing the layout of structure. You can optionally use
 * `StructureUtility#transpose()` to transpose the array to make the structure look nicer in source code.
 * <li>{@link com.gtnewhorizon.structurelib.structure.StructureDefinition.Builder#addElement(Character, IStructureElement)
 * addElement()}. It takes a char identifier (mapped to a char in any of
 * {@link com.gtnewhorizon.structurelib.structure.StructureDefinition.Builder#addShape(String, String[][]) addShape()}
 * calls) and a {@link IStructureElement}.
 * </ul>
 * <h2>Position A, B and C</h2>
 *
 * Imagine you stand in front of the controller, with controller facing towards you not rotated or flipped.
 * <ul>
 * <li>The "A" position would be the number of blocks on the left side of the controller, not counting controller
 * itself.</li>
 * <li>The "B" position would be the number of blocks on the top side of the controller, not counting controller
 * itself.</li>
 * <li>The "C" position would be the number of blocks between you and controller, not counting controller itself.</li>
 * </ul>
 *
 * @param <T> Type of the context object. Usually this will be the multiblock controller.
 */
public interface IStructureDefinition<T> {

    /**
     * Used internally
     * <p>
     * Might, but is not required, to throw {@link java.util.NoSuchElementException} if the given structure piece is not
     * found.
     *
     * @param name same name as for other methods here
     * @return the array of elements to process
     * @throws java.util.NoSuchElementException if the given structure piece is not found and the moon phase perfectly
     *                                          matches
     */
    IStructureElement<T>[] getStructureFor(String name);

    boolean isContainedInStructure(String name, int offsetA, int offsetB, int offsetC);

    /**
     * Run a structure check.
     *
     * @param object              context object. usually multiblock controller.
     * @param piece               the structure piece's string identifier.
     * @param world               the world object this check takes place in.
     * @param extendedFacing      the current structure's orientation.
     * @param basePositionX       X location of the structure
     * @param basePositionY       Y location of the structure
     * @param basePositionZ       Z location of the structure
     * @param basePositionA       see class javadoc
     * @param basePositionB       see class javadoc
     * @param basePositionC       see class javadoc
     * @param forceCheckAllBlocks check all location even if chunk not currently loaded.
     * @return true if successful, false otherwise
     */
    default boolean check(T object, String piece, World world, ExtendedFacing extendedFacing, int basePositionX,
                          int basePositionY, int basePositionZ, int basePositionA, int basePositionB, int basePositionC,
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

    /**
     * Spawn hint particles. Should not be called on server side.
     *
     * @param object         context object. usually multiblock controller.
     * @param piece          the structure piece's string identifier.
     * @param world          the world object this check takes place in.
     * @param extendedFacing the current structure's orientation.
     * @param basePositionX  X location of the structure
     * @param basePositionY  Y location of the structure
     * @param basePositionZ  Z location of the structure
     * @param basePositionA  see class javadoc
     * @param basePositionB  see class javadoc
     * @param basePositionC  see class javadoc
     * @return true if successful, false otherwise
     */
    default boolean hints(T object, ItemStack trigger, String piece, World world, ExtendedFacing extendedFacing,
                          int basePositionX, int basePositionY, int basePositionZ, int basePositionA, int basePositionB,
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

    /**
     * Build the multi in creative mode on a best-effort basis. Does not guarantee the structure is correct after a
     * successful call.
     *
     * @param object         context object. usually multiblock controller.
     * @param trigger        The trigger item that contains channel data.
     * @param piece          the structure piece's string identifier.
     * @param world          the world object this check takes place in.
     * @param extendedFacing the current structure's orientation.
     * @param basePositionX  X location of the structure
     * @param basePositionY  Y location of the structure
     * @param basePositionZ  Z location of the structure
     * @param basePositionA  see class javadoc
     * @param basePositionB  see class javadoc
     * @param basePositionC  see class javadoc
     * @return true if sucessful, false otherwise
     */
    default boolean build(T object, ItemStack trigger, String piece, World world, ExtendedFacing extendedFacing,
                          int basePositionX, int basePositionY, int basePositionZ, int basePositionA, int basePositionB,
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

    /**
     * Call {@link #build(Object, ItemStack, String, World, ExtendedFacing, int, int, int, int, int, int)} if
     * {@code hintsOnly}, call
     * {@link #hints(Object, ItemStack, String, World, ExtendedFacing, int, int, int, int, int, int)} otherwise.
     *
     * @param object         context object. usually multiblock controller.
     * @param trigger        The trigger item that contains channel data.
     * @param piece          the structure piece's string identifier.
     * @param world          the world object this check takes place in.
     * @param extendedFacing the current structure's orientation.
     * @param basePositionX  X location of the structure
     * @param basePositionY  Y location of the structure
     * @param basePositionZ  Z location of the structure
     * @param basePositionA  see class javadoc
     * @param basePositionB  see class javadoc
     * @param basePositionC  see class javadoc
     * @param hintsOnly      whether to spawn hints or do creative build.
     * @return true if sucessful, false otherwise
     * @see #hints(Object, ItemStack, String, World, ExtendedFacing, int, int, int, int, int, int)
     * @see #build(Object, ItemStack, String, World, ExtendedFacing, int, int, int, int, int, int)
     */
    default boolean buildOrHints(T object, ItemStack trigger, String piece, World world, ExtendedFacing extendedFacing,
                                 int basePositionX, int basePositionY, int basePositionZ, int basePositionA, int basePositionB,
                                 int basePositionC, boolean hintsOnly) {
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
     * Cause a survival build.
     *
     * @param object         context object. usually multiblock controller.
     * @param trigger        The trigger item that contains channel data.
     * @param piece          the structure piece's string identifier.
     * @param world          the world object this check takes place in.
     * @param extendedFacing the current structure's orientation.
     * @param basePositionX  X location of the structure
     * @param basePositionY  Y location of the structure
     * @param basePositionZ  Z location of the structure
     * @param basePositionA  see class javadoc
     * @param basePositionB  see class javadoc
     * @param basePositionC  see class javadoc
     * @param elementBudget  build up to this many elements
     * @param source         from where to drain resource
     * @param actor          source of action
     * @param check          whether {@link IStructureElement#check(Object, World, int, int, int)} should be called if
     *                       anything would be placed. use with caution. if in doubt, call
     *                       {@link #check(Object, String, World, ExtendedFacing, int, int, int, int, int, int, boolean)}
     *                       after this call and set this parameter to false
     * @return number of elements built, or -1 if structure done
     * @see #build(Object, ItemStack, String, World, ExtendedFacing, int, int, int, int, int, int)
     */
    @Deprecated
    default int survivalBuild(T object, ItemStack trigger, String piece, World world, ExtendedFacing extendedFacing,
                              int basePositionX, int basePositionY, int basePositionZ, int basePositionA, int basePositionB,
                              int basePositionC, int elementBudget, IItemSource source, EntityPlayerMP actor, boolean check) {
        EntityPlayer realActor;
        if (actor == null) {
            realActor = ISurvivalConstructable.__get_player();
            if (realActor == null) throw new IllegalArgumentException();
        } else {
            realActor = actor;
        }
        return survivalBuild(
            object,
            trigger,
            piece,
            world,
            extendedFacing,
            basePositionX,
            basePositionY,
            basePositionZ,
            basePositionA,
            basePositionB,
            basePositionC,
            elementBudget,
            ISurvivalBuildEnvironment.create(source, realActor),
            check);
    }

    /**
     * Cause a survival build.
     *
     * @param object         context object. usually multiblock controller.
     * @param trigger        The trigger item that contains channel data.
     * @param piece          the structure piece's string identifier.
     * @param world          the world object this check takes place in.
     * @param extendedFacing the current structure's orientation.
     * @param basePositionX  X location of the structure
     * @param basePositionY  Y location of the structure
     * @param basePositionZ  Z location of the structure
     * @param basePositionA  see class javadoc
     * @param basePositionB  see class javadoc
     * @param basePositionC  see class javadoc
     * @param elementBudget  build up to this many elements
     * @param env            build environment.
     * @param check          whether {@link IStructureElement#check(Object, World, int, int, int)} should be called if
     *                       anything would be placed. use with caution. if in doubt, call
     *                       {@link IStructureDefinition#check(Object, String, World, ExtendedFacing, int, int, int, int, int, int, boolean)}
     *                       after this call and set this parameter to false
     * @return number of elements built, or -1 if structure done
     * @see #build(Object, ItemStack, String, World, ExtendedFacing, int, int, int, int, int, int)
     */
    default int survivalBuild(T object, ItemStack trigger, String piece, World world, ExtendedFacing extendedFacing,
                              int basePositionX, int basePositionY, int basePositionZ, int basePositionA, int basePositionB,
                              int basePositionC, int elementBudget, ISurvivalBuildEnvironment env, boolean check) {
        SurvivalBuildStructureWalker<T> walker = new SurvivalBuildStructureWalker<>(
            object,
            trigger,
            elementBudget,
            env,
            this,
            piece,
            extendedFacing,
            new int[] { basePositionA, basePositionB, basePositionC },
            check);
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

    /**
     * Low level utility.
     *
     * @param object                                  context object. usually multiblock controller.
     * @param trigger                                 The trigger item that contains channel data.
     * @param elements                                the structure piece
     * @param world                                   the world object this check takes place in.
     * @param extendedFacing                          the current structure's orientation.
     * @param basePositionX                           X location of the structure
     * @param basePositionY                           Y location of the structure
     * @param basePositionZ                           Z location of the structure
     * @param basePositionA                           see class javadoc
     * @param basePositionB                           see class javadoc
     * @param basePositionC                           see class javadoc
     * @param hintsOnly                               whether to spawn hints or do creative build. no effect if
     *                                                {@code checkBlocksIfNotNullForceCheckAllIfTrue} is not null.
     * @param checkBlocksIfNotNullForceCheckAllIfTrue creative build or spawn hints if null. Force check not loaded
     *                                                chunk if true. Check only loaded chunk is false.
     * @return true if iteration completed successfully.
     * @param <T> type of context object.
     */
    static <T> boolean iterate(T object, ItemStack trigger, IStructureElement<T>[] elements, World world,
                               ExtendedFacing extendedFacing, int basePositionX, int basePositionY, int basePositionZ, int basePositionA,
                               int basePositionB, int basePositionC, boolean hintsOnly, Boolean checkBlocksIfNotNullForceCheckAllIfTrue) {
        if (!world.isRemote && hintsOnly) {
            return false;
        }

        if (checkBlocksIfNotNullForceCheckAllIfTrue != null) {
            boolean success = StructureUtility.iterateV2(
                elements,
                world,
                extendedFacing,
                basePositionX,
                basePositionY,
                basePositionZ,
                basePositionA,
                basePositionB,
                basePositionC,
                checkBlocksIfNotNullForceCheckAllIfTrue ? (e, w, x, y, z, a, b, c) -> e.check(object, w, x, y, z)
                    : skipBlockUnloaded((e, w, x, y, z, a, b, c) -> e.check(object, w, x, y, z)),
                checkBlocksIfNotNullForceCheckAllIfTrue ? "check" : "check force");
            if (StructureLibAPI.isDebugEnabled() && success) {
                StructureLib.LOGGER
                    .info("Multi [" + basePositionX + ", " + basePositionY + ", " + basePositionZ + "] pass");
            }
            return success;
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
                hintsOnly ? ignoreBlockUnloaded((e, w, x, y, z, a, b, c) -> {
                    e.spawnHint(object, world, x, y, z, trigger);
                            if (ChannelDataAccessor.hasSubChannel(trigger, StructureLibAPI.CHANNEL_SHOW_ERROR)
                        && !e.couldBeValid(object, world, x, y, z, trigger)) {
                        StructureLibAPI.markHintParticleError(StructureLib.getCurrentPlayer(), world, x, y, z);
                    }
                    return true;
                }) : ignoreBlockUnloaded((e, w, x, y, z, a, b, c) -> {
                    e.placeBlock(object, world, x, y, z, trigger);
                    return true;
                }),
                hintsOnly ? "spawnHint" : "placeBlock");
        }
        return true;
    }

    /**
     * Create a new instance of builder.
     *
     * @param <T> type of context object
     */
    static <T> StructureDefinition.Builder<T> builder() {
        return StructureDefinition.builder();
    }
}
