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

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;

public class InventoryUtility {
    private static final SortedRegistry<Function<ItemStack, Iterable<ItemStack>>> stackExtractors = new SortedRegistry<>();
    /**
     * The remove() of the Iterable returned must be implemented!
     */
    private static final SortedRegistry<Function<EntityPlayerMP, Iterable<ItemStack>>> inventoryProviders = new SortedRegistry<>();
    private static final NavigableMap<String, Function<EntityPlayerMP, Iterable<ItemStack>>> inventoryProviderRegistry = new TreeMap<>();

    static {
        inventoryProviders.register("5000-main-inventory", player -> new ItemStackArrayIterable(player.inventory.mainInventory));
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
    public static Map<ItemStack, Integer> takeFromInventory(EntityPlayerMP player, Predicate<ItemStack> predicate, boolean simulate, int count) {
        Map<ItemStack, Integer> store = new ItemStackMap<>();
        int sum = 0;
        for (Function<EntityPlayerMP, Iterable<ItemStack>> provider : inventoryProviders) {
            sum += takeFromInventory(provider.apply(player), predicate, simulate, count - sum, true, store);
            if (sum >= count)
                return store;
        }
        return store;
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
    public static Map<ItemStack, Integer> takeFromInventory(Iterable<ItemStack> inv, Predicate<ItemStack> predicate, boolean simulate, int count) {
        Map<ItemStack, Integer> store = new ItemStackMap<>();
        takeFromInventory(inv, predicate, simulate, count, true, store);
        return store;
    }

    private static int takeFromInventory(Iterable<ItemStack> inv, Predicate<ItemStack> predicate, boolean simulate, int count, boolean recurse, Map<ItemStack, Integer> store) {
        int found = 0;
        for (Iterator<ItemStack> iterator = inv.iterator(); iterator.hasNext(); ) {
            ItemStack stack = iterator.next();
            // invalid stack
            if (stack == null || stack.getItem() == null || stack.stackSize <= 0) continue;

            if (predicate.test(stack)) {
                found += stack.stackSize;
                if (found > count) {
                    if (!simulate) {
                        // leave the surplus in its place
                        int surplus = found - count;
                        store.merge(stack, stack.stackSize - surplus, Integer::sum);
                        stack.stackSize = surplus;
                    }
                    return count;
                }
                store.merge(stack, stack.stackSize, Integer::sum);
                if (!simulate)
                    iterator.remove();
                if (found == count)
                    return count;
            }
            if (!recurse) continue;
            for (Function<ItemStack, Iterable<ItemStack>> f : stackExtractors) {
                Iterable<ItemStack> stacks = f.apply(stack);
                if (stacks == null) continue;
                found += takeFromInventory(inv, predicate, simulate, count - found, false, store);
                if (found > count) return found;
            }
        }
        return found;
    }
}
