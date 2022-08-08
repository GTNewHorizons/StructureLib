package com.gtnewhorizon.structurelib.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

/**
 * Use StructureUtility to instantiate
 */
public interface IStructureElementChain<T> extends IStructureElement<T> {
    IStructureElement<T>[] fallbacks();

    @Override
    default boolean check(T t, World world, int x, int y, int z) {
        for (IStructureElement<T> fallback : fallbacks()) {
            if (fallback.check(t, world, x, y, z)) {
                return true;
            }
        }
        return false;
    }

    @Override
    default boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
        for (IStructureElement<T> fallback : fallbacks()) {
            if (fallback.spawnHint(t, world, x, y, z, trigger)) {
                return true;
            }
        }
        return false;
    }

    @Override
    default boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
        for (IStructureElement<T> fallback : fallbacks()) {
            if (fallback.placeBlock(t, world, x, y, z, trigger)) {
                return true;
            }
        }
        return false;
    }

    @Override
    default PlaceResult survivalPlaceBlock(
            T t,
            World world,
            int x,
            int y,
            int z,
            ItemStack trigger,
            IItemSource s,
            EntityPlayerMP actor,
            Consumer<IChatComponent> chatter) {
        boolean haveSkip = false;
        List<IChatComponent> bufferedNoise = new ArrayList<>();
        for (IStructureElement<T> fallback : fallbacks()) {
            PlaceResult result = fallback.survivalPlaceBlock(t, world, x, y, z, trigger, s, actor, bufferedNoise::add);
            switch (result) {
                case REJECT:
                    break;
                case SKIP:
                    haveSkip = true;
                    break;
                default:
                    return result;
            }
        }
        // dump all that noise back into upstream
        bufferedNoise.forEach(chatter);
        // TODO need reconsider to ensure this is the right course of action
        return haveSkip ? PlaceResult.SKIP : PlaceResult.REJECT;
    }
}
