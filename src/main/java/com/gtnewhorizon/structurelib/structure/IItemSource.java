package com.gtnewhorizon.structurelib.structure;

import com.gtnewhorizon.structurelib.util.InventoryIterable;
import com.gtnewhorizon.structurelib.util.InventoryUtility;
import com.gtnewhorizon.structurelib.util.ItemStackPredicate;
import com.gtnewhorizon.structurelib.util.ItemStackPredicate.NBTMode;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * Represent an item source. Take only, cannot be placed back.
 */
public interface IItemSource {
    /**
     * Retrieve some amount of items from this item source matching given filter.
     * <p>
     * We probably don't need to get a lot of items from this method,
     * so I don't care about the overhead of storing all fetched items even if downstream code doesn't need it
     *
     * @param predicate filtering predicate. should return true for ItemStacks that should be added to result collection
     * @param simulate  whether to actually commit the modification. true for dry run, false otherwise.
     * @param count     how many items to extract.
     * @return A nonnull map reflecting the result of extraction. Note this map is NBT and metadata aware.
     */
    @Nonnull
    Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean simulate, int count);

    /**
     * Take exactly one item that matches the predicate.
     *
     * @param predicate filtering predicate. should return true for ItemStacks that should be added to result collection
     * @param simulate  whether to actually commit the modification. true for dry run, false otherwise.
     * @return item stack, or null if none matches
     * @throws IllegalArgumentException if given predicate is null.
     */
    default ItemStack takeOne(Predicate<ItemStack> predicate, boolean simulate) {
        if (predicate == null) throw new IllegalArgumentException();
        Map<ItemStack, Integer> take = take(predicate, simulate, 1);
        return take.isEmpty() ? null : take.keySet().iterator().next();
    }

    /**
     * Take exactly count amount of items matching predicate.
     *
     * @param predicate filtering predicate. should return true for ItemStacks that should be added to result collection
     * @param simulate  whether to actually commit the modification. true for dry run, false otherwise.
     * @param count     how many items to extract.
     * @return true if at least count amount of items can be/is taken.
     * @throws IllegalArgumentException if given predicate is null
     */
    default boolean takeAll(Predicate<ItemStack> predicate, boolean simulate, int count) {
        // fast path for 1 item take requests
        if (count == 1) return takeOne(predicate, simulate) != null;
        Map<ItemStack, Integer> have = take(predicate, true, count);
        if (have.values().stream().mapToInt(Integer::intValue).sum() < count) return false;
        take(predicate, simulate, count);
        return true;
    }

    /**
     * Take exactly one item. ItemStack given must have a stack size of 1.
     * <p>
     * This is meant to be an optimized version of {@link #takeOne(Predicate, boolean)}.
     * Implementations that support item storage indexed by ItemStack can use this to speed up calculation.
     *
     * @param stack    what item to extract. The NBT tag of this ItemStack is siginifcant and will be
     *                 considered during matching. must have a stack size of 1.
     * @param simulate whether to actually commit the modification. true for dry run, false otherwise.
     * @return true if found. false otherwise
     * @throws IllegalArgumentException if given stack is invalid, or has a stack size other than 1.
     */
    default boolean takeOne(ItemStack stack, boolean simulate) {
        if (stack == null || stack.getItem() == null || stack.stackSize != 1) throw new IllegalArgumentException();
        ItemStack took = takeOne(ItemStackPredicate.from(stack, NBTMode.EXACT), simulate);
        return took != null && took.stackSize > 0;
    }

    /**
     * Take some item. Will not take item if it cannot find enough items to take.
     * <p>
     * This is meant to be an optimized version of {@link #takeOne(Predicate, boolean)}.
     * Implementations that support indexing off ItemStack can use this to speed up calculation.
     *
     * @param stack    what item and how many to extract. The NBT tag of this ItemStack is siginifcant and will be
     *                 considered during matching
     * @param simulate whether to actually commit the modification. true for dry run, false otherwise.
     * @return true if enough item can be/has been extracted. false otherwise
     * @throws IllegalArgumentException if given stack is invalid
     */
    default boolean takeAll(ItemStack stack, boolean simulate) {
        if (stack == null || stack.getItem() == null) throw new IllegalArgumentException();
        // fast path for 1 item take requests
        if (stack.stackSize == 1) return takeOne(stack, simulate);
        ItemStackPredicate predicate = ItemStackPredicate.from(stack, NBTMode.EXACT);
        Map<ItemStack, Integer> have = take(predicate, true, stack.stackSize);
        if (have.values().stream().mapToInt(Integer::intValue).sum() < stack.stackSize) return false;
        if (simulate) return true;
        take(predicate, false, stack.stackSize);
        return true;
    }

    /**
     * Construct an IItemSource from inventories associated with given player.
     * <p>
     * This will be backed by {@link InventoryUtility}
     */
    static IItemSource fromPlayer(EntityPlayerMP player) {
        return new IItemSource() {
            @Nonnull
            @Override
            public Map<ItemStack, Integer> take(Predicate<ItemStack> p, boolean s, int c) {
                return InventoryUtility.takeFromInventory(player, p, s, c);
            }

            @Override
            public boolean takeOne(ItemStack stack, boolean simulate) {
                if (stack == null || stack.getItem() == null || stack.stackSize != 1)
                    throw new IllegalArgumentException();
                return InventoryUtility.takeFromInventory(player, stack, simulate) == 1;
            }

            @Override
            public boolean takeAll(ItemStack stack, boolean simulate) {
                return InventoryUtility.takeFromInventory(player, stack, simulate) == stack.stackSize;
            }
        };
    }

    /**
     * Construct an IItemSource from given inventory
     * <p>
     * This will be backed by {@link InventoryUtility}
     */
    static IItemSource fromInventory(IInventory inv) {
        return (p, s, c) -> InventoryUtility.takeFromInventory(new InventoryIterable<>(inv), p, s, c, true);
    }
}
