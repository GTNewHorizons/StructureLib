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

public interface IItemSource {
    /**
     * We are probably not going to need to get a lot of items from this method
     * so I don't care about the overhead of storing all fetched items even if downstream code doesn't need it
     */
    @Nonnull
    Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean simulate, int count);

    default ItemStack takeOne(Predicate<ItemStack> predicate, boolean simulate) {
        Map<ItemStack, Integer> take = take(predicate, simulate, 1);
        return take.isEmpty() ? null : take.keySet().iterator().next();
    }

    default boolean takeAll(Predicate<ItemStack> predicate, boolean simulate, int count) {
        // fast path for 1 item take requests
        if (count == 1) return takeOne(predicate, simulate) != null;
        Map<ItemStack, Integer> have = take(predicate, true, count);
        if (have.values().stream().mapToInt(Integer::intValue).sum() < count) return false;
        take(predicate, simulate, count);
        return true;
    }

    default boolean takeOne(ItemStack stack, boolean simulate) {
        if (stack == null || stack.getItem() == null || stack.stackSize != 1) throw new IllegalArgumentException();
        ItemStack took = takeOne(ItemStackPredicate.from(stack, NBTMode.EXACT), simulate);
        return took == null || took.stackSize <= 0;
    }

    default boolean takeAll(ItemStack stack, boolean simulate) {
        // fast path for 1 item take requests
        if (stack.stackSize == 1) return takeOne(stack, simulate);
        ItemStackPredicate predicate = ItemStackPredicate.from(stack, NBTMode.EXACT);
        Map<ItemStack, Integer> have = take(predicate, true, stack.stackSize);
        if (have.values().stream().mapToInt(Integer::intValue).sum() < stack.stackSize) return false;
        take(predicate, simulate, stack.stackSize);
        return true;
    }

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

    static IItemSource fromInventory(IInventory inv) {
        return (p, s, c) -> InventoryUtility.takeFromInventory(new InventoryIterable<>(inv), p, s, c, true);
    }
}
