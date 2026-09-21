package com.gtnewhorizon.structurelib.fluid;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.oredict.OreDictionary;

/**
 * Tells StructureLib what it costs to place a fluid block, and how to place it.
 * <p>
 * A structure element that wants a fluid block, e.g. one created by {@code StructureUtility.ofBlock(Block, int)}, asks
 * this registry what the block state costs. Water and lava are registered by default, every other Forge fluid block is
 * recognised automatically with {@link #DEFAULT_FLUID_AMOUNT} per block, and a mod can register its own fluids, its own
 * costs, and its own placement logic here.
 * <p>
 * Registrations are keyed by block and block meta. Registering the wildcard meta {@link OreDictionary#WILDCARD_VALUE}
 * covers every meta of that block that does not have its own registration.
 */
public class FluidPlacementRegistry {

    /**
     * How much fluid one block of an automatically recognised fluid costs, which is one bucket by default.
     */
    public static final int DEFAULT_FLUID_AMOUNT = 1000;

    private static final Map<Block, Map<Integer, FluidBlockPlacement>> REGISTRY = new ConcurrentHashMap<>();
    /**
     * Block to fluid index, so that looking up a block that turns out not to be a fluid doesn't have to walk every
     * registered fluid. Rebuilt whenever the fluid registry grows, e.g. when a mod registers its fluids later than the
     * first lookup.
     */
    private static final Map<Block, Fluid> FLUID_BLOCKS = new ConcurrentHashMap<>();
    private static volatile int indexedFluidCount = -1;

    static {
        register(Blocks.water, 0);
        register(Blocks.flowing_water, 0);
        register(Blocks.lava, 0);
        register(Blocks.flowing_lava, 0);
    }

    private FluidPlacementRegistry() {}

    /**
     * Dummy method to force the class to initialize, and with it the default water and lava registrations.
     */
    public static void init() {}

    /**
     * Register what it costs to place the given block state, preferring the item form when the block has one.
     *
     * @param block the fluid block
     * @param meta  the block meta. {@link OreDictionary#WILDCARD_VALUE} covers every meta of this block.
     * @param cost  how much fluid one block of this state costs
     * @throws IllegalArgumentException if the block is null, the cost is invalid, or this state is already registered
     */
    public static void register(Block block, int meta, FluidStack cost) {
        register(block, meta, FluidBlockPlacement.of(cost));
    }

    /**
     * Register what it costs to place the given block state, with {@link #DEFAULT_FLUID_AMOUNT} of the fluid the block
     * holds, preferring the item form when the block has one.
     * <p>
     * The amount is the whole cost of that state. A block whose meta says how full it is therefore wants one
     * registration per meta it can be in, or no registration at all, in which case {@link #resolve(Block, int)} scales
     * the default amount down for a partially filled block.
     *
     * @param block the fluid block
     * @param meta  the block meta. {@link OreDictionary#WILDCARD_VALUE} covers every meta of this block.
     * @throws IllegalArgumentException if the block holds no fluid, or this state is already registered
     */
    public static void register(Block block, int meta) {
        register(block, meta, false);
    }

    /**
     * Register what it costs to place the given block state, with {@link #DEFAULT_FLUID_AMOUNT} of the fluid the block
     * holds.
     *
     * @param block      the fluid block
     * @param meta       the block meta. {@link OreDictionary#WILDCARD_VALUE} covers every meta of this block.
     * @param forceFluid whether to always pay with fluid, even when this block also has an item form
     * @throws IllegalArgumentException if the block holds no fluid, or this state is already registered
     */
    public static void register(Block block, int meta, boolean forceFluid) {
        FluidStack cost = FluidBlockPlacement.defaultCost(fluidOf(block));
        register(block, meta, forceFluid ? FluidBlockPlacement.forced(cost) : FluidBlockPlacement.of(cost));
    }

    /**
     * Register the whole block, i.e. every meta of it that has no registration of its own, with
     * {@link #DEFAULT_FLUID_AMOUNT} of the fluid it holds.
     *
     * @param block the fluid block
     * @throws IllegalArgumentException if the block holds no fluid, or the wildcard meta is already registered
     */
    public static void register(Block block) {
        register(block, OreDictionary.WILDCARD_VALUE);
    }

    /**
     * Register what it costs to place the given block state, with {@link #DEFAULT_FLUID_AMOUNT} of the fluid the block
     * holds, and placed the way the given placer says.
     * <p>
     * Use this for a fluid block that cannot be placed with a plain block set, e.g. one that has to be filled through
     * its own API.
     *
     * @param block  the fluid block
     * @param meta   the block meta. {@link OreDictionary#WILDCARD_VALUE} covers every meta of this block.
     * @param placer how to write the block into the world
     * @throws IllegalArgumentException if the block holds no fluid, or this state is already registered
     */
    public static void register(Block block, int meta, FluidBlockPlacer placer) {
        register(block, meta, FluidBlockPlacement.of(FluidBlockPlacement.defaultCost(fluidOf(block)), placer));
    }

    /**
     * Register what it costs to place the given block state.
     *
     * @param block      the fluid block
     * @param meta       the block meta. {@link OreDictionary#WILDCARD_VALUE} covers every meta of this block.
     * @param forceFluid whether to always pay with fluid, even when this block also has an item form
     * @param cost       how much fluid one block of this state costs
     * @throws IllegalArgumentException if the block is null, the cost is invalid, or this state is already registered
     */
    public static void register(Block block, int meta, FluidStack cost, boolean forceFluid) {
        register(block, meta, new FluidBlockPlacement(cost, forceFluid, null));
    }

    /**
     * Register what it costs to place the given block state, and how to place it.
     *
     * @param block     the fluid block
     * @param meta      the block meta. {@link OreDictionary#WILDCARD_VALUE} covers every meta of this block.
     * @param placement the cost and the placement logic
     * @throws IllegalArgumentException if the block or the placement is null, or this state is already registered
     */
    public static void register(Block block, int meta, FluidBlockPlacement placement) {
        if (block == null) throw new IllegalArgumentException("block must not be null");
        if (placement == null) throw new IllegalArgumentException("placement must not be null");
        Map<Integer, FluidBlockPlacement> byMeta = REGISTRY.computeIfAbsent(block, b -> new ConcurrentHashMap<>());
        if (byMeta.putIfAbsent(meta, placement) != null) {
            throw new IllegalArgumentException(
                    "Duplicate fluid placement for " + block.getUnlocalizedName() + ":" + meta);
        }
    }

    /**
     * What has been registered for exactly this block state.
     *
     * @return the registration, or null when there is none
     */
    @Nullable
    public static FluidBlockPlacement get(Block block, int meta) {
        if (block == null) return null;
        Map<Integer, FluidBlockPlacement> byMeta = REGISTRY.get(block);
        if (byMeta == null) return null;
        FluidBlockPlacement placement = byMeta.get(meta);
        return placement != null ? placement : byMeta.get(OreDictionary.WILDCARD_VALUE);
    }

    /**
     * What it costs to place this block state, whether it has been registered or not.
     * <p>
     * This is what autoplace asks. A registered state answers with its registration. Otherwise a block implementing
     * Forge's {@code IFluidBlock}, and any block whose material is a liquid, answer with {@link #DEFAULT_FLUID_AMOUNT}
     * of the fluid they hold, scaled down for a meta that asks for a partial block.
     *
     * @return the placement, or null when this block state is not a fluid block
     */
    @Nullable
    public static FluidBlockPlacement resolve(Block block, int meta) {
        FluidBlockPlacement placement = get(block, meta);
        if (placement != null) return placement;
        Fluid fluid = getFluidOf(block);
        if (fluid == null) return null;
        int amount = FluidAmounts.getAmountForMeta(block, meta, DEFAULT_FLUID_AMOUNT);
        if (amount <= 0) return null;
        return FluidBlockPlacement.of(new FluidStack(fluid, amount));
    }

    /**
     * Whether this block state is a fluid block that autoplace can place.
     */
    public static boolean isFluidBlock(Block block, int meta) {
        return resolve(block, meta) != null;
    }

    /**
     * Whether this block state can be placed as an item, i.e. whether the block has an item form.
     * <p>
     * Autoplace prefers the item form when there is one, so that blocks that are both a fluid block and an item keep
     * behaving the way they did before fluid autoplace existed.
     */
    public static boolean hasItemForm(Block block) {
        if (block == null) return false;
        return Item.getItemFromBlock(block) != null;
    }

    /**
     * The fluid held by given block, if any.
     * <p>
     * A block implementing Forge's {@code IFluidBlock} answers directly. Everything else is looked up in an index of
     * every registered fluid, and finally by material, which covers the vanilla water and lava blocks that no fluid
     * claims.
     *
     * @return the fluid, or null when this block does not hold one
     */
    @Nullable
    public static Fluid getFluidOf(Block block) {
        if (block == null) return null;
        if (block instanceof IFluidBlock fluidBlock) {
            Fluid fluid = fluidBlock.getFluid();
            if (fluid != null) return fluid;
        }
        Fluid byBlock = indexFluidBlocks().get(block);
        if (byBlock != null) return byBlock;
        Material material = block.getMaterial();
        if (material == Material.water) return FluidRegistry.WATER;
        if (material == Material.lava) return FluidRegistry.LAVA;
        return null;
    }

    /**
     * The fluid held by given block, for the registrations that default to {@link #DEFAULT_FLUID_AMOUNT} of it.
     *
     * @throws IllegalArgumentException if the block is null or holds no fluid
     */
    private static Fluid fluidOf(Block block) {
        Fluid fluid = getFluidOf(block);
        if (fluid == null) {
            throw new IllegalArgumentException(
                    "Block " + (block == null ? "null" : block.getUnlocalizedName())
                            + " holds no fluid,"
                            + " register it with an explicit cost instead");
        }
        return fluid;
    }

    private static synchronized Map<Block, Fluid> indexFluidBlocks() {
        Map<String, Fluid> fluids = FluidRegistry.getRegisteredFluids();
        if (fluids.size() == indexedFluidCount) return FLUID_BLOCKS;
        FLUID_BLOCKS.clear();
        for (Fluid fluid : fluids.values()) {
            if (fluid == null) continue;
            Block block = fluid.getBlock();
            if (block != null) FLUID_BLOCKS.put(block, fluid);
        }
        indexedFluidCount = fluids.size();
        return FLUID_BLOCKS;
    }
}
