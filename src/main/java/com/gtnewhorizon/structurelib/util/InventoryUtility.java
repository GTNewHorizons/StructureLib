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
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/**
 * This class is part of API, but is not stable. Use at your own risk.
 */
public class InventoryUtility {
    private static final SortedRegistry<ItemStackExtractor> stackExtractors = new SortedRegistry<>();
    /**
     * The remove() of the Iterable returned must be implemented!
     */
    private static final SortedRegistry<Function<EntityPlayerMP, Iterable<ItemStack>>> inventoryProviders =
            new SortedRegistry<>();

    static {
        inventoryProviders.register(
                "5000-main-inventory", player -> new ItemStackArrayIterable(player.inventory.mainInventory));
    }

    public static void registerStackExtractor(String key, ItemStackExtractor val) {
        stackExtractors.register(key, val);
    }

    public static void registerInventoryProvider(String key, Function<EntityPlayerMP, Iterable<ItemStack>> val) {
        inventoryProviders.register(key, val);
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
        for (Function<EntityPlayerMP, Iterable<ItemStack>> provider : inventoryProviders) {
            sum += takeFromInventory(provider.apply(player), predicate, simulate, count - sum, true, store, null);
            if (sum >= count) return store;
        }
        return store;
    }

    /**
     * take count amount of stacks that matches given filter. might take partial amount of stuff, so simulation is suggested
     * if you ever need to take more than one.
     *
     * @param player    source of stacks
     * @param filter    the precise type of item to extract. stackSize matters
     * @param simulate  whether to do removal
     * @return amount taken. never negative nor bigger than count...
     */
    public static int takeFromInventory(EntityPlayerMP player, ItemStack filter, boolean simulate) {
        int sum = 0;
        int count = filter.stackSize;
        ItemStackPredicate predicate = ItemStackPredicate.from(filter, NBTMode.EXACT);
        HashMap<ItemStack, Integer> store = new HashMap<>();
        for (Function<EntityPlayerMP, Iterable<ItemStack>> provider : inventoryProviders) {
            sum += takeFromInventory(provider.apply(player), predicate, simulate, count - sum, true, store, filter);
            if (sum >= count) return sum;
        }
        return sum;
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
            for (ItemStackExtractor f : stackExtractors) {
                if (f.getOptimizedExtractor() != null && filter != null) {
                    copiedFilter.stackSize = count - found;
                    found += f.getOptimizedExtractor().extract(stack, copiedFilter, simulate);
                } else {
                    Iterable<ItemStack> stacks = f.getPrimaryExtractor().apply(stack);
                    if (stacks == null) continue;
                    found += takeFromInventory(inv, predicate, simulate, count - found, false, store, null);
                }
                if (found > count) return found;
            }
        }
        return found;
    }

    public interface OptimizedExtractor {
        /**
         * Extract a particular type of item. The extractor can choose to not return all items contained within this item
         * as long as it makes sense, but the author should inform the player of this.
         *
         * Whether optimized extractor do recursive extraction is at the discretion of implementor.
         * @param source from where an extraction should be attempted
         * @param toExtract stack to extract. should not be mutated! match the NBT tag using EXACT mode.
         * @param simulate true if only query but does not actually remove
         * @return amount extracted
         */
        int extract(ItemStack source, ItemStack toExtract, boolean simulate);
    }

    public static final class ItemStackExtractor {
        private final Function<ItemStack, Iterable<ItemStack>> primaryExtractor;
        private final OptimizedExtractor optimizedExtractor;

        public static ItemStackExtractor createOnlyOptimized(@Nonnull OptimizedExtractor optimizedExtractor) {
            return new ItemStackExtractor(s -> Collections.emptyList(), optimizedExtractor);
        }

        public static ItemStackExtractor create(@Nonnull Function<ItemStack, Iterable<ItemStack>> primaryExtractor) {
            return new ItemStackExtractor(primaryExtractor, null);
        }

        public static ItemStackExtractor create(
                @Nonnull Function<ItemStack, Iterable<ItemStack>> primaryExtractor,
                @Nullable OptimizedExtractor optimizedExtractor) {
            return new ItemStackExtractor(primaryExtractor, optimizedExtractor);
        }

        private ItemStackExtractor(
                @Nonnull Function<ItemStack, Iterable<ItemStack>> primaryExtractor,
                @Nullable OptimizedExtractor optimizedExtractor) {
            this.primaryExtractor = primaryExtractor;
            this.optimizedExtractor = optimizedExtractor;
        }

        /**
         * Get the primary extractor. The primary extractor simply list all available ItemStacks.
         */
        @Nonnull
        public Function<ItemStack, Iterable<ItemStack>> getPrimaryExtractor() {
            return primaryExtractor;
        }

        /**
         * Get the optimized extractor. The optimized extractor extract stacks by a given item stack instead of a generic
         * predicate.
         */
        @Nullable
        public OptimizedExtractor getOptimizedExtractor() {
            return optimizedExtractor;
        }
    }
}
