package com.gtnewhorizon.structurelib.xmod;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import cpw.mods.fml.common.Optional;
import serverutils.data.ClaimedChunks;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.data.Universe;
import serverutils.lib.math.ChunkDimPos;

public class ServerUtilitiesCompat {

    @Optional.Method(modid = "serverutilities")
    public static boolean checkPermission(World world, EntityPlayer actor, int x, int z) {
        final ForgeTeam owningTeam = ClaimedChunks.instance
                .getChunkTeam(new ChunkDimPos(x >> 4, z >> 4, world.provider.dimensionId));
        if (owningTeam == null) {
            return true;
        }
        final ForgePlayer currentPlayer = new ForgePlayer(
                Universe.get(),
                actor.getUniqueID(),
                actor.getCommandSenderName());
        return owningTeam.isAlly(currentPlayer) || owningTeam.isMember(currentPlayer);
    }

}
