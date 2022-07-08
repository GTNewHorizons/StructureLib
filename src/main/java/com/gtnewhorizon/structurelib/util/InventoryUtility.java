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

import com.gtnewhorizon.structurelib.util.ItemStackArrayIterable;
import com.gtnewhorizon.structurelib.util.SortedRegistry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import java.util.Iterator;
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
    public static int takeFromInventory(EntityPlayerMP player, Predicate<ItemStack> predicate, boolean simulate, int count) {
        int found = 0;
        for (Function<EntityPlayerMP, Iterable<ItemStack>> provider : inventoryProviders) {
            found += takeFromInventory(provider.apply(player), predicate, simulate, count - found);
            if (found >= count)
                return found;
        }
        return found;
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
    public static int takeFromInventory(Iterable<ItemStack> inv, Predicate<ItemStack> predicate, boolean simulate, int count) {
        return takeFromInventory(inv, predicate, simulate, count, true);
    }

    private static int takeFromInventory(Iterable<ItemStack> inv, Predicate<ItemStack> predicate, boolean simulate, int count, boolean recurse) {
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
                        stack.stackSize = found - count;
                    }
                    return count;
                }
                if (!simulate)
                    iterator.remove();
                if (found == count)
                    return count;
            }
            if (!recurse) continue;
            for (Function<ItemStack, Iterable<ItemStack>> f : stackExtractors) {
                Iterable<ItemStack> stacks = f.apply(stack);
                if (stacks == null) continue;
                found += takeFromInventory(inv, predicate, simulate, count - found, false);
                if (found > count) return found;
            }
        }
        return found;
    }
}
