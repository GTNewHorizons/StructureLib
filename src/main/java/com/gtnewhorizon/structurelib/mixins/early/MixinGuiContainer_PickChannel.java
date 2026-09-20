package com.gtnewhorizon.structurelib.mixins.early;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.structurelib.ChannelPickHandler;
import com.gtnewhorizon.structurelib.ChannelWipeConfirmation;
import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.item.ItemConstructableTrigger;

import codechicken.nei.NEIClientConfig;
import codechicken.nei.guihook.GuiContainerManager;
import cpw.mods.fml.common.Loader;

@Mixin(value = GuiContainer.class, priority = 100)
public abstract class MixinGuiContainer_PickChannel {

    @Inject(method = "mouseClicked(III)V", at = @At("HEAD"), cancellable = true)
    private void slib$onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (mouseButton != 2) return;

        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return;

        ItemStack cursorStack = player.inventory.getItemStack();
        if (cursorStack == null || !(cursorStack.getItem() instanceof ItemConstructableTrigger)) return;

        // Sink middle click as soon as projector is picked up
        ci.cancel();

        ItemStack hoveredStack = null;
        try {
            Slot slot = slib$invokeGetSlotAtPosition(mouseX, mouseY);
            if (slot != null) {
                hoveredStack = slot.getStack();
            }

            // try NEI
            if (hoveredStack == null && Loader.isModLoaded("NotEnoughItems") && !NEIClientConfig.isHidden()) {
                hoveredStack = GuiContainerManager.getStackMouseOver((GuiContainer) (Object) this);
            }
        } catch (Exception e) {
            StructureLib.LOGGER.error("Error while resolving hovered slot for channel pick", e);
        }

        boolean isSneaking = GuiScreen.isShiftKeyDown();
        if (isSneaking && !ChannelWipeConfirmation.confirm(player)) return;

        ChannelPickHandler.handleMiddleClick(player, cursorStack, hoveredStack, isSneaking);
    }

    @Invoker("getSlotAtPosition")
    protected abstract Slot slib$invokeGetSlotAtPosition(int x, int y);
}
