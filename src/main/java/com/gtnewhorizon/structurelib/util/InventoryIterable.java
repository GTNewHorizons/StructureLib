package com.gtnewhorizon.structurelib.util;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import java.util.Iterator;

public class InventoryIterable implements Iterable<ItemStack> {
    private final IInventory inv;

    public InventoryIterable(IInventory inv) {
        this.inv = inv;
    }

    @Override
    public Iterator<ItemStack> iterator() {
        return new Iterator<ItemStack>() {
            private int ptr = 0;

            @Override
            public boolean hasNext() {
                return ptr >= inv.getSizeInventory();
            }

            @Override
            public ItemStack next() {
                return inv.getStackInSlot(ptr++);
            }

            @Override
            public void remove() {
                inv.setInventorySlotContents(ptr - 1, null);
            }
        };
    }
}
