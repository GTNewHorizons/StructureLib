package com.gtnewhorizon.structurelib.mixins.early.blockChangeNotifier;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.structurelib.event.BlockChangeNotifier;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

@Mixin(World.class)
public class MixinWorld {

    @Inject(
            method = "Lnet/minecraft/world/World;setBlock(IIILnet/minecraft/block/Block;II)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;markAndNotifyBlock(IIILnet/minecraft/world/chunk/Chunk;Lnet/minecraft/block/Block;Lnet/minecraft/block/Block;I)V",
                    shift = At.Shift.AFTER,
                    remap = false))
    public void setBlockObserve(int x, int y, int z, Block newBlock, int newMeta, int flags,
            CallbackInfoReturnable<Boolean> cir, @Local(name = "block1") Block originalBlock,
            @Share("originalMeta") LocalIntRef originalMeta, @Share("chunk") LocalRef<Chunk> chunkLocalRef) {
        BlockChangeNotifier.onBlockChange(
                (World) ((Object) (this)),
                chunkLocalRef.get(),
                x,
                y,
                z,
                originalBlock,
                newBlock,
                originalMeta.get(),
                newMeta);
    }

    @Inject(
            method = "Lnet/minecraft/world/World;setBlock(IIILnet/minecraft/block/Block;II)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/Chunk;func_150807_a(IIILnet/minecraft/block/Block;I)Z",
                    shift = At.Shift.BEFORE))
    public void captureChunkAndOriginalMetadata(int x, int y, int z, Block blockIn, int metadataIn, int flags,
            CallbackInfoReturnable<Boolean> cir, @Local(name = "chunk") Chunk chunk,
            @Share("originalMeta") LocalIntRef originalMeta, @Share("chunk") LocalRef<Chunk> chunkLocalRef) {
        chunkLocalRef.set(chunk);
        originalMeta.set(chunk.getBlockMetadata(x & 15, y, z & 15));
    }
}
