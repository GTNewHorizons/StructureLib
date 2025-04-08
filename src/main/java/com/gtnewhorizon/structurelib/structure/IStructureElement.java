package com.gtnewhorizon.structurelib.structure;

import static com.gtnewhorizon.structurelib.StructureLib.LOGGER;
import static com.gtnewhorizon.structurelib.StructureLib.PANIC_MODE;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.item.ItemConstructableTrigger;
import com.gtnewhorizon.structurelib.util.ItemStackPredicate;
import com.gtnewhorizon.structurelib.util.ItemStackPredicate.NBTMode;

/**
 * Use StructureUtility to instantiate. These are the building blocks for your {@link IStructureDefinition}. It
 * represents what a particular element can be and how this particular element should be autoplaced.
 */
public interface IStructureElement<T> {

    boolean check(T t, World world, int x, int y, int z);

    /**
     * Pure (stateless and side effect free) function to test if current block could be valid. Used to give user hints
     * about incorrectly placed blocks given a controller and trigger item. If couldBeValid(...) == false for a set of
     * channels, with those same tiers, check(...) == false. Not required, defaults to true which is always safe but
     * won't give the user error hints.
     */
    default boolean couldBeValid(T t, World world, int x, int y, int z, ItemStack trigger) {
        return true;
    }

    boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger);

    boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger);

    /**
     * Try place the block by taking resource from given IItemSource.
     * <p>
     * You might want to use
     * {@link StructureUtility#survivalPlaceBlock(Block, int, World, int, int, int, IItemSource, net.minecraft.entity.player.EntityPlayer)}
     * or its overloads to implement this.
     *
     * @param trigger trigger item
     * @param s       drain resources from this place
     * @param actor   source of action. for very critical errors you can also just send the error messages here,
     *                bypassing any filter that chatter might have.
     * @param chatter send error messages here. Caller will choose an appropriate way to forward it to player if the
     *                other fallbacks also fails.
     * @deprecated caller should call the non deprecated overload. implementor for reusable {@link IStructureElement}
     *             should still implement this. implementor for private implementations can freely ignore this as long
     *             as you know the caller will not call this overload.
     */
    @Deprecated
    default PlaceResult survivalPlaceBlock(T t, World world, int x, int y, int z, ItemStack trigger, IItemSource s,
            EntityPlayerMP actor, Consumer<IChatComponent> chatter) {
        if (PANIC_MODE) throw new RuntimeException("Panic Tripwire hit");
        if (StructureLibAPI.isDebugEnabled()) LOGGER.error(
                "Default implementation of survivalPlaceBlock hit! Things aren't going to work well! IStructureElement class: {}",
                getClass().getName());
        if (!StructureLibAPI.isBlockTriviallyReplaceable(world, x, y, z, actor)) return PlaceResult.REJECT;
        return PlaceResult.SKIP;
    }

    /**
     * A simplified version of survivalPlaceBlock. Return null if not implemented.
     * <p>
     * <b>BY OVERRIDING THIS TO NULL, YOU ACKNOWLEDGE THAT THE {@link #check(Object, World, int, int, int)} OF THIS
     * CLASS IS SIDE EFFECT FREE AND CAN HAVE ITS CHECK CALLED WITHOUT BREAKING ANYTHING. OTHERWISE OVERRIDE THE
     * {@link #survivalPlaceBlock(Object, World, int, int, int, ItemStack, AutoPlaceEnvironment)} TO PROVIDE A
     * SIDEEFFECT FREE {@link PlaceResult#SKIP}.</b>
     * <p>
     * It should be noticed that this filter is only advisory and the actual
     * {@link #survivalPlaceBlock(Object, World, int, int, int, ItemStack, AutoPlaceEnvironment)} or
     * {@link #check(Object, World, int, int, int)} are free to reject/accept/place blocks not contained in this list,
     * e.g. when the element can be placed anywhere in the structure, but no more than 3 overall; when the structure
     * element cannot know all blocks it will accept at the time of calling; when this is backed by a legacy
     * {@link IStructureElement} that just didn't implement this API.
     *
     * @param trigger trigger item
     * @param env     autoplacing environment
     * @return null if not implemented, otherwise an instance describing what kind of blocks will be placed by
     *         {@link #survivalPlaceBlock(Object, World, int, int, int, ItemStack, AutoPlaceEnvironment)}
     */
    @Nullable
    default BlocksToPlace getBlocksToPlace(T t, World world, int x, int y, int z, ItemStack trigger,
            AutoPlaceEnvironment env) {
        return null;
    }

    /**
     * Try place the block by taking resource from given {@link IItemSource}.
     * <p>
     * You might want to use
     * {@link StructureUtility#survivalPlaceBlock(Block, int, World, int, int, int, IItemSource, net.minecraft.entity.player.EntityPlayer)}
     * or its overloads to implement this.
     * <p>
     * The default implementation provided will
     * <ol>
     * <li>Get {@link BlocksToPlace} via
     * {@link #getBlocksToPlace(Object, World, int, int, int, ItemStack, AutoPlaceEnvironment)}. If not null
     * <ol>
     * <li>call {@link #check(Object, World, int, int, int)}. If this returns {@code true}, {@link PlaceResult#SKIP}
     * will be returned without further action.</li>
     * <li>Use the {@link BlocksToPlace} retrieved earlier and passed in {@link IItemSource} to determine an item to
     * place</li>
     * <li>Hand over control to
     * {@link StructureUtility#survivalPlaceBlock(ItemStack, NBTMode, NBTTagCompound, boolean, World, int, int, int, IItemSource, EntityPlayer, Consumer)}</li>
     * </ol>
     * </li>
     * <li>Call legacy
     * {@link #survivalPlaceBlock(Object, World, int, int, int, ItemStack, IItemSource, EntityPlayerMP, Consumer)} if
     * {@code env.getActor()} contains an {@link EntityPlayerMP}</li>
     * <li>Emit warning under debug mode (or throw exception under panic mode), then return
     * {@link PlaceResult#SKIP}</li>
     * </ol>
     * It should be noticed that the default implementation is likely to cause unexpected issues if the
     * {@link #check(Object, World, int, int, int)} is not side effect free, or is not idempotent. E.g. the
     * {@link #check(Object, World, int, int, int)} will add current coord to a {@link java.util.List}. As this method
     * might be invoked at a location that is already accepted, this will cause the machine to have multiple of this
     * coord in its list.
     *
     * @param trigger trigger item
     * @param env     autoplacing environment
     */
    default PlaceResult survivalPlaceBlock(T t, World world, int x, int y, int z, ItemStack trigger,
            AutoPlaceEnvironment env) {
        int mode = ItemConstructableTrigger.getMode(trigger);
        BlocksToPlace e = getBlocksToPlace(t, world, x, y, z, trigger, env);
        IItemSource source = env.getSource();
        EntityPlayer actor = env.getActor();
        Consumer<IChatComponent> chatter = env.getChatter();
        if (e != null) {
            if (mode < 2 && check(t, world, x, y, z)) return PlaceResult.SKIP;
            if (e.getStacks() == null) {
                ItemStack taken = source.takeOne(e.getPredicate(), true);
                return StructureUtility.survivalPlaceBlock(
                        taken,
                        NBTMode.EXACT,
                        taken.stackTagCompound,
                        true,
                        world,
                        x,
                        y,
                        z,
                        source,
                        actor,
                        chatter);
            }
            for (ItemStack stack : e.getStacks()) {
                if (mode < 2 && !source.takeOne(stack, true)) continue;
                return StructureUtility.survivalPlaceBlock(
                        stack,
                        NBTMode.EXACT,
                        stack.stackTagCompound,
                        true,
                        world,
                        x,
                        y,
                        z,
                        source,
                        actor,
                        chatter);
            }
            return PlaceResult.REJECT;
        }
        if (actor instanceof EntityPlayerMP)
            return survivalPlaceBlock(t, world, x, y, z, trigger, source, (EntityPlayerMP) actor, chatter);
        if (PANIC_MODE) throw new RuntimeException("Panic Tripwire hit");
        if (StructureLibAPI.isDebugEnabled()) LOGGER.info(
                "Fallback shim code of survivalPlaceBlock hit! Things aren't going to work well! IStructureElement class: {}",
                getClass().getName());
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
            public boolean couldBeValid(T t, World world, int x, int y, int z, ItemStack trigger) {
                return IStructureElement.this.couldBeValid(t, world, x, y, z, trigger);
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
         * This element either exists already, or does not yet have an implementation for survivalPlaceBlock, or some
         * other unforeseen situations. TODO this definition doesn't seem right. Should we separate SKIP from EXISTS, or
         * SKIP from ERROR?
         */
        SKIP,
        /**
         * This element's space is occupied by other stuff and require player attention, or player missing required
         * resource, or the block cannot be placed due to obscure mechanisms, or some other unforeseen situations.
         */
        REJECT,
        /**
         * Autoplace cannot proceed within this tick. To proceed further would require stopping the placement and wait
         * for next round.
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

    final class BlocksToPlace {

        public static final BlocksToPlace errored = createEmpty();
        private final Predicate<ItemStack> predicate;
        private final Iterable<ItemStack> stacks;

        public static BlocksToPlace createEmpty() {
            return new BlocksToPlace(s -> false, Collections.emptyList());
        }

        public static BlocksToPlace create(ItemStack... stacks) {
            return create(Arrays.asList(stacks));
        }

        public static BlocksToPlace create(Iterable<ItemStack> stacks) {
            Predicate<ItemStack> predicate = null;

            for (ItemStack stack : stacks) {
                ItemStackPredicate p = ItemStackPredicate.from(stack, NBTMode.EXACT);
                if (predicate == null) predicate = p;
                else predicate = predicate.or(p);
            }

            return new BlocksToPlace(predicate, stacks);
        }

        public static BlocksToPlace create(Block block, int meta) {
            Item itemBlock = Item.getItemFromBlock(block);
            if (itemBlock instanceof ISpecialItemBlock) {
                meta = ((ISpecialItemBlock) itemBlock).getItemMetaFromBlockMeta(block, meta);
            }
            return create(itemBlock, meta);
        }

        public static BlocksToPlace create(Item item, int meta) {
            return new BlocksToPlace(
                    ItemStackPredicate.from(item).setMeta(meta),
                    Collections.singletonList(new ItemStack(item, 1, meta)));
        }

        public static BlocksToPlace create(ItemStack itemStack) {
            return new BlocksToPlace(ItemStackPredicate.from(itemStack), Collections.singletonList(itemStack));
        }

        public static BlocksToPlace create(Predicate<ItemStack> predicate) {
            return new BlocksToPlace(predicate, null);
        }

        BlocksToPlace(Predicate<ItemStack> predicate, Iterable<ItemStack> stacks) {
            this.predicate = predicate;
            this.stacks = stacks;
        }

        /**
         * Get the blocks to place filter. This is usually slower as it requires walking through inventories one by one.
         * <p>
         * Suitable for use with {@link IItemSource#takeOne(Predicate, boolean)}
         *
         * @return a predicate. never null.
         */
        @Nonnull
        public Predicate<ItemStack> getPredicate() {
            return predicate;
        }

        /**
         * Get the ItemStacks that this one is known to accept. Can be null if this info is not available at the time
         * this instance is constructed.
         * <p>
         * Suitable for use with {@link IItemSource#takeOne(ItemStack, boolean)}.
         * <p>
         * This should be equivalent to the predicate returned from {@link #getPredicate()}
         */
        @Nullable
        public Iterable<ItemStack> getStacks() {
            return stacks;
        }
    }
}
