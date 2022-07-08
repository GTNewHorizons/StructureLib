package com.gtnewhorizon.structurelib.structure;

import com.gtnewhorizon.structurelib.util.InventoryIterable;
import com.gtnewhorizon.structurelib.util.InventoryUtility;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Predicate;

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

    static IItemSource fromPlayer(EntityPlayerMP player) {
        return (p, s, c) -> InventoryUtility.takeFromInventory(player, p, s, c);
    }

    static IItemSource fromPlayer(EntityPlayerMP player, int except) {
        return (p, s, c) -> InventoryUtility.takeFromInventory(player, p, s, c);
    }

    static IItemSource fromInventory(IInventory inv) {
        return (p, s, c) -> InventoryUtility.takeFromInventory(new InventoryIterable(inv), p, s, c);
    }
}
