package com.gtnewhorizon.structurelib.util;

import java.util.Iterator;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class InventoryIterable<Inv extends IInventory> implements Iterable<ItemStack> {
    private final Inv inv;

    public InventoryIterable(Inv inv) {
        this.inv = inv;
    }

    public Inv getInventory() {
        return inv;
    }

    @Override
    public Iterator<ItemStack> iterator() {
        return new Iterator<ItemStack>() {
            private int ptr = 0;

            @Override
            public boolean hasNext() {
                return ptr < inv.getSizeInventory();
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
