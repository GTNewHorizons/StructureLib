package com.gtnewhorizon.structurelib;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.gui.EmptyContainer;
import com.gtnewhorizon.structurelib.gui.GuiScreenConfigureChannels;

import cpw.mods.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        switch (ID) {
            case 0:
                return new EmptyContainer(player);
            default:
                return null;
        }
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        switch (ID) {
            case 0:
                return new GuiScreenConfigureChannels(new EmptyContainer(player), player.inventory.getStackInSlot(x));
            default:
                return null;
        }
    }
}
