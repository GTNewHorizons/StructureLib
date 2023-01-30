package com.gtnewhorizon.structurelib.util;

import java.util.Iterator;

import net.minecraft.item.ItemStack;

public class ItemStackArrayIterable implements Iterable<ItemStack> {

    private final ItemStack[] stacks;

    public ItemStackArrayIterable(ItemStack[] stacks) {
        this.stacks = stacks;
    }

    public ItemStack[] getStacks() {
        return stacks;
    }

    @Override
    public Iterator<ItemStack> iterator() {
        return new Iterator<ItemStack>() {

            private int ptr = 0;

            @Override
            public boolean hasNext() {
                return ptr < stacks.length;
            }

            @Override
            public ItemStack next() {
                return stacks[ptr++];
            }

            @Override
            public void remove() {
                stacks[ptr - 1] = null;
            }
        };
    }
}
