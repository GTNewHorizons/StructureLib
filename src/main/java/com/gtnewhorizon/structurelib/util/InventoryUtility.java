/**
 * Copyright (c) 2022, glee8e
 * This file is part of StructureLib.
 * <p>
 * StructureLib is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * StructureLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with Foobar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.gtnewhorizon.structurelib.util;

import com.gtnewhorizon.structurelib.util.ItemStackPredicate.NBTMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * This class is part of API, but is not stable. Use at your own risk.
 */
public class InventoryUtility {
    private static final SortedRegistry<ItemStackExtractor<?>> stackExtractors = new SortedRegistry<>();
    /**
     * The remove() of the Iterable returned must be implemented!
     */
    private static final SortedRegistry<InventoryProvider<?>> inventoryProviders = new SortedRegistry<>();

    static {
        inventoryProviders.register("5000-main-inventory", new InventoryProvider<InventoryIterable<InventoryPlayer>>() {
            @Override
            public InventoryIterable<InventoryPlayer> getInventory(EntityPlayerMP player) {
                return new InventoryIterable<>(player.inventory, player.inventory.mainInventory.length);
            }

            @Override
            public void markDirty(InventoryIterable<InventoryPlayer> inv) {
                inv.getInventory().markDirty();
            }
        });
    }

    public static void registerStackExtractor(String key, ItemStackExtractor<?> val) {
        stackExtractors.register(key, val);
    }

    public static <Inv extends IInventory> void registerStackExtractor(
            String key, Function<ItemStack, ? extends Inv> extractor) {
        registerStackExtractor(key, newItemStackProvider(extractor));
    }

    public static void registerInventoryProvider(String key, InventoryProvider<?> val) {
        inventoryProviders.register(key, val);
    }

    public static <Inv extends IInventory> void registerInventoryProvider(
            String key, Function<EntityPlayerMP, ? extends Inv> extractor) {
        registerInventoryProvider(key, newInventoryProvider(extractor));
    }

    public static <Inv extends IInventory> InventoryProvider<InventoryIterable<Inv>> newInventoryProvider(
        Function<EntityPlayerMP, ? extends Inv> extractor) {
        return new InventoryProvider<InventoryIterable<Inv>>() {
            @Override
            public InventoryIterable<Inv> getInventory(EntityPlayerMP player) {
                Inv inv = extractor.apply(player);
                return inv != null ? new InventoryIterable<>(inv) : null;
            }

            @Override
            public void markDirty(InventoryIterable<Inv> inv) {
                inv.getInventory().markDirty();
            }
        };
    }

    public static <Inv extends IInventory> ItemStackExtractor<InventoryIterable<Inv>> newItemStackProvider(
        Function<ItemStack, ? extends Inv> extractor) {
        return new ItemStackExtractor<InventoryIterable<Inv>>() {
            @Override
            public InventoryIterable<Inv> getInventory(ItemStack source) {
                Inv inv = extractor.apply(source);
                return inv != null ? new InventoryIterable<>(inv) : null;
            }

            @Override
            public void markDirty(InventoryIterable<Inv> inv) {
                inv.getInventory().markDirty();
            }
        };
    }

    /**
     * take count amount of stacks that matches given filter. might take partial amount of stuff, so simulation is suggested
     * if you ever need to take more than one.
     *
     * @param player    source of stacks
     * @param predicate item stack filter
     * @param simulate  whether to do removal
     * @param count     let's hope int size is enough...
     * @return amount taken. never negative nor bigger than count...
     */
    public static Map<ItemStack, Integer> takeFromInventory(
            EntityPlayerMP player, Predicate<ItemStack> predicate, boolean simulate, int count) {
        Map<ItemStack, Integer> store = new ItemStackMap<>();
        int sum = 0;
        for (InventoryProvider<?> provider : inventoryProviders) {
            sum += takeFromPlayer(player, predicate, simulate, count - sum, store, provider, null);
            if (sum >= count) return store;
        }
        return store;
    }

    /**
     * take count amount of stacks that matches given filter. might take partial amount of stuff, so simulation is suggested
     * if you ever need to take more than one.
     *
     * @param player   source of stacks
     * @param filter   the precise type of item to extract. stackSize matters
     * @param simulate whether to do removal
     * @return amount taken. never negative nor bigger than count...
     */
    public static int takeFromInventory(EntityPlayerMP player, ItemStack filter, boolean simulate) {
        int sum = 0;
        int count = filter.stackSize;
        ItemStackPredicate predicate = ItemStackPredicate.from(filter, NBTMode.EXACT);
        HashMap<ItemStack, Integer> store = new HashMap<>();
        for (InventoryProvider<?> provider : inventoryProviders) {
            sum += takeFromPlayer(player, predicate, simulate, count - sum, store, provider, filter);
            if (sum >= count) return sum;
        }
        return sum;
    }

    // workaround java generics issue
    private static <R extends Iterable<ItemStack>> int takeFromPlayer(
            EntityPlayerMP player,
            Predicate<ItemStack> predicate,
            boolean simulate,
            int count,
            Map<ItemStack, Integer> store,
            InventoryProvider<R> provider,
            ItemStack filter) {
        R inv = provider.getInventory(player);
        if (inv == null) return 0;
        int taken = takeFromInventory(inv, predicate, simulate, count, true, store, filter);
        if (taken > 0) provider.markDirty(inv);
        return taken;
    }

    /**
     * take count amount of stacks that matches given filter. Will do 1 level of recursion to try find more stacks....
     * e.g. from backpacks...
     *
     * @param inv       source of stacks
     * @param predicate item stack filter
     * @param simulate  whether to do removal
     * @param count     let's hope int size is enough...
     * @return amount taken. never negative nor bigger than count...
     */
    public static Map<ItemStack, Integer> takeFromInventory(
            Iterable<ItemStack> inv, Predicate<ItemStack> predicate, boolean simulate, int count) {
        Map<ItemStack, Integer> store = new ItemStackMap<>();
        takeFromInventory(inv, predicate, simulate, count, true, store, null);
        return store;
    }

    private static int takeFromInventory(
            @Nonnull Iterable<ItemStack> inv,
            @Nonnull Predicate<ItemStack> predicate,
            boolean simulate,
            int count,
            boolean recurse,
            @Nonnull Map<ItemStack, Integer> store,
            @Nullable ItemStack filter) {
        int found = 0;
        ItemStack copiedFilter = null;
        if (filter != null) {
            copiedFilter = new ItemStack(filter.getItem(), filter.stackSize, Items.feather.getDamage(filter));
            copiedFilter.setTagCompound(filter.stackTagCompound);
        }
        for (Iterator<ItemStack> iterator = inv.iterator(); iterator.hasNext(); ) {
            ItemStack stack = iterator.next();
            // invalid stack
            if (stack == null || stack.getItem() == null || stack.stackSize <= 0) continue;

            if (predicate.test(stack)) {
                found += stack.stackSize;
                if (found > count) {
                    int surplus = found - count;
                    store.merge(stack, stack.stackSize - surplus, Integer::sum);
                    if (!simulate) {
                        // leave the surplus in its place
                        stack.stackSize = surplus;
                    }
                    return count;
                }
                store.merge(stack, stack.stackSize, Integer::sum);
                if (!simulate) iterator.remove();
                if (found == count) return count;
            }
            if (!recurse) continue;
            for (ItemStackExtractor<?> f : stackExtractors) {
                int taken = -1;
                if (filter != null) {
                    copiedFilter.stackSize = count - found;
                    taken = f.getItem(stack, copiedFilter, simulate);
                }
                if (taken == -1) {
                    taken = takeFromStack(predicate, simulate, count - found, store, stack, f, filter);
                    found += taken;
                }
                if (found >= count) return found;
            }
        }
        return found;
    }

    private static <R extends Iterable<ItemStack>> int takeFromStack(
            Predicate<ItemStack> predicate,
            boolean simulate,
            int count,
            Map<ItemStack, Integer> store,
            ItemStack stack,
            ItemStackExtractor<R> f,
            ItemStack filter) {
        R inv = f.getInventory(stack);
        if (inv == null) return 0;
        int taken = takeFromInventory(inv, predicate, simulate, count, false, store, filter);
        if (taken > 0) f.markDirty(inv);
        return taken;
    }

    public interface OptimizedExtractor {
        /**
         * Extract a particular type of item. The extractor can choose to not return all items contained within this item
         * as long as it makes sense, but the author should inform the player of this.
         * <p>
         * Whether optimized extractor do recursive extraction is at the discretion of implementor.
         *
         * @param source    from where an extraction should be attempted
         * @param toExtract stack to extract. should not be mutated! match the NBT tag using EXACT mode.
         * @param simulate  true if only query but does not actually remove
         * @return amount extracted
         */
        int extract(ItemStack source, ItemStack toExtract, boolean simulate);
    }

    public interface InventoryProvider<R extends Iterable<ItemStack>> {
        R getInventory(EntityPlayerMP player);

        void markDirty(R inv);
    }

    public interface ItemStackExtractor<R extends Iterable<ItemStack>> {
        /**
         * Extract an inventory from given item stack.
         */
        R getInventory(ItemStack source);

        /**
         * Callback for the inventory is modified.
         */
        void markDirty(R inv);

        /**
         * Extract a particular type of item. The extractor can choose to not return all items contained within this item
         * as long as it makes sense, but the author should inform the player of this.
         * <p>
         * Whether this method does recursive extraction is at the discretion of implementor.
         * <p>
         * This method is optional. Return -1 to signal it does not support this operation.
         *
         * @param source    from where an extraction should be attempted
         * @param toExtract stack to extract. should not be mutated! match the NBT tag using EXACT mode.
         * @param simulate  true if only query but does not actually remove
         * @return amount extracted, or -1 if this is not implemented
         */
        default int getItem(ItemStack source, ItemStack toExtract, boolean simulate) {
            return -1;
        }

        static ItemStackExtractor<Iterable<ItemStack>> createOnlyOptimized(
                @Nonnull OptimizedExtractor optimizedExtractor) {
            return new ItemStackExtractor<Iterable<ItemStack>>() {
                @Override
                public Iterable<ItemStack> getInventory(ItemStack source) {
                    return Collections.emptyList();
                }

                @Override
                public void markDirty(Iterable<ItemStack> inv) {
                }

                @Override
                public int getItem(ItemStack source, ItemStack toExtract, boolean simulate) {
                    return optimizedExtractor.extract(source, toExtract, simulate);
                }
            };
        }
    }
}
