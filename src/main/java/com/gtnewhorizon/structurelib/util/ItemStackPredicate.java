package com.gtnewhorizon.structurelib.util;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public final class ItemStackPredicate implements Predicate<ItemStack> {
    public static ItemStackPredicate from(Item item) {
        return new ItemStackPredicate(item);
    }

    public static ItemStackPredicate from(Block block) {
        return new ItemStackPredicate(Item.getItemFromBlock(block));
    }

    private final Item item;
    private int meta;

    private NBTTagCompound tag;
    private NBTMode mode;

    private ItemStackPredicate(Item item) {
        this.item = item;
    }

    public ItemStackPredicate withMeta(int meta) {
        this.meta = meta;
        return this;
    }

    public ItemStackPredicate withTag(NBTMode mode, NBTTagCompound tag) {
        this.mode = mode;
        this.tag = tag;
        return this;
    }

    @Override
    public boolean test(ItemStack itemStack) {
        if (item != null) if (itemStack.getItem() != item) return false;
        if (meta != -1) if (Items.feather.getDamage(itemStack) != meta) return false;
        return mode.test(tag, itemStack.stackTagCompound);
    }

    public enum NBTMode implements BiPredicate<NBTTagCompound, NBTTagCompound> {
        IGNORE {
            @Override
            public boolean test(NBTTagCompound lhs, NBTTagCompound rhs) {
                return true;
            }
        },
        IN {
            @Override
            public boolean test(NBTTagCompound lhs, NBTTagCompound rhs) {
                if (lhs == null || lhs.hasNoTags()) return true;
                if (rhs == null || rhs.hasNoTags()) return false;
                for (String key : MiscUtils.getTagKeys(lhs)) {
                    if (!rhs.hasKey(key, lhs.func_150299_b(key)))
                        return false;
                    NBTBase tag = lhs.getTag(key);
                    if (tag instanceof NBTTagCompound) {
                        if (!test((NBTTagCompound) tag, rhs.getCompoundTag(key)))
                            return false;
                    } else {
                        if (!tag.equals(rhs.getTag(key)))
                            return false;
                    }
                }
                return true;
            }
        },
        EXACT {
            @Override
            public boolean test(NBTTagCompound lhs, NBTTagCompound rhs) {
                if (lhs != null && lhs.hasNoTags()) lhs = null;
                if (rhs != null && rhs.hasNoTags()) rhs = null;
                return Objects.equals(lhs, rhs);
            }
        },
        IGNORE_KNOWN_INSIGNIFICANT_TAGS {
            @Override
            public boolean test(NBTTagCompound lhs, NBTTagCompound rhs) {
                // fast path for empty tags
                if (rhs == null || rhs.hasNoTags()) return lhs == null || lhs.hasNoTags();
                // TODO make an implementation without copying a potentially huge tag
                rhs = (NBTTagCompound) rhs.copy();
                for (String s : KNOWN_INSIGNIFICANT_TAGS) rhs.removeTag(s);
                return EXACT.test(lhs, rhs);
            }
        };
        private static final String[] KNOWN_INSIGNIFICANT_TAGS = {
            "display", // TODO expand/refine this
        };
    }
}
