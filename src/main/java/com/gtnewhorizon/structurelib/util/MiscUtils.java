package com.gtnewhorizon.structurelib.util;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/**
 * not related to a certain mod!
 */
public class MiscUtils {

    private MiscUtils() {}

    public static Set<String> getTagKeys(NBTTagCompound tag) {
        return tag.func_150296_c();
    }

    /**
     * Gets the standard vanilla hit result for a player.
     */
    public static MovingObjectPosition getHitResult(EntityPlayer player) {
        double reachDistance = player instanceof EntityPlayerMP mp ? mp.theItemInWorldManager.getBlockReachDistance()
                : Minecraft.getMinecraft().playerController.getBlockReachDistance();

        Vec3 posVec = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);

        Vec3 lookVec = player.getLook(1);

        Vec3 modifiedPosVec = posVec.addVector(
                lookVec.xCoord * reachDistance,
                lookVec.yCoord * reachDistance,
                lookVec.zCoord * reachDistance);

        MovingObjectPosition hit = player.worldObj.rayTraceBlocks(posVec, modifiedPosVec, true);

        return hit != null && hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK ? null : hit;
    }
}
