/**
 * Copyright (c) 2022, glee8e This file is part of StructureLib.
 * <p>
 * StructureLib is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * <p>
 * StructureLib is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License along with Foobar; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.gtnewhorizon.structurelib.util;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryEnderChest;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.gtnhlib.util.map.ItemStackMap;
import com.gtnewhorizon.structurelib.util.InventoryUtility.ItemStackExtractor.APIType;
import com.gtnewhorizon.structurelib.util.ItemStackPredicate.NBTMode;

/**
 * This class is part of API, but is not stable. Use at your own risk.
 */
public class InventoryUtility {

    private static final SortedRegistry<ItemStackExtractor> stackExtractors = new SortedRegistry<>();
    private static final List<Predicate<? super EntityPlayerMP>> enableEnder = new CopyOnWriteArrayList<>();
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
                // player save its content using means other than inv.markDirty()
                // here we only need to sync it to client
                inv.getInventory().player.inventoryContainer.detectAndSendChanges();
            }
        });
        inventoryProviders
                .register("7000-ender-inventory", new InventoryProvider<InventoryIterable<InventoryEnderChest>>() {

                    @Override
                    public InventoryIterable<InventoryEnderChest> getInventory(EntityPlayerMP player) {
                        if (enableEnder.stream().anyMatch(p -> p.test(player)))
                            return new InventoryIterable<>(player.getInventoryEnderChest());
                        return null;
                    }

                    @Override
                    public void markDirty(InventoryIterable<InventoryEnderChest> inv) {
                        // inv.getInventory().markDirty();
                        // TODO this seems to be a noop
                    }
                });
    }

    public static void registerEnableEnderCondition(Predicate<? super EntityPlayerMP> predicate) {
        enableEnder.add(predicate);
    }

    public static void registerStackExtractor(String key, ItemStackExtractor val) {
        if (Arrays.stream(APIType.values()).noneMatch(val::isAPIImplemented))
            throw new IllegalArgumentException("Must implement at least one API");
        stackExtractors.register(key, val);
    }

    public static <Inv extends IInventory> void registerStackExtractor(String key,
            Function<ItemStack, ? extends Inv> extractor) {
        registerStackExtractor(key, newItemStackProvider(extractor));
    }

    public static void registerInventoryProvider(String key, InventoryProvider<?> val) {
        inventoryProviders.register(key, val);
    }

    public static <Inv extends IInventory> void registerInventoryProvider(String key,
            Function<EntityPlayerMP, ? extends Inv> extractor) {
        registerInventoryProvider(key, newInventoryProvider(extractor));
    }

    public static Iterator<? extends ItemStackExtractor> getStackExtractors() {
        return stackExtractors.iterator();
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

    public static ItemStackExtractor newItemStackProvider(Function<ItemStack, ? extends IInventory> extractor) {
        return new ItemStackExtractor() {

            @Override
            public boolean isAPIImplemented(APIType type) {
                return type == APIType.MAIN;
            }

            @Override
            public int takeFromStack(Predicate<ItemStack> predicate, boolean simulate, int count,
                    ItemStackCounter store, ItemStack stack, ItemStack filter, EntityPlayerMP player) {
                IInventory inv = extractor.apply(stack);
                if (inv == null) return 0;
                int found = takeFromInventory(
                        new InventoryIterable<>(inv),
                        predicate,
                        simulate,
                        count,
                        store,
                        filter,
                        player,
                        false);
                if (found > 0) inv.markDirty();
                return found;
            }
        };
    }

    /**
     * take count amount of stacks that matches given filter. might take partial amount of stuff, so simulation is
     * suggested if you ever need to take more than one.
     *
     * @param player    source of stacks
     * @param predicate item stack filter
     * @param simulate  whether to do removal
     * @param count     let's hope int size is enough...
     * @return amount taken. never negative nor bigger than count...
     */
    public static Map<ItemStack, Integer> takeFromInventory(EntityPlayerMP player, Predicate<ItemStack> predicate,
            boolean simulate, int count) {
        ItemStackCounterImpl store = new ItemStackCounterImpl();
        int sum = 0;
        for (InventoryProvider<?> provider : inventoryProviders) {
            sum += takeFromPlayer(player, predicate, simulate, count - sum, store, provider, null);
            if (sum >= count) return store.getStore();
        }
        return store.getStore();
    }

    /**
     * take count amount of stacks that matches given filter. might take partial amount of stuff, so simulation is
     * suggested if you ever need to take more than one.
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
        ItemStackCounterImpl store = new ItemStackCounterImpl();
        for (InventoryProvider<?> provider : inventoryProviders) {
            sum += takeFromPlayer(player, predicate, simulate, count - sum, store, provider, filter);
            if (sum >= count) return sum;
        }
        return sum;
    }

    // workaround java generics issue
    private static <R extends Iterable<ItemStack>> int takeFromPlayer(EntityPlayerMP player,
            Predicate<ItemStack> predicate, boolean simulate, int count, ItemStackCounterImpl store,
            InventoryProvider<R> provider, ItemStack filter) {
        R inv = provider.getInventory(player);
        if (inv == null) return 0;
        int taken = takeFromInventory(inv, predicate, simulate, count, store, filter, player, true);
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
     * @param recursive do recursive lookup using {@link #getStackExtractors() stack extractors}
     * @return amount taken. never negative nor bigger than count...
     */
    public static Map<ItemStack, Integer> takeFromInventory(Iterable<ItemStack> inv, Predicate<ItemStack> predicate,
            boolean simulate, int count, boolean recursive) {
        ItemStackCounterImpl store = new ItemStackCounterImpl();
        takeFromInventory(inv, predicate, simulate, count, store, null, null, recursive);
        return store.getStore();
    }

    private static int takeFromInventory(@Nonnull Iterable<ItemStack> inv, @Nonnull Predicate<ItemStack> predicate,
            boolean simulate, int count, @Nonnull ItemStackCounter store, @Nullable ItemStack filter,
            @Nullable EntityPlayerMP player, boolean recursive) {
        int found = 0;
        ItemStack copiedFilter = null;
        if (filter != null) {
            copiedFilter = new ItemStack(filter.getItem(), filter.stackSize, Items.feather.getDamage(filter));
            copiedFilter.setTagCompound(filter.stackTagCompound);
        }
        for (Iterator<ItemStack> iterator = inv.iterator(); iterator.hasNext();) {
            ItemStack stack = iterator.next();
            // invalid stack
            if (stack == null || stack.getItem() == null || stack.stackSize <= 0) continue;

            if (predicate.test(stack)) {
                found += stack.stackSize;
                if (found > count) {
                    int surplus = found - count;
                    store.add(stack, stack.stackSize - surplus);
                    if (!simulate) {
                        // leave the surplus in its place
                        stack.stackSize = surplus;
                    }
                    return count;
                }
                store.add(stack, stack.stackSize);
                if (!simulate) iterator.remove();
                if (found == count) return count;
            }
            if (!recursive) continue;
            for (ItemStackExtractor f : stackExtractors) {
                if (filter != null && f.isAPIImplemented(APIType.EXTRACT_ONE_STACK)) {
                    copiedFilter.stackSize = count - found;
                    found += f.getItem(stack, copiedFilter, simulate, player);
                } else {
                    found += f.takeFromStack(predicate, simulate, count - found, store, stack, filter, player);
                }
                if (found >= count) return found;
            }
        }
        return found;
    }

    public interface OptimizedExtractor {

        /**
         * Extract a particular type of item. The extractor can choose to not return all items contained within this
         * item as long as it makes sense, but the author should inform the player of this.
         * <p>
         * Whether optimized extractor do recursive extraction is at the discretion of implementor.
         *
         * @param source    from where an extraction should be attempted
         * @param toExtract stack to extract. should not be mutated! match the NBT tag using EXACT mode.
         * @param simulate  true if only query but does not actually remove
         * @param player    executor of extraction, or null if from a machine
         * @return amount extracted
         */
        int extract(ItemStack source, ItemStack toExtract, boolean simulate, EntityPlayerMP player);
    }

    public interface InventoryProvider<R extends Iterable<ItemStack>> {

        R getInventory(EntityPlayerMP player);

        void markDirty(R inv);
    }

    public interface ItemStackExtractor {

        enum APIType {
            MAIN,
            EXTRACT_ONE_STACK,
        }

        boolean isAPIImplemented(APIType type);

        /**
         * Extract a particular type of item. The extractor can choose to not return all items contained within this
         * item as long as it makes sense, but the author should inform the player of this.
         * <p>
         * Whether this method does recursive extraction is at the discretion of implementor.
         *
         * @param predicate the main filtering predicate. never null. It's assumed predicate always returns true on
         *                  filter, if that is not null
         * @param store     where to store extracted items. Should increment the
         * @param source    from where an extraction should be attempted
         * @param filter    stack to extract. should not be mutated! match the NBT tag using EXACT mode. might be null.
         * @param simulate  true if only query but does not actually remove
         * @param player    executor of extraction, or null if from a machine
         * @return amount extracted, or -1 if this is not implemented
         */
        int takeFromStack(Predicate<ItemStack> predicate, boolean simulate, int count, ItemStackCounter store,
                ItemStack source, ItemStack filter, EntityPlayerMP player);

        /**
         * Extract a particular type of item. The extractor can choose to not return all items contained within this
         * item as long as it makes sense, but the author should inform the player of this.
         * <p>
         * Whether this method does recursive extraction is at the discretion of implementor.
         *
         * @param source    from where an extraction should be attempted
         * @param toExtract stack to extract. should not be mutated! match the NBT tag using EXACT mode.
         * @param simulate  true if only query but does not actually remove
         * @param player    executor of extraction, or null if from a machine
         * @return amount extracted, or -1 if this is not implemented
         */
        default int getItem(ItemStack source, ItemStack toExtract, boolean simulate, EntityPlayerMP player) {
            return -1;
        }

        static ItemStackExtractor createOnlyOptimized(@Nonnull OptimizedExtractor optimizedExtractor) {
            return new ItemStackExtractor() {

                @Override
                public boolean isAPIImplemented(APIType type) {
                    return type == APIType.EXTRACT_ONE_STACK;
                }

                @Override
                public int takeFromStack(Predicate<ItemStack> predicate, boolean simulate, int count,
                        ItemStackCounter store, ItemStack stack, ItemStack filter, EntityPlayerMP player) {
                    return 0;
                }

                @Override
                public int getItem(ItemStack source, ItemStack toExtract, boolean simulate, EntityPlayerMP player) {
                    return optimizedExtractor.extract(source, toExtract, simulate, player);
                }
            };
        }
    }

    public interface ItemStackCounter {

        /**
         * Add some amount of stack. Note stackSize is used instead of stack.stackSize.
         */
        void add(ItemStack stack, int stackSize);
    }

    private static class ItemStackCounterImpl implements ItemStackCounter {

        private final Map<ItemStack, Integer> store = new ItemStackMap<>(true);

        @Override
        public void add(ItemStack stack, int stackSize) {
            if (stack == null || stack.getItem() == null || stackSize <= 0) throw new IllegalArgumentException();
            store.merge(stack, stackSize, Integer::sum);
        }

        public Map<ItemStack, Integer> getStore() {
            return store;
        }
    }
}
