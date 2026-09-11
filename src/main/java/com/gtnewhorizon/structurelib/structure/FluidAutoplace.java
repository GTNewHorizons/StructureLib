package com.gtnewhorizon.structurelib.structure;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;

import com.gtnewhorizon.gtnhlib.chat.customcomponents.ChatComponentFluid;
import com.gtnewhorizon.gtnhlib.chat.customcomponents.ChatComponentFluidName;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.structurelib.ConfigurationHandler;
import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.fluid.FluidAmounts;
import com.gtnewhorizon.structurelib.fluid.FluidBlockRequirement;
import com.gtnewhorizon.structurelib.fluid.FluidFillPolicy;
import com.gtnewhorizon.structurelib.fluid.IFluidSource;
import com.gtnewhorizon.structurelib.structure.IStructureElement.PlaceResult;

/**
 * Autoplace of fluid blocks.
 * <p>
 * Placing a fluid is not the same as placing a block. A block stays where it was put, while a fluid flows out of an
 * unfinished structure, floods the surroundings, and turns a partially filled position into something that no longer
 * counts as a source block. This class handles that:
 * <ul>
 * <li>It only places a fluid once the rest of the structure is in place, so that the fluid has something to hold it.
 * See {@link GateMode} for how strict that gate is.</li>
 * <li>It charges only for the fluid that is actually missing from a position, so topping a position up after a leak
 * costs the difference instead of a whole bucket.</li>
 * <li>It only consumes fluid after the block has been placed successfully, and puts the previous block back if the
 * fluid turns out to be gone by then.</li>
 * </ul>
 * Structure elements reach this class through
 * {@link StructureUtility#survivalPlaceBlock(Block, int, World, int, int, int, IItemSource, EntityPlayer, Consumer)} or
 * {@link StructureUtility#ofFluidBlock(Block, int)}, and custom elements may call
 * {@link #tryPlace(World, int, int, int, AutoPlaceEnvironment, FluidBlockRequirement)} directly.
 */
public class FluidAutoplace {

    /**
     * How careful autoplace is about placing a fluid into a structure that is not finished yet.
     */
    public enum GateMode {
        /**
         * Place the fluid right away. The fluid can flow out of the unfinished structure.
         */
        NONE,
        /**
         * Only place the fluid when none of its six neighbours is a position that the fluid can flow into and that
         * doesn't belong to the structure. The fluid can still flow into the part of the structure that is not built
         * yet, but it cannot leave the structure.
         */
        LENIENT,
        /**
         * Only place the fluid once every non fluid element of the structure is satisfied. This is the default.
         * <p>
         * The fluid is placed as soon as the structure can hold it, which is normally the round that finishes the
         * structure: the fluids that round could not place yet are tried once more when it is done building, so they do
         * not have to wait for another round.
         */
        STRICT
    }

    private FluidAutoplace() {}

    /**
     * Try to place a fluid block, draining the fluid from the autoplace environment's fluid source.
     * <p>
     * Nothing is consumed when this returns anything other than {@link PlaceResult#ACCEPT}, and the block that was
     * there before is put back when the fluid cannot be taken after the block has been placed.
     *
     * @param world       world to place in
     * @param x           x coord
     * @param y           y coord
     * @param z           z coord
     * @param env         autoplace environment, providing the fluid source, the actor and the chatter
     * @param requirement which block the structure wants there, what it costs, and how strict the check is
     * @return {@link PlaceResult#SKIP} when the position is already good enough, {@link PlaceResult#ACCEPT} when the
     *         fluid block has been placed, {@link PlaceResult#REJECT_CONTINUE} when the structure is not ready for its
     *         fluid yet or fluid autoplace is off, and {@link PlaceResult#REJECT} when the fluid is missing
     */
    public static PlaceResult tryPlace(World world, int x, int y, int z, AutoPlaceEnvironment env,
            FluidBlockRequirement requirement) {
        if (world.isRemote || !ConfigurationHandler.INSTANCE.isFluidAutoplaceEnabled()) {
            return PlaceResult.REJECT_CONTINUE;
        }
        if (isSatisfied(world, x, y, z, requirement)) return PlaceResult.SKIP;
        if (!canReplace(world, x, y, z, env.getActor())) return PlaceResult.REJECT;
        if (!isGateOpen(world, x, y, z, env)) {
            env.markFluidPlacementDeferred();
            return PlaceResult.REJECT_CONTINUE;
        }
        IFluidSource source = env.getEffectiveFluidSource();
        if (source == null) {
            reportMissingFluid(env, requirement, requirement.cost().amount);
            return PlaceResult.REJECT;
        }
        Block existingBlock = world.getBlock(x, y, z);
        int existingMeta = world.getBlockMetadata(x, y, z);
        int target = requirement.cost().amount;
        int existing = existingBlock == requirement.block() ? FluidAmounts.getStoredAmount(world, x, y, z, target) : 0;
        int missing = Math.max(0, target - existing);
        if (missing > 0 && !source.takeAll(new FluidStack(requirement.cost(), missing), true)) {
            reportMissingFluid(
                    env,
                    requirement,
                    missing - source.getAvailable(new FluidStack(requirement.cost(), missing)));
            return PlaceResult.REJECT;
        }
        if (!placeFluidBlock(world, x, y, z, requirement)) return PlaceResult.REJECT;
        if (missing > 0 && !source.takeAll(new FluidStack(requirement.cost(), missing), false)) {
            restore(world, x, y, z, existingBlock, existingMeta);
            return PlaceResult.REJECT;
        }
        return PlaceResult.ACCEPT;
    }

    /**
     * Try to place one of several fluid blocks, in order, and stop at the first one that is already there or that could
     * be placed.
     *
     * @return the result of the first attempt that was more than a {@link PlaceResult#REJECT_CONTINUE}, or
     *         {@link PlaceResult#REJECT} when the fluid of an alternative was missing, or
     *         {@link PlaceResult#REJECT_CONTINUE} when every alternative had to wait
     * @see #tryPlace(World, int, int, int, AutoPlaceEnvironment, FluidBlockRequirement)
     */
    public static PlaceResult tryPlace(World world, int x, int y, int z, AutoPlaceEnvironment env,
            Iterable<FluidBlockRequirement> requirements) {
        PlaceResult result = PlaceResult.REJECT_CONTINUE;
        for (FluidBlockRequirement requirement : requirements) {
            PlaceResult attempt = tryPlace(world, x, y, z, env, requirement);
            if (attempt == PlaceResult.REJECT_CONTINUE) continue;
            if (attempt == PlaceResult.REJECT) {
                result = PlaceResult.REJECT;
                continue;
            }
            return attempt;
        }
        return result;
    }

    /**
     * Whether the position already holds as much fluid as the structure asks for.
     * <p>
     * This is what a fluid structure element uses for its check, so that a check and an autoplace always agree.
     *
     * @param world       world to look at
     * @param x           x coord
     * @param y           y coord
     * @param z           z coord
     * @param requirement which block the structure wants there, and how strict the check is
     */
    public static boolean isSatisfied(World world, int x, int y, int z, FluidBlockRequirement requirement) {
        Block worldBlock = world.getBlock(x, y, z);
        if (worldBlock != requirement.block()) return false;
        if (world.getBlockMetadata(x, y, z) == requirement.meta()) return true;
        if (requirement.policy() == FluidFillPolicy.EXACT_STATE) return false;
        int target = requirement.cost().amount;
        return requirement.policy().isSatisfied(FluidAmounts.getStoredAmount(world, x, y, z, target), target);
    }

    /**
     * Put the fluid block into the world without paying for it, which is what a creative mode build does.
     * <p>
     * Unlike {@link #tryPlace(World, int, int, int, AutoPlaceEnvironment, FluidBlockRequirement)} this does not check
     * whether the structure is ready to hold the fluid. A creative mode build places everything that isn't a fluid
     * first for that reason.
     *
     * @return true if the block has been placed
     */
    public static boolean place(World world, int x, int y, int z, FluidBlockRequirement requirement) {
        return placeFluidBlock(world, x, y, z, requirement);
    }

    /**
     * Returns a human-readable description of the structure element requirement.
     */
    public static String describe(FluidBlockRequirement requirement) {
        FluidStack stack = requirement.cost();
        return StatCollector.translateToLocalFormatted(
                "structurelib.autoplace.requirement_fluid",
                stack.getLocalizedName(),
                NumberFormatUtil.formatFluid(stack.amount));
    }

    /**
     * Whether a fluid may be placed at this position right now.
     * <p>
     * With {@link GateMode#STRICT} the answer is computed by a walk over the whole structure and cached for the rest of
     * the round, as that walk is expensive. The cache is dropped as soon as the round has built something, so that the
     * fluids which waited for it are not held back by an answer that predates those placements.
     *
     * @param world world to place in
     * @param x     x coord
     * @param y     y coord
     * @param z     z coord
     * @param env   autoplace environment
     */
    public static boolean isGateOpen(World world, int x, int y, int z, AutoPlaceEnvironment env) {
        GateMode mode = ConfigurationHandler.INSTANCE.getFluidGateMode();
        if (mode == GateMode.NONE) return true;
        if (mode == GateMode.LENIENT) return isLocallyContained(world, x, y, z, env);
        Boolean cached = env.getFluidGateResult();
        if (cached != null) return cached;
        boolean ready = isStructureReady(world, env);
        env.setFluidGateResult(ready);
        return ready;
    }

    /**
     * Whether every non fluid element of the structure is satisfied, i.e. whether the structure is built far enough to
     * hold the fluid.
     * <p>
     * An autoplace environment that doesn't know its structure, e.g. one handed to a legacy caller, always answers
     * true, as there is nothing to be checked against.
     */
    private static boolean isStructureReady(World world, AutoPlaceEnvironment env) {
        IStructureDefinition<?> definition = env.getDefinition();
        int[] base = env.getBasePositionXYZ();
        if (definition == null || base == null) return true;
        Object context = env.getContextObject();
        int[] baseOffset = env.getBaseOffsetABC();
        ExtendedFacing facing = env.getFacing();
        IStructureElement<Object>[] elements = getElements(definition, env.getPiece());
        if (elements == null) return true;
        return StructureUtility.iterateV2(
                elements,
                world,
                facing,
                base[0],
                base[1],
                base[2],
                baseOffset[0],
                baseOffset[1],
                baseOffset[2],
                (element, w, ex, ey, ez, a, b, c) -> element.isFluidElement(context)
                        || element.check(context, w, ex, ey, ez),
                "fluid gate");
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static IStructureElement<Object>[] getElements(IStructureDefinition<?> definition, String piece) {
        try {
            return (IStructureElement<Object>[]) definition.getStructureFor(piece);
        } catch (RuntimeException e) {
            StructureLib.LOGGER
                    .warn("Could not look up structure piece {} to check whether it can hold fluid", piece, e);
            return null;
        }
    }

    /**
     * Whether the fluid cannot flow out of the structure from this position, i.e. whether every neighbour is either
     * blocked, or part of the structure and therefore eventually filled by it.
     */
    private static boolean isLocallyContained(World world, int x, int y, int z, AutoPlaceEnvironment env) {
        ExtendedFacing facing = env.getFacing();
        // An environment that doesn't know the structure cannot tell which neighbours belong to it. Don't block then.
        if (facing == null) return true;
        int[] abcOffset = new int[3];
        int[] xyzOffset = new int[3];
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            int nx = x + direction.offsetX;
            int ny = y + direction.offsetY;
            int nz = z + direction.offsetZ;
            if (isBlocking(world, nx, ny, nz)) continue;
            xyzOffset[0] = direction.offsetX;
            xyzOffset[1] = direction.offsetY;
            xyzOffset[2] = direction.offsetZ;
            facing.getOffsetABC(xyzOffset, abcOffset);
            if (env.isContainedInPiece(abcOffset[0], abcOffset[1], abcOffset[2])) continue;
            return false;
        }
        return true;
    }

    private static boolean isBlocking(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block == null) return true;
        if (block.isAir(world, x, y, z)) return false;
        if (block.getMaterial() != null && block.getMaterial().isLiquid()) return false;
        return !block.isReplaceable(world, x, y, z);
    }

    private static boolean canReplace(World world, int x, int y, int z, EntityPlayer actor) {
        Block block = world.getBlock(x, y, z);
        if (block == null || block.isAir(world, x, y, z)) return true;
        if (block.getMaterial() != null && block.getMaterial().isLiquid()) return true;
        if (block instanceof IFluidBlock) return true;
        return StructureLibAPI.isBlockTriviallyReplaceable(world, x, y, z, actor);
    }

    private static boolean placeFluidBlock(World world, int x, int y, int z, FluidBlockRequirement requirement) {
        FluidStack fluid = requirement.cost();
        Block block = requirement.block();
        if (requirement.placer() != null) {
            if (!requirement.placer().place(world, x, y, z, block, requirement.meta(), fluid)) return false;
        } else {
            // The notification flags matter here. A fluid that is put into the world silently neither flows nor tells
            // its neighbours about itself, so it would sit where it was placed until something else happens to touch
            // it, which for water inside a multiblock can be never.
            if (!world.setBlock(x, y, z, block, requirement.meta(), 3)) return false;
        }
        // A fluid that is never ticked does not spread, and a fluid placed as a flowing one would never settle.
        world.scheduleBlockUpdate(x, y, z, block, block.tickRate(world));
        return true;
    }

    private static void restore(World world, int x, int y, int z, Block block, int meta) {
        StructureLibAPI.restoreBlock(world, x, y, z, block, meta);
    }

    private static void reportMissingFluid(AutoPlaceEnvironment env, FluidBlockRequirement requirement, int missing) {
        Consumer<IChatComponent> chatter = env.getChatter();
        if (chatter == null) return;
        FluidStack stack = new FluidStack(requirement.cost(), Math.max(1, missing));
        chatter.accept(
                new ChatComponentTranslation(
                        "structurelib.autoplace.missing_fluid",
                        new ChatComponentFluidName(stack),
                        new ChatComponentFluid(stack)));
    }
}
