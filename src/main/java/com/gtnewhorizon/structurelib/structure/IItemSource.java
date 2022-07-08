package com.gtnewhorizon.structurelib.structure;

import com.gtnewhorizon.structurelib.util.InventoryIterable;
import com.gtnewhorizon.structurelib.util.InventoryUtility;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import java.util.function.Predicate;

public interface IItemSource {
    int take(Predicate<ItemStack> predicate, boolean simulate, int count);

    default boolean takeOne(Predicate<ItemStack> predicate, boolean simulate) {
        return take(predicate, simulate, 1) == 1;
    }

    default boolean takeAll(Predicate<ItemStack> predicate, boolean simulate, int count) {
        // fast path for 1 item take requests
        if (count == 1) return takeOne(predicate, simulate);
        int have = take(predicate, true, count);
        if (have < count) return false;
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
