package com.gtnewhorizon.structurelib.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class InventoryIterable<Inv extends IInventory> implements Iterable<ItemStack> {

    private final Inv inv;
    private final int maxSlot;

    public InventoryIterable(Inv inv) {
        this(inv, -1);
    }

    InventoryIterable(Inv inv, int maxSlot) {
        this.inv = inv;
        this.maxSlot = maxSlot;
    }

    public Inv getInventory() {
        return inv;
    }

    /**
     * The slot this iteration stops at, exclusive, or -1 when it runs to the end of the inventory.
     * <p>
     * Callers that have to write into the inventory instead of iterating over it, e.g. because they replace a stack
     * rather than removing it, need this to respect the same range the iterator uses.
     */
    public int getMaxSlot() {
        return maxSlot;
    }

    @Override
    public Iterator<ItemStack> iterator() {
        return new Iterator<ItemStack>() {

            private int ptr = 0;

            @Override
            public boolean hasNext() {
                return ptr < inv.getSizeInventory() && (maxSlot == -1 || ptr < maxSlot);
            }

            @Override
            public ItemStack next() {
                if (!hasNext()) throw new NoSuchElementException();
                return inv.getStackInSlot(ptr++);
            }

            @Override
            public void remove() {
                inv.setInventorySlotContents(ptr - 1, null);
            }
        };
    }
}
