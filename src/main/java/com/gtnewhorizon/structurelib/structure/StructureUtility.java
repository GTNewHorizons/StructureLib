package com.gtnewhorizon.structurelib.structure;

import static com.gtnewhorizon.structurelib.StructureLib.DEBUG_MODE;
import static java.lang.Integer.MIN_VALUE;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IStructureElement.PlaceResult;
import com.gtnewhorizon.structurelib.structure.adders.IBlockAdder;
import com.gtnewhorizon.structurelib.structure.adders.ITileAdder;
import com.gtnewhorizon.structurelib.util.ItemStackPredicate.NBTMode;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
import cpw.mods.fml.common.registry.GameRegistry;
import java.util.*;
import java.util.function.*;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Fluent API for structure checking!
 * <p>
 * (Just import static this class to have a nice fluent syntax while defining structure definitions)
 */
public class StructureUtility {
    private static final String NICE_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz=|!@#$%&()[]{};:<>/?_,.*^'`";

    @SuppressWarnings("rawtypes")
    private static final Map<Vec3Impl, IStructureNavigate> STEP = new HashMap<>();

    @SuppressWarnings("rawtypes")
    private static final IStructureElement AIR = new IStructureElement() {
        @Override
        public boolean check(Object t, World world, int x, int y, int z) {
            return world.isAirBlock(x, y, z);
        }

        @Override
        public boolean spawnHint(Object o, World world, int x, int y, int z, ItemStack trigger) {
            StructureLibAPI.hintParticle(world, x, y, z, StructureLibAPI.getBlockHint(), 13);
            return true;
        }

        @Override
        public boolean placeBlock(Object o, World world, int x, int y, int z, ItemStack trigger) {
            world.setBlock(x, y, z, Blocks.air, 0, 2);
            return false;
        }

        @Override
        public PlaceResult survivalPlaceBlock(
                Object o,
                World world,
                int x,
                int y,
                int z,
                ItemStack trigger,
                IItemSource s,
                EntityPlayerMP actor,
                Consumer chatter) {
            if (check(o, world, x, y, z)) return PlaceResult.SKIP;
            if (!StructureLibAPI.isBlockTriviallyReplaceable(world, x, y, z, actor)) return PlaceResult.REJECT;
            world.setBlock(x, y, z, Blocks.air, 0, 2);
            return PlaceResult.ACCEPT;
        }
    };

    @SuppressWarnings("rawtypes")
    private static final IStructureElement NOT_AIR = new IStructureElement() {
        @Override
        public boolean check(Object t, World world, int x, int y, int z) {
            return !world.isAirBlock(x, y, z);
        }

        @Override
        public boolean spawnHint(Object o, World world, int x, int y, int z, ItemStack trigger) {
            StructureLibAPI.hintParticle(world, x, y, z, StructureLibAPI.getBlockHint(), 14);
            return true;
        }

        @Override
        public boolean placeBlock(Object o, World world, int x, int y, int z, ItemStack trigger) {
            world.setBlock(x, y, z, StructureLibAPI.getBlockHint(), 14, 2);
            return true;
        }

        @Override
        public PlaceResult survivalPlaceBlock(
                Object o,
                World world,
                int x,
                int y,
                int z,
                ItemStack trigger,
                IItemSource s,
                EntityPlayerMP actor,
                Consumer chatter) {
            if (check(o, world, x, y, z)) return PlaceResult.SKIP;
            // user should place anything here.
            // maybe make this configurable, but for now we try to take some cobble from user
            if (s.takeOne(new ItemStack(Blocks.cobblestone), false)) {
                world.setBlock(x, y, z, Blocks.cobblestone, 0, 2);
            }
            return PlaceResult.REJECT;
        }
    };

    @SuppressWarnings("rawtypes")
    private static final IStructureElement ERROR = new IStructureElement() {
        @Override
        public boolean check(Object t, World world, int x, int y, int z) {
            return false;
        }

        @Override
        public boolean spawnHint(Object o, World world, int x, int y, int z, ItemStack trigger) {
            StructureLibAPI.hintParticle(world, x, y, z, StructureLibAPI.getBlockHint(), 15);
            return true;
        }

        @Override
        public boolean placeBlock(Object o, World world, int x, int y, int z, ItemStack trigger) {
            return true;
        }

        @Override
        public PlaceResult survivalPlaceBlock(
                Object o,
                World world,
                int x,
                int y,
                int z,
                ItemStack trigger,
                IItemSource s,
                EntityPlayerMP actor,
                Consumer chatter) {
            return PlaceResult.REJECT;
        }
    };

    private StructureUtility() {}

    /**
     * This method assume block at given coord is NOT acceptable, but may or may not be trivially replaceable
     */
    public static PlaceResult survivalPlaceBlock(
            Block block, int meta, World world, int x, int y, int z, IItemSource s, EntityPlayerMP actor) {
        return survivalPlaceBlock(block, meta, world, x, y, z, s, actor, null);
    }

    /**
     * This method assume block at given coord is NOT acceptable, but may or may not be trivially replaceable
     */
    public static PlaceResult survivalPlaceBlock(
            Block block,
            int meta,
            World world,
            int x,
            int y,
            int z,
            IItemSource s,
            EntityPlayerMP actor,
            Consumer<IChatComponent> chatter) {
        if (block == null) throw new NullPointerException();
        if (!StructureLibAPI.isBlockTriviallyReplaceable(world, x, y, z, actor)) return PlaceResult.REJECT;
        Item itemBlock = Item.getItemFromBlock(block);
        int itemMeta = itemBlock instanceof ISpecialItemBlock
                ? ((ISpecialItemBlock) itemBlock).getItemMetaFromBlockMeta(block, meta)
                : meta;
        if (!s.takeOne(new ItemStack(itemBlock, 1, itemMeta), false)) {
            if (chatter != null)
                chatter.accept(new ChatComponentTranslation(
                        "structurelib.autoplace.error.no_simple_block",
                        new ItemStack(itemBlock, 1, itemMeta).func_151000_E()));
            return PlaceResult.REJECT;
        }
        if (block instanceof ICustomBlockSetting) {
            ICustomBlockSetting block2 = (ICustomBlockSetting) block;
            block2.setBlock(world, x, y, z, meta);
        } else {
            world.setBlock(x, y, z, block, meta, 2);
        }
        return PlaceResult.ACCEPT;
    }

    /**
     * This method assume block at given coord is NOT acceptable, but may or may not be trivially replaceable
     *
     * @param stack   a valid stack with stack size of exactly 1. Must be of an ItemBlock!
     */
    public static PlaceResult survivalPlaceBlock(
            ItemStack stack,
            NBTMode nbtMode,
            NBTTagCompound tag,
            boolean assumeStackPresent,
            World world,
            int x,
            int y,
            int z,
            IItemSource s,
            EntityPlayerMP actor) {
        return survivalPlaceBlock(stack, nbtMode, tag, assumeStackPresent, world, x, y, z, s, actor, null);
    }

    /**
     * This method assume block at given coord is NOT acceptable, but may or may not be trivially replaceable
     *
     * @param stack   a valid stack with stack size of exactly 1. Must be of an ItemBlock!
     * @param chatter pass in null to suppress error output, or just use the other overload
     */
    public static PlaceResult survivalPlaceBlock(
            ItemStack stack,
            NBTMode nbtMode,
            NBTTagCompound tag,
            boolean assumeStackPresent,
            World world,
            int x,
            int y,
            int z,
            IItemSource s,
            EntityPlayerMP actor,
            @Nullable Consumer<IChatComponent> chatter) {
        if (stack == null) throw new NullPointerException();
        if (stack.stackSize != 1) throw new IllegalArgumentException();
        if (!(stack.getItem() instanceof ItemBlock)) throw new IllegalArgumentException();
        if (!StructureLibAPI.isBlockTriviallyReplaceable(world, x, y, z, actor)) return PlaceResult.REJECT;
        if (!assumeStackPresent && !s.takeOne(stack, true)) {
            if (chatter != null)
                chatter.accept(new ChatComponentTranslation(
                        "structurelib.autoplace.error.no_item_stack", stack.func_151000_E()));
            return PlaceResult.REJECT;
        }
        if (!stack.copy().tryPlaceItemIntoWorld(actor, world, x, y, z, ForgeDirection.UP.ordinal(), 0.5f, 0.5f, 0.5f))
            return PlaceResult.REJECT;
        if (!s.takeOne(stack, false))
            // this is bad! probably an exploit somehow. Let's nullify the block we just placed instead
            world.setBlockToAir(x, y, z);
        return PlaceResult.ACCEPT;
    }

    @SuppressWarnings("unchecked")
    public static <T> IStructureElement<T> isAir() {
        return AIR;
    }

    @SuppressWarnings("unchecked")
    public static <T> IStructureElement<T> notAir() {
        return NOT_AIR;
    }

    /**
     * Check returns false.
     * Placement is always handled by this and does nothing.
     * Makes little to no use it in  fallback chain.
     */
    @SuppressWarnings("unchecked")
    public static <T> IStructureElement<T> error() {
        return ERROR;
    }

    // region hint only

    /**
     * Check always returns: true.
     */
    public static <T> IStructureElementNoPlacement<T> ofHint(int dots) {
        int meta = dots - 1;
        return new IStructureElementNoPlacement<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return true;
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                StructureLibAPI.hintParticle(world, x, y, z, StructureLibAPI.getBlockHint(), meta);
                return false;
            }
        };
    }

    /**
     * Check always returns: true.
     */
    public static <T> IStructureElementNoPlacement<T> ofHintDeferred(Supplier<IIcon[]> icons) {
        return new IStructureElementNoPlacement<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return true;
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                StructureLibAPI.hintParticle(world, x, y, z, icons.get());
                return false;
            }
        };
    }

    /**
     * Check always returns: true.
     */
    public static <T> IStructureElementNoPlacement<T> ofHintDeferred(Supplier<IIcon[]> icons, short[] RGBa) {
        return new IStructureElementNoPlacement<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return true;
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                StructureLibAPI.hintParticleTinted(world, x, y, z, icons.get(), RGBa);
                return false;
            }
        };
    }

    // endregion

    // region block

    /**
     * A more primitive variant of {@link #ofBlocksTiered(ITierConverter, List, Object, BiConsumer, Function)}
     * Main difference is this one is check only.
     *
     * @see #ofBlocksTiered(ITierConverter, Object, BiConsumer, Function)
     */
    public static <T, TIER> IStructureElementCheckOnly<T> ofBlocksTiered(
            ITierConverter<TIER> tierExtractor,
            @Nullable TIER notSet,
            BiConsumer<T, TIER> setter,
            Function<T, TIER> getter) {
        if (tierExtractor == null) throw new IllegalArgumentException();
        if (setter == null) throw new IllegalArgumentException();
        if (getter == null) throw new IllegalArgumentException();

        return (t, world, x, y, z) -> {
            Block block = world.getBlock(x, y, z);
            int meta = world.getBlockMetadata(x, y, z);
            TIER tier = tierExtractor.convert(block, meta);
            TIER current = getter.apply(t);
            if (Objects.equals(notSet, current)) {
                setter.accept(t, tier);
                return true;
            }
            return Objects.equals(current, tier);
        };
    }

    /**
     * Element representing a component with different tiers. Player can use more blueprint to get hints denoting more
     * advanced components.
     * <p>
     * There is yet no TileEntity counter part of this utility. Feel free to submit a PR to add it.
     *
     * @param allKnownTiers All known tiers as of calling. Can be empty or null. No hint will be spawned if empty or null. Cannot have null elements.
     *                      First element denotes the most primitive tier. Last element denotes the most advanced tier.
     *                      If not all tiers are available at definition construction time, use {@link #lazy(Supplier)} or its overloads to delay a little bit.
     * @param notSet        The value returned from {@code getter} when there were no tier info found in T yet. Can be null.
     * @param getter        a function to retrieve the current tier from T
     * @param setter        a function to set the current tier into T
     * @param tierExtractor a function to extract tier info from a block.
     */
    public static <T, TIER> IStructureElement<T> ofBlocksTiered(
            ITierConverter<TIER> tierExtractor,
            @Nullable List<Pair<Block, Integer>> allKnownTiers,
            @Nullable TIER notSet,
            BiConsumer<T, TIER> setter,
            Function<T, TIER> getter) {
        List<Pair<Block, Integer>> hints = allKnownTiers == null ? Collections.emptyList() : allKnownTiers;
        if (hints.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException();
        IStructureElementCheckOnly<T> check = ofBlocksTiered(tierExtractor, notSet, setter, getter);
        return new IStructureElement<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return check.check(t, world, x, y, z);
            }

            private Pair<Block, Integer> getHint(ItemStack trigger) {
                return hints.get(Math.min(Math.max(trigger.stackSize, 1), hints.size()) - 1);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                Pair<Block, Integer> hint = getHint(trigger);
                if (hint == null) return false;
                StructureLibAPI.hintParticle(world, x, y, z, hint.getKey(), hint.getValue());
                return true;
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                Pair<Block, Integer> hint = getHint(trigger);
                if (hint == null) return false;
                if (hint.getKey() instanceof ICustomBlockSetting) {
                    ICustomBlockSetting block = (ICustomBlockSetting) hint.getKey();
                    block.setBlock(world, x, y, z, hint.getValue());
                } else {
                    world.setBlock(x, y, z, hint.getKey(), hint.getValue(), 2);
                }
                return true;
            }

            @Override
            public PlaceResult survivalPlaceBlock(
                    T t,
                    World world,
                    int x,
                    int y,
                    int z,
                    ItemStack trigger,
                    IItemSource s,
                    EntityPlayerMP actor,
                    Consumer<IChatComponent> chatter) {
                if (check(t, world, x, y, z)) return PlaceResult.SKIP;
                Pair<Block, Integer> hint = getHint(trigger);
                if (hint == null) return PlaceResult.REJECT; // TODO or SKIP?
                return StructureUtility.survivalPlaceBlock(
                        hint.getKey(), hint.getValue(), world, x, y, z, s, actor, chatter);
            }
        };
    }

    /**
     * Denote a block using unlocalized names. This can be useful to get around mod loading order issues.
     * <p>
     * While no immediate error will be thrown, client code should ensure said mod is loaded and
     * said mod is present, otherwise bad things will happen later!
     */
    public static <T> IStructureElement<T> ofBlockUnlocalizedName(String modid, String unlocalizedName, int meta) {
        return ofBlockUnlocalizedName(modid, unlocalizedName, meta, false);
    }

    /**
     * Denote a block using unlocalized names. This can be useful to get around mod loading order issues.
     * <p>
     * While no immediate error will be thrown, client code should ensure said mod is loaded and
     * said mod is present, otherwise bad things will happen later!
     * <p>
     * Will place block or hint using the given meta if wildcard is true.
     */
    public static <T> IStructureElement<T> ofBlockUnlocalizedName(
            String modid, String unlocalizedName, int meta, boolean wildcard) {
        if (StringUtils.isBlank(unlocalizedName)) throw new IllegalArgumentException();
        if (meta < 0) throw new IllegalArgumentException();
        if (meta > 15) throw new IllegalArgumentException();
        if (StringUtils.isBlank(modid)) throw new IllegalArgumentException();
        return new IStructureElement<T>() {
            private Block block;

            private Block getBlock() {
                if (block == null) block = GameRegistry.findBlock(modid, unlocalizedName);
                return block;
            }

            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return world.getBlock(x, y, z) == getBlock() && (wildcard || world.getBlockMetadata(x, y, z) == meta);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                if (getBlock() == null) return error().spawnHint(t, world, x, y, z, trigger);
                StructureLibAPI.hintParticle(world, x, y, z, getBlock(), meta);
                return true;
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                if (getBlock() == null) return error().placeBlock(t, world, x, y, z, trigger);
                world.setBlock(x, y, z, getBlock(), meta, 2);
                return true;
            }

            @Override
            public PlaceResult survivalPlaceBlock(
                    T t,
                    World world,
                    int x,
                    int y,
                    int z,
                    ItemStack trigger,
                    IItemSource s,
                    EntityPlayerMP actor,
                    Consumer<IChatComponent> chatter) {
                if (check(t, world, x, y, z)) return PlaceResult.SKIP;
                if (getBlock() == null) return PlaceResult.REJECT;
                return StructureUtility.survivalPlaceBlock(getBlock(), meta, world, x, y, z, s, actor, chatter);
            }
        };
    }

    /**
     * Similiar to the other overload, but allows client code to specify a fallback in case said block was not found later
     * when the element got called.
     * <p>
     * This is slightly different to using the other overload and another element in a {@link #ofChain(IStructureElement[])},
     * as that combination would cause crash, where this won't.
     */
    public static <T> IStructureElement<T> ofBlockUnlocalizedName(
            String modid, String unlocalizedName, int meta, IStructureElement<T> fallback) {
        if (StringUtils.isBlank(unlocalizedName)) throw new IllegalArgumentException();
        if (meta < 0) throw new IllegalArgumentException();
        if (meta > 15) throw new IllegalArgumentException();
        if (StringUtils.isBlank(modid)) throw new IllegalArgumentException();
        if (fallback == null) throw new IllegalArgumentException();
        return new IStructureElement<T>() {
            private Block block;
            private boolean initialized;

            private boolean init() {
                if (!initialized) {
                    block = GameRegistry.findBlock(modid, unlocalizedName);
                    initialized = true;
                }
                return block != null;
            }

            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                if (init()) return world.getBlock(x, y, z) != block && world.getBlockMetadata(x, y, z) == meta;
                else return fallback.check(t, world, x, y, z);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                if (init()) {
                    StructureLibAPI.hintParticle(world, x, y, z, block, meta);
                    return true;
                } else return fallback.spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                if (init()) {
                    world.setBlock(x, y, z, block, meta, 2);
                    return true;
                } else return fallback.placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(
                    T t,
                    World world,
                    int x,
                    int y,
                    int z,
                    ItemStack trigger,
                    IItemSource s,
                    EntityPlayerMP actor,
                    Consumer<IChatComponent> chatter) {
                if (check(t, world, x, y, z)) return PlaceResult.SKIP;
                if (init()) return StructureUtility.survivalPlaceBlock(block, meta, world, x, y, z, s, actor, chatter);
                return fallback.survivalPlaceBlock(t, world, x, y, z, trigger, s, actor, chatter);
            }
        };
    }

    /**
     * Does not allow Block duplicates (with different meta)
     */
    public static <T> IStructureElementNoPlacement<T> ofBlocksFlatHint(
            Map<Block, Integer> blocsMap, Block hintBlock, int hintMeta) {
        if (blocsMap == null || blocsMap.isEmpty() || hintBlock == null) {
            throw new IllegalArgumentException();
        }
        return new IStructureElementNoPlacement<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                Block worldBlock = world.getBlock(x, y, z);
                return blocsMap.getOrDefault(worldBlock, MIN_VALUE) == worldBlock.getDamageValue(world, x, y, z);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                StructureLibAPI.hintParticle(world, x, y, z, hintBlock, hintMeta);
                return true;
            }
        };
    }

    /**
     * Allows block duplicates (with different meta)
     */
    public static <T> IStructureElementNoPlacement<T> ofBlocksMapHint(
            Map<Block, Collection<Integer>> blocsMap, Block hintBlock, int hintMeta) {
        if (blocsMap == null || blocsMap.isEmpty() || hintBlock == null) {
            throw new IllegalArgumentException();
        }
        for (Collection<Integer> value : blocsMap.values()) {
            if (value.isEmpty()) {
                throw new IllegalArgumentException();
            }
        }
        return new IStructureElementNoPlacement<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                Block worldBlock = world.getBlock(x, y, z);
                return blocsMap.getOrDefault(worldBlock, Collections.emptySet())
                        .contains(worldBlock.getDamageValue(world, x, y, z));
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                StructureLibAPI.hintParticle(world, x, y, z, hintBlock, hintMeta);
                return true;
            }
        };
    }

    public static <T> IStructureElementNoPlacement<T> ofBlockHint(
            Block block, int meta, Block hintBlock, int hintMeta) {
        if (block == null || hintBlock == null) {
            throw new IllegalArgumentException();
        }
        return new IStructureElementNoPlacement<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                Block worldBlock = world.getBlock(x, y, z);
                return block == worldBlock && meta == worldBlock.getDamageValue(world, x, y, z);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                StructureLibAPI.hintParticle(world, x, y, z, hintBlock, hintMeta);
                return true;
            }
        };
    }

    public static <T> IStructureElementNoPlacement<T> ofBlockHint(Block block, int meta) {
        return ofBlockHint(block, meta, block, meta);
    }

    public static <T> IStructureElementNoPlacement<T> ofBlockAdderHint(
            IBlockAdder<T> iBlockAdder, Block hintBlock, int hintMeta) {
        if (iBlockAdder == null || hintBlock == null) {
            throw new IllegalArgumentException();
        }
        return new IStructureElementNoPlacement<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                Block worldBlock = world.getBlock(x, y, z);
                return iBlockAdder.apply(t, worldBlock, worldBlock.getDamageValue(world, x, y, z));
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                StructureLibAPI.hintParticle(world, x, y, z, hintBlock, hintMeta);
                return true;
            }
        };
    }

    /**
     * Does not allow Block duplicates (with different meta)
     */
    public static <T> IStructureElement<T> ofBlocksFlat(
            Map<Block, Integer> blocsMap, Block defaultBlock, int defaultMeta) {
        if (blocsMap == null || blocsMap.isEmpty() || defaultBlock == null) {
            throw new IllegalArgumentException();
        }
        if (defaultBlock instanceof ICustomBlockSetting) {
            return new IStructureElement<T>() {
                @Override
                public boolean check(T t, World world, int x, int y, int z) {
                    Block worldBlock = world.getBlock(x, y, z);
                    return blocsMap.getOrDefault(worldBlock, MIN_VALUE) == worldBlock.getDamageValue(world, x, y, z);
                }

                @Override
                public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                    ((ICustomBlockSetting) defaultBlock).setBlock(world, x, y, z, defaultMeta);
                    return true;
                }

                @Override
                public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                    StructureLibAPI.hintParticle(world, x, y, z, defaultBlock, defaultMeta);
                    return true;
                }

                @Override
                public PlaceResult survivalPlaceBlock(
                        T t,
                        World world,
                        int x,
                        int y,
                        int z,
                        ItemStack trigger,
                        IItemSource s,
                        EntityPlayerMP actor,
                        Consumer<IChatComponent> chatter) {
                    if (check(t, world, x, y, z)) return PlaceResult.SKIP;
                    return StructureUtility.survivalPlaceBlock(
                            defaultBlock, defaultMeta, world, x, y, z, s, actor, chatter);
                }
            };
        } else {
            return new IStructureElement<T>() {
                @Override
                public boolean check(T t, World world, int x, int y, int z) {
                    Block worldBlock = world.getBlock(x, y, z);
                    return blocsMap.getOrDefault(worldBlock, MIN_VALUE) == worldBlock.getDamageValue(world, x, y, z);
                }

                @Override
                public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                    world.setBlock(x, y, z, defaultBlock, defaultMeta, 2);
                    return true;
                }

                @Override
                public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                    StructureLibAPI.hintParticle(world, x, y, z, defaultBlock, defaultMeta);
                    return true;
                }

                @Override
                public PlaceResult survivalPlaceBlock(
                        T t,
                        World world,
                        int x,
                        int y,
                        int z,
                        ItemStack trigger,
                        IItemSource s,
                        EntityPlayerMP actor,
                        Consumer<IChatComponent> chatter) {
                    if (check(t, world, x, y, z)) return PlaceResult.SKIP;
                    return StructureUtility.survivalPlaceBlock(
                            defaultBlock, defaultMeta, world, x, y, z, s, actor, chatter);
                }
            };
        }
    }

    /**
     * Allows block duplicates (with different meta)
     */
    public static <T> IStructureElement<T> ofBlocksMap(
            Map<Block, Collection<Integer>> blocsMap, Block defaultBlock, int defaultMeta) {
        if (blocsMap == null || blocsMap.isEmpty() || defaultBlock == null) {
            throw new IllegalArgumentException();
        }
        for (Collection<Integer> value : blocsMap.values()) {
            if (value.isEmpty()) {
                throw new IllegalArgumentException();
            }
        }
        if (defaultBlock instanceof ICustomBlockSetting) {
            return new IStructureElement<T>() {
                @Override
                public boolean check(T t, World world, int x, int y, int z) {
                    Block worldBlock = world.getBlock(x, y, z);
                    return blocsMap.getOrDefault(worldBlock, Collections.emptySet())
                            .contains(worldBlock.getDamageValue(world, x, y, z));
                }

                @Override
                public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                    ((ICustomBlockSetting) defaultBlock).setBlock(world, x, y, z, defaultMeta);
                    return true;
                }

                @Override
                public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                    StructureLibAPI.hintParticle(world, x, y, z, defaultBlock, defaultMeta);
                    return true;
                }

                @Override
                public PlaceResult survivalPlaceBlock(
                        T t,
                        World world,
                        int x,
                        int y,
                        int z,
                        ItemStack trigger,
                        IItemSource s,
                        EntityPlayerMP actor,
                        Consumer<IChatComponent> chatter) {
                    if (check(t, world, x, y, z)) return PlaceResult.SKIP;
                    return StructureUtility.survivalPlaceBlock(
                            defaultBlock, defaultMeta, world, x, y, z, s, actor, chatter);
                }
            };
        } else {
            return new IStructureElement<T>() {
                @Override
                public boolean check(T t, World world, int x, int y, int z) {
                    Block worldBlock = world.getBlock(x, y, z);
                    return blocsMap.getOrDefault(worldBlock, Collections.emptySet())
                            .contains(worldBlock.getDamageValue(world, x, y, z));
                }

                @Override
                public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                    world.setBlock(x, y, z, defaultBlock, defaultMeta, 2);
                    return true;
                }

                @Override
                public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                    StructureLibAPI.hintParticle(world, x, y, z, defaultBlock, defaultMeta);
                    return true;
                }

                @Override
                public PlaceResult survivalPlaceBlock(
                        T t,
                        World world,
                        int x,
                        int y,
                        int z,
                        ItemStack trigger,
                        IItemSource s,
                        EntityPlayerMP actor,
                        Consumer<IChatComponent> chatter) {
                    if (check(t, world, x, y, z)) return PlaceResult.SKIP;
                    return StructureUtility.survivalPlaceBlock(
                            defaultBlock, defaultMeta, world, x, y, z, s, actor, chatter);
                }
            };
        }
    }

    public static <T> IStructureElement<T> ofBlock(Block block, int meta, Block defaultBlock, int defaultMeta) {
        if (block == null || defaultBlock == null) {
            throw new IllegalArgumentException();
        }
        if (block instanceof ICustomBlockSetting) {
            return new IStructureElement<T>() {
                @Override
                public boolean check(T t, World world, int x, int y, int z) {
                    Block worldBlock = world.getBlock(x, y, z);
                    return block == worldBlock && meta == worldBlock.getDamageValue(world, x, y, z);
                }

                @Override
                public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                    ((ICustomBlockSetting) defaultBlock).setBlock(world, x, y, z, defaultMeta);
                    return true;
                }

                @Override
                public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                    StructureLibAPI.hintParticle(world, x, y, z, defaultBlock, defaultMeta);
                    return true;
                }

                @Override
                public PlaceResult survivalPlaceBlock(
                        T t,
                        World world,
                        int x,
                        int y,
                        int z,
                        ItemStack trigger,
                        IItemSource s,
                        EntityPlayerMP actor,
                        Consumer<IChatComponent> chatter) {
                    if (check(t, world, x, y, z)) return PlaceResult.SKIP;
                    return StructureUtility.survivalPlaceBlock(
                            defaultBlock, defaultMeta, world, x, y, z, s, actor, chatter);
                }
            };
        } else {
            return new IStructureElement<T>() {
                @Override
                public boolean check(T t, World world, int x, int y, int z) {
                    Block worldBlock = world.getBlock(x, y, z);
                    return block == worldBlock && meta == worldBlock.getDamageValue(world, x, y, z);
                }

                @Override
                public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                    world.setBlock(x, y, z, defaultBlock, defaultMeta, 2);
                    return true;
                }

                @Override
                public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                    StructureLibAPI.hintParticle(world, x, y, z, defaultBlock, defaultMeta);
                    return true;
                }

                @Override
                public PlaceResult survivalPlaceBlock(
                        T t,
                        World world,
                        int x,
                        int y,
                        int z,
                        ItemStack trigger,
                        IItemSource s,
                        EntityPlayerMP actor,
                        Consumer<IChatComponent> chatter) {
                    if (check(t, world, x, y, z)) return PlaceResult.SKIP;
                    return StructureUtility.survivalPlaceBlock(
                            defaultBlock, defaultMeta, world, x, y, z, s, actor, chatter);
                }
            };
        }
    }

    /**
     * Same as above but ignores target meta id
     */
    public static <T> IStructureElement<T> ofBlockAnyMeta(Block block, Block defaultBlock, int defaultMeta) {
        if (block == null || defaultBlock == null) {
            throw new IllegalArgumentException();
        }
        if (block instanceof ICustomBlockSetting) {
            return new IStructureElement<T>() {
                @Override
                public boolean check(T t, World world, int x, int y, int z) {
                    return block == world.getBlock(x, y, z);
                }

                @Override
                public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                    ((ICustomBlockSetting) defaultBlock).setBlock(world, x, y, z, defaultMeta);
                    return true;
                }

                @Override
                public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                    StructureLibAPI.hintParticle(world, x, y, z, defaultBlock, defaultMeta);
                    return true;
                }

                @Override
                public PlaceResult survivalPlaceBlock(
                        T t,
                        World world,
                        int x,
                        int y,
                        int z,
                        ItemStack trigger,
                        IItemSource s,
                        EntityPlayerMP actor,
                        Consumer<IChatComponent> chatter) {
                    if (check(t, world, x, y, z)) return PlaceResult.SKIP;
                    return StructureUtility.survivalPlaceBlock(
                            defaultBlock, defaultMeta, world, x, y, z, s, actor, chatter);
                }
            };
        } else {
            return new IStructureElement<T>() {
                @Override
                public boolean check(T t, World world, int x, int y, int z) {
                    return block == world.getBlock(x, y, z);
                }

                @Override
                public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                    world.setBlock(x, y, z, defaultBlock, defaultMeta, 2);
                    return true;
                }

                @Override
                public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                    StructureLibAPI.hintParticle(world, x, y, z, defaultBlock, defaultMeta);
                    return true;
                }

                @Override
                public PlaceResult survivalPlaceBlock(
                        T t,
                        World world,
                        int x,
                        int y,
                        int z,
                        ItemStack trigger,
                        IItemSource s,
                        EntityPlayerMP actor,
                        Consumer<IChatComponent> chatter) {
                    if (check(t, world, x, y, z)) return PlaceResult.SKIP;
                    return StructureUtility.survivalPlaceBlock(
                            defaultBlock, defaultMeta, world, x, y, z, s, actor, chatter);
                }
            };
        }
    }

    public static <T> IStructureElement<T> ofBlock(Block block, int meta) {
        return ofBlock(block, meta, block, meta);
    }

    /**
     * Same as above but ignores target meta id
     */
    public static <T> IStructureElement<T> ofBlockAnyMeta(Block block) {
        return ofBlockAnyMeta(block, block, 0);
    }

    /**
     * Same as above but allows to set hint particle render
     */
    public static <T> IStructureElement<T> ofBlockAnyMeta(Block block, int defaultMeta) {
        return ofBlockAnyMeta(block, block, defaultMeta);
    }

    // endregion

    // region adders

    public static <T> IStructureElement<T> ofBlockAdder(
            IBlockAdder<T> iBlockAdder, Block defaultBlock, int defaultMeta) {
        if (iBlockAdder == null || defaultBlock == null) {
            throw new IllegalArgumentException();
        }
        if (defaultBlock instanceof ICustomBlockSetting) {
            return new IStructureElement<T>() {
                @Override
                public boolean check(T t, World world, int x, int y, int z) {
                    Block worldBlock = world.getBlock(x, y, z);
                    return iBlockAdder.apply(t, worldBlock, worldBlock.getDamageValue(world, x, y, z));
                }

                @Override
                public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                    ((ICustomBlockSetting) defaultBlock).setBlock(world, x, y, z, defaultMeta);
                    return true;
                }

                @Override
                public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                    StructureLibAPI.hintParticle(world, x, y, z, defaultBlock, defaultMeta);
                    return true;
                }

                @Override
                public PlaceResult survivalPlaceBlock(
                        T t,
                        World world,
                        int x,
                        int y,
                        int z,
                        ItemStack trigger,
                        IItemSource s,
                        EntityPlayerMP actor,
                        Consumer<IChatComponent> chatter) {
                    return StructureUtility.survivalPlaceBlock(
                            defaultBlock, defaultMeta, world, x, y, z, s, actor, chatter);
                }
            };
        } else {
            return new IStructureElement<T>() {
                @Override
                public boolean check(T t, World world, int x, int y, int z) {
                    Block worldBlock = world.getBlock(x, y, z);
                    return iBlockAdder.apply(t, worldBlock, worldBlock.getDamageValue(world, x, y, z));
                }

                @Override
                public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                    world.setBlock(x, y, z, defaultBlock, defaultMeta, 2);
                    return true;
                }

                @Override
                public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                    StructureLibAPI.hintParticle(world, x, y, z, defaultBlock, defaultMeta);
                    return true;
                }

                @Override
                public PlaceResult survivalPlaceBlock(
                        T t,
                        World world,
                        int x,
                        int y,
                        int z,
                        ItemStack trigger,
                        IItemSource s,
                        EntityPlayerMP actor,
                        Consumer<IChatComponent> chatter) {
                    return StructureUtility.survivalPlaceBlock(
                            defaultBlock, defaultMeta, world, x, y, z, s, actor, chatter);
                }
            };
        }
    }

    public static <T> IStructureElement<T> ofBlockAdder(IBlockAdder<T> iBlockAdder, int dots) {
        return ofBlockAdder(iBlockAdder, StructureLibAPI.getBlockHint(), dots - 1);
    }

    public static <T> IStructureElementNoPlacement<T> ofTileAdder(
            ITileAdder<T> iTileAdder, Block hintBlock, int hintMeta) {
        if (iTileAdder == null || hintBlock == null) {
            throw new IllegalArgumentException();
        }
        return new IStructureElementNoPlacement<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                TileEntity tileEntity = world.getTileEntity(x, y, z);
                // This used to check if it's a GT tile. Since this is now an standalone mod we no longer do this
                return iTileAdder.apply(t, tileEntity);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                StructureLibAPI.hintParticle(world, x, y, z, hintBlock, hintMeta);
                return true;
            }
        };
    }

    public static <T, E> IStructureElementNoPlacement<T> ofSpecificTileAdder(
            BiPredicate<T, E> iTileAdder, Class<E> tileClass, Block hintBlock, int hintMeta) {
        if (iTileAdder == null || hintBlock == null || tileClass == null) {
            throw new IllegalArgumentException();
        }
        return new IStructureElementNoPlacement<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                TileEntity tileEntity = world.getTileEntity(x, y, z);
                // This used to check if it's a GT tile. Since this is now an standalone mod we no longer do this
                return tileClass.isInstance(tileEntity) && iTileAdder.test(t, tileClass.cast(tileEntity));
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                StructureLibAPI.hintParticle(world, x, y, z, hintBlock, hintMeta);
                return true;
            }
        };
    }
    // No more hatch adder. Implement it via tile adder. We could of course add a wrapper around it in gregtech, but not
    // any more in this standalone mod.

    // endregion

    // region side effects

    public static <B extends IStructureElement<T>, T> IStructureElement<T> onElementPass(
            Consumer<T> onCheckPass, B element) {
        return new IStructureElement<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                boolean check = element.check(t, world, x, y, z);
                if (check) {
                    onCheckPass.accept(t);
                }
                return check;
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                return element.placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                return element.spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(
                    T t,
                    World world,
                    int x,
                    int y,
                    int z,
                    ItemStack trigger,
                    IItemSource s,
                    EntityPlayerMP actor,
                    Consumer<IChatComponent> chatter) {
                return element.survivalPlaceBlock(t, world, x, y, z, trigger, s, actor, chatter);
            }
        };
    }

    public static <B extends IStructureElement<T>, T> IStructureElement<T> onElementFail(
            Consumer<T> onFail, B element) {
        return new IStructureElement<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                boolean check = element.check(t, world, x, y, z);
                if (!check) {
                    onFail.accept(t);
                }
                return check;
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                return element.placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                return element.spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(
                    T t,
                    World world,
                    int x,
                    int y,
                    int z,
                    ItemStack trigger,
                    IItemSource s,
                    EntityPlayerMP actor,
                    Consumer<IChatComponent> chatter) {
                return element.survivalPlaceBlock(t, world, x, y, z, trigger, s, actor, chatter);
            }
        };
    }

    // endregion

    /**
     * Enable this structure element only if given predicate returns true.
     */
    public static <T> IStructureElement<T> onlyIf(
            Predicate<? super T> predicate, IStructureElement<? super T> downstream) {
        return onlyIf(predicate, downstream, PlaceResult.SKIP);
    }

    /**
     * Enable this structure element only if given predicate returns true.
     */
    public static <T> IStructureElement<T> onlyIf(
            Predicate<? super T> predicate,
            IStructureElement<? super T> downstream,
            PlaceResult placeResultWhenDisabled) {
        return new IStructureElement<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return predicate.test(t) && downstream.check(t, world, x, y, z);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                return predicate.test(t) && downstream.spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                return predicate.test(t) && downstream.placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(
                    T t,
                    World world,
                    int x,
                    int y,
                    int z,
                    ItemStack trigger,
                    IItemSource s,
                    EntityPlayerMP actor,
                    Consumer<IChatComponent> chatter) {
                if (predicate.test(t))
                    return downstream.survivalPlaceBlock(t, world, x, y, z, trigger, s, actor, chatter);
                return placeResultWhenDisabled;
            }
        };
    }

    /**
     * Take care while chaining, as it will try to call every structure element until it returns true.
     * If none does it will finally return false.
     */
    @SafeVarargs
    public static <T> IStructureElementChain<T> ofChain(IStructureElement<T>... elementChain) {
        if (elementChain == null || elementChain.length == 0) {
            throw new IllegalArgumentException();
        }
        for (IStructureElement<T> iStructureElement : elementChain) {
            if (iStructureElement == null) {
                throw new IllegalArgumentException();
            }
        }
        return () -> elementChain;
    }

    /**
     * Take care while chaining, as it will try to call every structure element until it returns true.
     * If none does it will finally return false.
     */
    @SuppressWarnings("unchecked")
    public static <T> IStructureElementChain<T> ofChain(List<IStructureElement<T>> elementChain) {
        return ofChain(elementChain.toArray(new IStructureElement[0]));
    }

    // region context
    public static <CTX, T extends IWithExtendedContext<CTX>, E extends IStructureElement<CTX>>
            IStructureElement<T> withContext(E elem) {
        return new IStructureElement<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return elem.check(t.getCurrentContext(), world, x, y, z);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                return elem.spawnHint(t.getCurrentContext(), world, x, y, z, trigger);
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                return elem.placeBlock(t.getCurrentContext(), world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(
                    T t,
                    World world,
                    int x,
                    int y,
                    int z,
                    ItemStack trigger,
                    IItemSource s,
                    EntityPlayerMP actor,
                    Consumer<IChatComponent> chatter) {
                return elem.survivalPlaceBlock(t.getCurrentContext(), world, x, y, z, trigger, s, actor, chatter);
            }
        };
    }
    // endregion

    // region defer

    /**
     * Similar to defer, but caches the first returned element returned and won't call it again.
     * Initialization is not thread safe.
     */
    public static <T> IStructureElementDeferred<T> lazy(Supplier<IStructureElement<T>> to) {
        if (to == null) {
            throw new IllegalArgumentException();
        }
        return new LazyStructureElement<>(t -> to.get());
    }

    /**
     * Similar to defer, but caches the first returned element returned and won't call it again.
     * Initialization is not thread safe.
     */
    public static <T> IStructureElementDeferred<T> lazy(Function<T, IStructureElement<T>> to) {
        if (to == null) {
            throw new IllegalArgumentException();
        }
        return new LazyStructureElement<>(to);
    }

    public static <T> IStructureElementDeferred<T> defer(Supplier<IStructureElement<T>> to) {
        if (to == null) {
            throw new IllegalArgumentException();
        }
        return new IStructureElementDeferred<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return to.get().check(t, world, x, y, z);
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                return to.get().placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                return to.get().spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(
                    T t,
                    World world,
                    int x,
                    int y,
                    int z,
                    ItemStack trigger,
                    IItemSource s,
                    EntityPlayerMP actor,
                    Consumer<IChatComponent> chatter) {
                return to.get().survivalPlaceBlock(t, world, x, y, z, trigger, s, actor, chatter);
            }
        };
    }

    public static <T> IStructureElementDeferred<T> defer(Function<T, IStructureElement<T>> to) {
        if (to == null) {
            throw new IllegalArgumentException();
        }
        return new IStructureElementDeferred<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return to.apply(t).check(t, world, x, y, z);
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                return to.apply(t).placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                return to.apply(t).spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(
                    T t,
                    World world,
                    int x,
                    int y,
                    int z,
                    ItemStack trigger,
                    IItemSource s,
                    EntityPlayerMP actor,
                    Consumer<IChatComponent> chatter) {
                return to.apply(t).survivalPlaceBlock(t, world, x, y, z, trigger, s, actor, chatter);
            }
        };
    }

    public static <T, K> IStructureElementDeferred<T> defer(
            Function<T, K> keyExtractor, Map<K, IStructureElement<T>> map) {
        if (keyExtractor == null || map == null) {
            throw new IllegalArgumentException();
        }
        return new IStructureElementDeferred<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return map.get(keyExtractor.apply(t)).check(t, world, x, y, z);
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                return map.get(keyExtractor.apply(t)).placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                return map.get(keyExtractor.apply(t)).spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(
                    T t,
                    World world,
                    int x,
                    int y,
                    int z,
                    ItemStack trigger,
                    IItemSource s,
                    EntityPlayerMP actor,
                    Consumer<IChatComponent> chatter) {
                return map.get(keyExtractor.apply(t)).survivalPlaceBlock(t, world, x, y, z, trigger, s, actor, chatter);
            }
        };
    }

    public static <T, K> IStructureElementDeferred<T> defer(
            Function<T, K> keyExtractor, Map<K, IStructureElement<T>> map, IStructureElement<T> defaultElem) {
        if (keyExtractor == null || map == null) {
            throw new IllegalArgumentException();
        }
        return new IStructureElementDeferred<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return map.getOrDefault(keyExtractor.apply(t), defaultElem).check(t, world, x, y, z);
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                return map.getOrDefault(keyExtractor.apply(t), defaultElem).placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                return map.getOrDefault(keyExtractor.apply(t), defaultElem).spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(
                    T t,
                    World world,
                    int x,
                    int y,
                    int z,
                    ItemStack trigger,
                    IItemSource s,
                    EntityPlayerMP actor,
                    Consumer<IChatComponent> chatter) {
                return map.getOrDefault(keyExtractor.apply(t), defaultElem)
                        .survivalPlaceBlock(t, world, x, y, z, trigger, s, actor, chatter);
            }
        };
    }

    @SafeVarargs
    public static <T> IStructureElementDeferred<T> defer(
            Function<T, Integer> keyExtractor, IStructureElement<T>... array) {
        if (keyExtractor == null || array == null) {
            throw new IllegalArgumentException();
        }
        return new IStructureElementDeferred<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return array[keyExtractor.apply(t)].check(t, world, x, y, z);
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                return array[keyExtractor.apply(t)].placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                return array[keyExtractor.apply(t)].spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(
                    T t,
                    World world,
                    int x,
                    int y,
                    int z,
                    ItemStack trigger,
                    IItemSource s,
                    EntityPlayerMP actor,
                    Consumer<IChatComponent> chatter) {
                return array[keyExtractor.apply(t)].survivalPlaceBlock(t, world, x, y, z, trigger, s, actor, chatter);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> IStructureElementDeferred<T> defer(
            Function<T, Integer> keyExtractor, List<IStructureElement<T>> array) {
        return defer(keyExtractor, array.toArray(new IStructureElement[0]));
    }

    public static <T> IStructureElementDeferred<T> defer(BiFunction<T, ItemStack, IStructureElement<T>> to) {
        if (to == null) {
            throw new IllegalArgumentException();
        }
        return new IStructureElementDeferred<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return to.apply(t, null).check(t, world, x, y, z);
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                return to.apply(t, trigger).placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                return to.apply(t, trigger).spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(
                    T t,
                    World world,
                    int x,
                    int y,
                    int z,
                    ItemStack trigger,
                    IItemSource s,
                    EntityPlayerMP actor,
                    Consumer<IChatComponent> chatter) {
                return to.apply(t, trigger).survivalPlaceBlock(t, world, x, y, z, trigger, s, actor, chatter);
            }
        };
    }

    public static <T, K> IStructureElementDeferred<T> defer(
            BiFunction<T, ItemStack, K> keyExtractor, Map<K, IStructureElement<T>> map) {
        if (keyExtractor == null || map == null) {
            throw new IllegalArgumentException();
        }
        return new IStructureElementDeferred<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return map.get(keyExtractor.apply(t, null)).check(t, world, x, y, z);
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                return map.get(keyExtractor.apply(t, trigger)).placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                return map.get(keyExtractor.apply(t, trigger)).spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(
                    T t,
                    World world,
                    int x,
                    int y,
                    int z,
                    ItemStack trigger,
                    IItemSource s,
                    EntityPlayerMP actor,
                    Consumer<IChatComponent> chatter) {
                return map.get(keyExtractor.apply(t, trigger))
                        .survivalPlaceBlock(t, world, x, y, z, trigger, s, actor, chatter);
            }
        };
    }

    public static <T, K> IStructureElementDeferred<T> defer(
            BiFunction<T, ItemStack, K> keyExtractor,
            Map<K, IStructureElement<T>> map,
            IStructureElement<T> defaultElem) {
        if (keyExtractor == null || map == null) {
            throw new IllegalArgumentException();
        }
        return new IStructureElementDeferred<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return map.getOrDefault(keyExtractor.apply(t, null), defaultElem)
                        .check(t, world, x, y, z);
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                return map.getOrDefault(keyExtractor.apply(t, trigger), defaultElem)
                        .placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                return map.getOrDefault(keyExtractor.apply(t, trigger), defaultElem)
                        .spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(
                    T t,
                    World world,
                    int x,
                    int y,
                    int z,
                    ItemStack trigger,
                    IItemSource s,
                    EntityPlayerMP actor,
                    Consumer<IChatComponent> chatter) {
                return map.getOrDefault(keyExtractor.apply(t, trigger), defaultElem)
                        .survivalPlaceBlock(t, world, x, y, z, trigger, s, actor, chatter);
            }
        };
    }

    @SafeVarargs
    public static <T> IStructureElementDeferred<T> defer(
            BiFunction<T, ItemStack, Integer> keyExtractor, IStructureElement<T>... array) {
        if (keyExtractor == null || array == null) {
            throw new IllegalArgumentException();
        }
        return new IStructureElementDeferred<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return array[keyExtractor.apply(t, null)].check(t, world, x, y, z);
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                return array[keyExtractor.apply(t, trigger)].placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                return array[keyExtractor.apply(t, trigger)].spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(
                    T t,
                    World world,
                    int x,
                    int y,
                    int z,
                    ItemStack trigger,
                    IItemSource s,
                    EntityPlayerMP actor,
                    Consumer<IChatComponent> chatter) {
                return array[keyExtractor.apply(t, trigger)].survivalPlaceBlock(
                        t, world, x, y, z, trigger, s, actor, chatter);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> IStructureElementDeferred<T> defer(
            BiFunction<T, ItemStack, Integer> keyExtractor, List<IStructureElement<T>> array) {
        return defer(keyExtractor, array.toArray(new IStructureElement[0]));
    }

    public static <T> IStructureElementDeferred<T> defer(
            Function<T, IStructureElement<T>> toCheck, BiFunction<T, ItemStack, IStructureElement<T>> to) {
        if (to == null) {
            throw new IllegalArgumentException();
        }
        return new IStructureElementDeferred<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return toCheck.apply(t).check(t, world, x, y, z);
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                return to.apply(t, trigger).placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                return to.apply(t, trigger).spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(
                    T t,
                    World world,
                    int x,
                    int y,
                    int z,
                    ItemStack trigger,
                    IItemSource s,
                    EntityPlayerMP actor,
                    Consumer<IChatComponent> chatter) {
                return to.apply(t, trigger).survivalPlaceBlock(t, world, x, y, z, trigger, s, actor, chatter);
            }
        };
    }

    public static <T, K> IStructureElementDeferred<T> defer(
            Function<T, K> keyExtractorCheck,
            BiFunction<T, ItemStack, K> keyExtractor,
            Map<K, IStructureElement<T>> map) {
        if (keyExtractor == null || map == null) {
            throw new IllegalArgumentException();
        }
        return new IStructureElementDeferred<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return map.get(keyExtractorCheck.apply(t)).check(t, world, x, y, z);
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                return map.get(keyExtractor.apply(t, trigger)).placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                return map.get(keyExtractor.apply(t, trigger)).spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(
                    T t,
                    World world,
                    int x,
                    int y,
                    int z,
                    ItemStack trigger,
                    IItemSource s,
                    EntityPlayerMP actor,
                    Consumer<IChatComponent> chatter) {
                return map.get(keyExtractor.apply(t, trigger))
                        .survivalPlaceBlock(t, world, x, y, z, trigger, s, actor, chatter);
            }
        };
    }

    public static <T, K> IStructureElementDeferred<T> defer(
            Function<T, K> keyExtractorCheck,
            BiFunction<T, ItemStack, K> keyExtractor,
            Map<K, IStructureElement<T>> map,
            IStructureElement<T> defaultElem) {
        if (keyExtractor == null || map == null) {
            throw new IllegalArgumentException();
        }
        return new IStructureElementDeferred<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return map.getOrDefault(keyExtractorCheck.apply(t), defaultElem).check(t, world, x, y, z);
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                return map.getOrDefault(keyExtractor.apply(t, trigger), defaultElem)
                        .placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                return map.getOrDefault(keyExtractor.apply(t, trigger), defaultElem)
                        .spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(
                    T t,
                    World world,
                    int x,
                    int y,
                    int z,
                    ItemStack trigger,
                    IItemSource s,
                    EntityPlayerMP actor,
                    Consumer<IChatComponent> chatter) {
                return map.getOrDefault(keyExtractor.apply(t, trigger), defaultElem)
                        .survivalPlaceBlock(t, world, x, y, z, trigger, s, actor, chatter);
            }
        };
    }

    @SafeVarargs
    public static <T> IStructureElementDeferred<T> defer(
            Function<T, Integer> keyExtractorCheck,
            BiFunction<T, ItemStack, Integer> keyExtractor,
            IStructureElement<T>... array) {
        if (keyExtractor == null || array == null) {
            throw new IllegalArgumentException();
        }
        return new IStructureElementDeferred<T>() {
            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return array[keyExtractorCheck.apply(t)].check(t, world, x, y, z);
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                return array[keyExtractor.apply(t, trigger)].placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                return array[keyExtractor.apply(t, trigger)].spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(
                    T t,
                    World world,
                    int x,
                    int y,
                    int z,
                    ItemStack trigger,
                    IItemSource s,
                    EntityPlayerMP actor,
                    Consumer<IChatComponent> chatter) {
                return array[keyExtractor.apply(t, trigger)].survivalPlaceBlock(
                        t, world, x, y, z, trigger, s, actor, chatter);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> IStructureElementDeferred<T> defer(
            Function<T, Integer> keyExtractorCheck,
            BiFunction<T, ItemStack, Integer> keyExtractor,
            List<IStructureElement<T>> array) {
        return defer(keyExtractorCheck, keyExtractor, array.toArray(new IStructureElement[0]));
    }

    // endregion

    /**
     * Used internally, to generate skips for structure definitions
     */
    public static <T> IStructureNavigate<T> step(int a, int b, int c) {
        return step(new Vec3Impl(a, b, c));
    }

    /**
     * Used internally, to generate skips for structure definitions
     */
    @SuppressWarnings("unchecked")
    public static <T> IStructureNavigate<T> step(Vec3Impl step) {
        if (step == null || step.get0() < 0 || step.get1() < 0 || step.get2() < 0) {
            throw new IllegalArgumentException();
        }
        return STEP.computeIfAbsent(step, vec3 -> {
            if (vec3.get2() > 0) {
                return stepC(vec3.get0(), vec3.get1(), vec3.get2());
            } else if (vec3.get1() > 0) {
                return stepB(vec3.get0(), vec3.get1(), vec3.get2());
            } else {
                return stepA(vec3.get0(), vec3.get1(), vec3.get2());
            }
        });
    }

    private static <T> IStructureNavigate<T> stepA(int a, int b, int c) {
        return new IStructureNavigate<T>() {
            @Override
            public int getStepA() {
                return a;
            }

            @Override
            public int getStepB() {
                return b;
            }

            @Override
            public int getStepC() {
                return c;
            }
        };
    }

    private static <T> IStructureNavigate<T> stepB(int a, int b, int c) {
        return new IStructureNavigate<T>() {
            @Override
            public int getStepA() {
                return a;
            }

            @Override
            public int getStepB() {
                return b;
            }

            @Override
            public int getStepC() {
                return c;
            }

            @Override
            public boolean resetA() {
                return true;
            }
        };
    }

    private static <T> IStructureNavigate<T> stepC(int a, int b, int c) {
        return new IStructureNavigate<T>() {
            @Override
            public int getStepA() {
                return a;
            }

            @Override
            public int getStepB() {
                return b;
            }

            @Override
            public int getStepC() {
                return c;
            }

            @Override
            public boolean resetA() {
                return true;
            }

            @Override
            public boolean resetB() {
                return true;
            }
        };
    }

    /**
     * Used only to get pseudo code in structure writer...
     * <p>
     * NOTE: GT specific code got removed. TODO add a mean
     *
     * @param tileEntityClassifier return a string that denote the type of a tile entity, or null if it's nothing special. useful if the tile entity cannot be simply distinguished via getClass.
     */
    public static String getPseudoJavaCode(
            World world,
            ExtendedFacing extendedFacing,
            int basePositionX,
            int basePositionY,
            int basePositionZ,
            int basePositionA,
            int basePositionB,
            int basePositionC,
            Function<? super TileEntity, String> tileEntityClassifier,
            int sizeA,
            int sizeB,
            int sizeC,
            boolean transpose) {
        Map<Block, Set<Integer>> blocks = new TreeMap<>(Comparator.comparing(Block::getUnlocalizedName));
        Set<Class<? extends TileEntity>> tiles = new HashSet<>();
        Set<String> specialTiles = new HashSet<>();
        iterate(
                world,
                extendedFacing,
                basePositionX,
                basePositionY,
                basePositionZ,
                basePositionA,
                basePositionB,
                basePositionC,
                sizeA,
                sizeB,
                sizeC,
                ((w, x, y, z) -> {
                    TileEntity tileEntity = w.getTileEntity(x, y, z);
                    if (tileEntity == null) {
                        Block block = w.getBlock(x, y, z);
                        if (block != null && block != Blocks.air) {
                            blocks.compute(block, (b, set) -> {
                                if (set == null) {
                                    set = new TreeSet<>();
                                }
                                set.add(block.getDamageValue(world, x, y, z));
                                return set;
                            });
                        }
                    } else {
                        String classification = tileEntityClassifier.apply(tileEntity);
                        if (classification == null) {
                            tiles.add(tileEntity.getClass());
                        } else specialTiles.add(classification);
                    }
                }));
        Map<String, Character> map = new HashMap<>();
        StringBuilder builder = new StringBuilder();
        {
            int i = 0;
            char c;
            builder.append("\n\nStructure:\n").append("\nBlocks:\n");
            for (Map.Entry<Block, Set<Integer>> entry : blocks.entrySet()) {
                Block block = entry.getKey();
                Set<Integer> set = entry.getValue();
                for (Integer meta : set) {
                    c = NICE_CHARS.charAt(i++);
                    if (i > NICE_CHARS.length()) {
                        return "Too complicated for nice chars";
                    }
                    map.put(block.getUnlocalizedName() + '\0' + meta, c);
                    builder.append(c)
                            .append(" -> ofBlock...(")
                            .append(block.getUnlocalizedName())
                            .append(", ")
                            .append(meta)
                            .append(", ...);\n");
                }
            }
            builder.append("\nTiles:\n");
            for (Class<? extends TileEntity> tile : tiles) {
                c = NICE_CHARS.charAt(i++);
                if (i > NICE_CHARS.length()) {
                    return "Too complicated for nice chars";
                }
                map.put(tile.getCanonicalName(), c);
                builder.append(c).append(" -> ofTileAdder(").append(tile).append(", ...);\n");
            }
            builder.append("\nSpecial Tiles:\n");
            for (String tile : specialTiles) {
                c = NICE_CHARS.charAt(i++);
                if (i > NICE_CHARS.length()) {
                    return "Too complicated for nice chars";
                }
                map.put(tile, c);
                builder.append(c)
                        .append(" -> ofSpecialTileAdder(")
                        .append(tile)
                        .append(", ...); // You will probably want to change it to something else\n");
            }
        }
        builder.append("\nOffsets:\n")
                .append(basePositionA)
                .append(' ')
                .append(basePositionB)
                .append(' ')
                .append(basePositionC)
                .append('\n');
        if (transpose) {
            builder.append("\nTransposed Scan:\n").append("new String[][]{\n").append("    {\"");
            iterate(
                    world,
                    extendedFacing,
                    basePositionX,
                    basePositionY,
                    basePositionZ,
                    basePositionA,
                    basePositionB,
                    basePositionC,
                    true,
                    sizeA,
                    sizeB,
                    sizeC,
                    ((w, x, y, z) -> {
                        TileEntity tileEntity = w.getTileEntity(x, y, z);
                        if (tileEntity == null) {
                            Block block = w.getBlock(x, y, z);
                            if (block != null && block != Blocks.air) {
                                builder.append(map.get(
                                        block.getUnlocalizedName() + '\0' + block.getDamageValue(world, x, y, z)));
                            } else {
                                builder.append(' ');
                            }
                        } else {
                            String classification = tileEntityClassifier.apply(tileEntity);
                            if (classification == null) {
                                classification = tileEntity.getClass().getCanonicalName();
                            }
                            builder.append(map.get(classification));
                        }
                    }),
                    () -> builder.append("\",\""),
                    () -> {
                        builder.setLength(builder.length() - 2);
                        builder.append("},\n    {\"");
                    });
            builder.setLength(builder.length() - 8);
            builder.append("\n}\n\n");
        } else {
            builder.append("\nNormal Scan:\n").append("new String[][]{{\n").append("    \"");
            iterate(
                    world,
                    extendedFacing,
                    basePositionX,
                    basePositionY,
                    basePositionZ,
                    basePositionA,
                    basePositionB,
                    basePositionC,
                    false,
                    sizeA,
                    sizeB,
                    sizeC,
                    ((w, x, y, z) -> {
                        TileEntity tileEntity = w.getTileEntity(x, y, z);
                        if (tileEntity == null) {
                            Block block = w.getBlock(x, y, z);
                            if (block != null && block != Blocks.air) {
                                builder.append(map.get(
                                        block.getUnlocalizedName() + '\0' + block.getDamageValue(world, x, y, z)));
                            } else {
                                builder.append(' ');
                            }
                        } else {
                            String classification = tileEntityClassifier.apply(tileEntity);
                            if (classification == null) {
                                classification = tileEntity.getClass().getCanonicalName();
                            }
                            builder.append(map.get(classification));
                        }
                    }),
                    () -> builder.append("\",\n").append("    \""),
                    () -> {
                        builder.setLength(builder.length() - 7);
                        builder.append("\n").append("},{\n").append("    \"");
                    });
            builder.setLength(builder.length() - 8);
            builder.append("}\n\n");
        }
        return (builder.toString().replaceAll("\"\"", "E"));
    }

    static <T> boolean iterateV2(
            IStructureElement<T>[] elements,
            World world,
            ExtendedFacing extendedFacing,
            int basePositionX,
            int basePositionY,
            int basePositionZ,
            int basePositionA,
            int basePositionB,
            int basePositionC,
            IStructureWalker<T> predicate,
            String iterateType) {
        // change base position to base offset
        basePositionA = -basePositionA;
        basePositionB = -basePositionB;
        basePositionC = -basePositionC;

        int[] abc = new int[] {basePositionA, basePositionB, basePositionC};
        int[] xyz = new int[3];

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

                if (DEBUG_MODE)
                    StructureLib.LOGGER.info(
                            "Multi [{}, {}, {}] {} step @ {} {}",
                            basePositionX,
                            basePositionY,
                            basePositionZ,
                            iterateType,
                            Arrays.toString(xyz),
                            Arrays.toString(abc));

                if (world.blockExists(xyz[0], xyz[1], xyz[2])) {
                    if (!predicate.visit(element, world, xyz[0], xyz[1], xyz[2])) {
                        if (DEBUG_MODE) {
                            StructureLib.LOGGER.info(
                                    "Multi [{}, {}, {}] {} stop @ {} {}",
                                    basePositionX,
                                    basePositionY,
                                    basePositionZ,
                                    iterateType,
                                    Arrays.toString(xyz),
                                    Arrays.toString(abc));
                        }
                        return false;
                    }
                } else {
                    if (DEBUG_MODE) {
                        StructureLib.LOGGER.info(
                                "Multi [{}, {}, {}] {} !blockExists @ {} {}",
                                basePositionX,
                                basePositionY,
                                basePositionZ,
                                iterateType,
                                Arrays.toString(xyz),
                                Arrays.toString(abc));
                    }
                    return false;
                }
                abc[0] += 1;
            }
        }
        return true;
    }

    public static void iterate(
            World world,
            ExtendedFacing extendedFacing,
            int basePositionX,
            int basePositionY,
            int basePositionZ,
            int basePositionA,
            int basePositionB,
            int basePositionC,
            int sizeA,
            int sizeB,
            int sizeC,
            IBlockPosConsumer iBlockPosConsumer) {
        sizeA -= basePositionA;
        sizeB -= basePositionB;
        sizeC -= basePositionC;

        int[] abc = new int[3];
        int[] xyz = new int[3];

        for (abc[2] = -basePositionC; abc[2] < sizeC; abc[2]++) {
            for (abc[1] = -basePositionB; abc[1] < sizeB; abc[1]++) {
                for (abc[0] = -basePositionA; abc[0] < sizeA; abc[0]++) {
                    extendedFacing.getWorldOffset(abc, xyz);
                    iBlockPosConsumer.consume(
                            world, xyz[0] + basePositionX, xyz[1] + basePositionY, xyz[2] + basePositionZ);
                }
            }
        }
    }

    public static void iterate(
            World world,
            ExtendedFacing extendedFacing,
            int basePositionX,
            int basePositionY,
            int basePositionZ,
            int basePositionA,
            int basePositionB,
            int basePositionC,
            boolean transpose,
            int sizeA,
            int sizeB,
            int sizeC,
            IBlockPosConsumer iBlockPosConsumer,
            Runnable nextB,
            Runnable nextC) {
        sizeA -= basePositionA;
        sizeB -= basePositionB;
        sizeC -= basePositionC;

        int[] abc = new int[3];
        int[] xyz = new int[3];
        if (transpose) {
            for (abc[1] = -basePositionB; abc[1] < sizeB; abc[1]++) {
                for (abc[2] = -basePositionC; abc[2] < sizeC; abc[2]++) {
                    for (abc[0] = -basePositionA; abc[0] < sizeA; abc[0]++) {
                        extendedFacing.getWorldOffset(abc, xyz);
                        iBlockPosConsumer.consume(
                                world, xyz[0] + basePositionX, xyz[1] + basePositionY, xyz[2] + basePositionZ);
                    }
                    nextB.run();
                }
                nextC.run();
            }
        } else {
            for (abc[2] = -basePositionC; abc[2] < sizeC; abc[2]++) {
                for (abc[1] = -basePositionB; abc[1] < sizeB; abc[1]++) {
                    for (abc[0] = -basePositionA; abc[0] < sizeA; abc[0]++) {
                        extendedFacing.getWorldOffset(abc, xyz);
                        iBlockPosConsumer.consume(
                                world, xyz[0] + basePositionX, xyz[1] + basePositionY, xyz[2] + basePositionZ);
                    }
                    nextB.run();
                }
                nextC.run();
            }
        }
    }

    /**
     * Transposes shape (swaps B and C axis, can be used to un-transpose transposed shape)
     * WARNING! Do not use on old api...
     *
     * @param structurePiece shape (transposed shape)
     * @return transposed shape (untransposed shape)
     */
    public static String[][] transpose(String[][] structurePiece) {
        String[][] shape = new String[structurePiece[0].length][structurePiece.length];
        for (int i = 0; i < structurePiece.length; i++) {
            for (int j = 0; j < structurePiece[i].length; j++) {
                shape[j][i] = structurePiece[i][j];
            }
        }
        return shape;
    }
}
