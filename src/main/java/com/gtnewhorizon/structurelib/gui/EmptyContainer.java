package com.gtnewhorizon.structurelib.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public class EmptyContainer extends Container {

    private final EntityPlayer player;

    public EmptyContainer(EntityPlayer player) {
        this.player = player;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return player == this.player;
    }
}
