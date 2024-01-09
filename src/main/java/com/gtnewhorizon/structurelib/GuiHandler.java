package com.gtnewhorizon.structurelib;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.gui.GuiScreenConfigureChannels;

import cpw.mods.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        switch (ID) {
            case 0:
                return new GuiScreenConfigureChannels(player.inventory.getStackInSlot(x));
            default:
                return null;
        }
    }
}
