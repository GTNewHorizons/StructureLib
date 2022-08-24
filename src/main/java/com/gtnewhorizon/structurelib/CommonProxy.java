package com.gtnewhorizon.structurelib;

import com.gtnewhorizon.structurelib.net.UpdateHintParticleMessage;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

public class CommonProxy {

    private static final short[] RGBA_RED_TINT = {255, 128, 128, 0};

    public void hintParticleTinted(World w, int x, int y, int z, IIcon[] icons, short[] RGBa) {}

    public void hintParticleTinted(World w, int x, int y, int z, Block block, int meta, short[] RGBa) {}

    public void hintParticle(World w, int x, int y, int z, IIcon[] icons) {}

    public void hintParticle(World w, int x, int y, int z, Block block, int meta) {}

    public boolean updateHintParticleTint(EntityPlayer player, World w, int x, int y, int z, short[] rgBa) {
        if (player instanceof EntityPlayerMP) { // just in case
            StructureLib.net.sendTo(
                    new UpdateHintParticleMessage(x, (short) y, z, rgBa[0], rgBa[1], rgBa[2], rgBa[3]),
                    (EntityPlayerMP) player);
            return true;
        } else {
            return false;
        }
    }

    public EntityPlayer getCurrentPlayer() {
        return null;
    }

    public boolean isCurrentPlayer(EntityPlayer player) {
        return false;
    }

    public void addClientSideChatMessages(String... messages) {}

    public void startHinting(World w) {}

    public void endHinting(World w) {}

    public void preInit(FMLPreInitializationEvent e) {}

    public long getOverworldTime() {
        return MinecraftServer.getServer().getEntityWorld().getTotalWorldTime();
    }

    public void uploadChannels(ItemStack trigger) {}

    public boolean markHintParticleError(EntityPlayer player, World w, int x, int y, int z) {
        return updateHintParticleTint(player, w, x, y, z, RGBA_RED_TINT);
    }
}
