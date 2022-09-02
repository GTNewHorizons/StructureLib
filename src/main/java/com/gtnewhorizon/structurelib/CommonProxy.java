package com.gtnewhorizon.structurelib;

import com.gtnewhorizon.structurelib.net.ErrorHintParticleMessage;
import com.gtnewhorizon.structurelib.net.UpdateHintParticleMessage;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

public class CommonProxy {
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
        if (player instanceof EntityPlayerMP) { // just in case
            StructureLib.net.sendTo(new ErrorHintParticleMessage(x, (short) y, z), (EntityPlayerMP) player);
            return true;
        } else {
            return false;
        }
    }

    private final Map<EntityPlayerMP, Map<Object, Long>> throttleMap = new WeakHashMap<>();

    public void addThrottledChat(
            Object throttleKey,
            EntityPlayer player,
            IChatComponent text,
            short intervalRequired,
            boolean forceUpdateLastSend) {
        if (player instanceof EntityPlayerMP) {
            Map<Object, Long> submap = throttleMap.computeIfAbsent((EntityPlayerMP) player, p -> new HashMap<>());
            addThrottledChat(throttleKey, player, text, intervalRequired, forceUpdateLastSend, submap);
        }
    }

    protected static void addThrottledChat(
            Object throttleKey,
            EntityPlayer player,
            IChatComponent text,
            short intervalRequired,
            boolean forceUpdateLastSend,
            Map<Object, Long> submap) {
        long now = System.currentTimeMillis();
        Long old;
        if (forceUpdateLastSend) {
            old = submap.put(throttleKey, now);
        } else {
            old = submap.get(throttleKey);
        }
        if (old == null || now - old >= intervalRequired) {
            player.addChatComponentMessage(text);
            if (!forceUpdateLastSend) submap.put(throttleKey, now);
        }
    }
}
