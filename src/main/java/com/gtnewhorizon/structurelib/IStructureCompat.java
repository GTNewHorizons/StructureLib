package com.gtnewhorizon.structurelib;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

public interface IStructureCompat {

    void markTextureUsed(IIcon o);

    boolean checkServerUtilitiesPermission(World world, EntityPlayer actor, int x, int z);
}
