package com.gtnewhorizon.structurelib.util;

import net.minecraft.item.ItemStack;

import com.cleanroommc.bogosorter.api.BeforeSortEvent;
import com.gtnewhorizon.structurelib.item.ItemConstructableTrigger;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BogoCompat {

    private static final int MOUSE_MIDDLE = -98;

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onBeforeSortInGui(final BeforeSortEvent event) {
        // Cancel Bogo inventory sort if hologram is picked-up/held.
        // Bogo sort is usually bound to middle-click.
        // This is just to avoid annoying player experience.
        if (!event.isFromKeybind()) return;

        final boolean isInGui = event.isInGui();
        if (event.getSortKeyCode() != MOUSE_MIDDLE) return;

        ItemStack item = null;

        if (isInGui) item = event.getPlayer().inventory.getItemStack();
        else item = event.getPlayer().getHeldItem();

        if (item == null || !(item.getItem() instanceof ItemConstructableTrigger)) return;

        // Cancel sort
        event.setCanceled(true);
    }
}
