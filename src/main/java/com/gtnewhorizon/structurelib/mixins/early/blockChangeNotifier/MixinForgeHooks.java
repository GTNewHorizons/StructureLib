package com.gtnewhorizon.structurelib.mixins.early.blockChangeNotifier;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.BlockSnapshot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.structurelib.event.BlockChangeNotifier;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(ForgeHooks.class)
public class MixinForgeHooks {

    @Inject(
            method = "onPlaceItemIntoWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;markAndNotifyBlock(IIILnet/minecraft/world/chunk/Chunk;Lnet/minecraft/block/Block;Lnet/minecraft/block/Block;I)V",
                    shift = At.Shift.AFTER),
            remap = false)
    private static void onBlockChange(ItemStack itemstack, EntityPlayer player, World world, int x, int y, int z,
            int side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir,
            @Local(name = "blocksnapshot") BlockSnapshot blocksnapshot, @Local(name = "newBlock") Block newBlock,
            @Local(name = "metadata") int newMetadata) {
        BlockChangeNotifier.onBlockChange(
                world,
                null,
                blocksnapshot.x,
                blocksnapshot.y,
                blocksnapshot.z,
                blocksnapshot.replacedBlock,
                newBlock,
                blocksnapshot.meta,
                newMetadata);
    }

}
