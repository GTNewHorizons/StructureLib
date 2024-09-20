package com.gtnewhorizon.structurelib.structure;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

class LazyStructureElement<T> implements IStructureElementDeferred<T> {

    private Function<T, IStructureElement<T>> to;
    private IStructureElement<T> elem;

    public LazyStructureElement(Function<T, IStructureElement<T>> to) {
        this.to = to;
    }

    private IStructureElement<T> get(T t) {
        if (to != null) {
            elem = to.apply(t);
            to = null;
        }
        return elem;
    }

    @Override
    public boolean check(T t, World world, int x, int y, int z) {
        return get(t).check(t, world, x, y, z);
    }

    @Override
    public boolean couldBeValid(T t, World world, int x, int y, int z, ItemStack trigger) {
        return get(t).couldBeValid(t, world, x, y, z, trigger);
    }

    @Override
    public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
        return get(t).placeBlock(t, world, x, y, z, trigger);
    }

    @Override
    public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
        return get(t).spawnHint(t, world, x, y, z, trigger);
    }

    @Nullable
    @Override
    public BlocksToPlace getBlocksToPlace(T t, World world, int x, int y, int z, ItemStack trigger,
            AutoPlaceEnvironment env) {
        return get(t).getBlocksToPlace(t, world, x, y, z, trigger, env);
    }

    @Override
    public PlaceResult survivalPlaceBlock(T t, World world, int x, int y, int z, ItemStack trigger, IItemSource s,
            EntityPlayerMP actor, Consumer<IChatComponent> chatter) {
        return get(t).survivalPlaceBlock(t, world, x, y, z, trigger, s, actor, chatter);
    }

    @Override
    public PlaceResult survivalPlaceBlock(T t, World world, int x, int y, int z, ItemStack trigger,
            AutoPlaceEnvironment env) {
        return get(t).survivalPlaceBlock(t, world, x, y, z, trigger, env);
    }
}
