package com.gtnewhorizon.structurelib.proxy;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.gtnewhorizon.structurelib.ConfigurationHandler;
import com.gtnewhorizon.structurelib.entity.fx.EntityFXBlockHint;
import com.gtnewhorizon.structurelib.entity.fx.WeightlessParticleFX;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static com.gtnewhorizon.structurelib.StructureLib.RANDOM;

public class ClientProxy extends CommonProxy {
    @Override
    public void hintParticleTinted(World w, int x, int y, int z, IIcon[] icons, short[] RGBa) {
        EntityFXBlockHint hint = new EntityFXBlockHint(w, x, y, z, icons).withColorTint(RGBa);
        Minecraft.getMinecraft().effectRenderer.addEffect(hint);
        ensureHinting();
        if (ConfigurationHandler.INSTANCE.isRemoveCollidingHologram()) {
            HintParticleInfo info = new HintParticleInfo(x, y, z, currentHints);
            EntityFXBlockHint dupe = allHints.inverse().get(info);
            if (dupe != null) {
                List<EntityFXBlockHint> owner = allHints.get(dupe).owner;
                // in case someone forget to call endHinting
                if (owner != currentHints) {
                    hintOwners.remove(owner);
                    owner.forEach(EntityFXBlockHint::setDead);
                    owner.clear();
                }
            }
            allHints.forcePut(hint, info);
        }
        currentHints.add(hint);

        EntityFX particle = new WeightlessParticleFX(w, x + RANDOM.nextFloat() * 0.5F, y + RANDOM.nextFloat() * 0.5F, z + RANDOM.nextFloat() * 0.5F, 0, 0, 0);
        particle.setRBGColorF(0, 0.6F * RANDOM.nextFloat(), 0.8f);
        Minecraft.getMinecraft().effectRenderer.addEffect(particle);
    }

    @Override
    public void hintParticleTinted(World w, int x, int y, int z, Block block, int meta, short[] RGBa) {
        hintParticleTinted(w, x, y, z, createIIconFromBlock(block, meta), RGBa);
    }

    @Override
    public void hintParticle(World w, int x, int y, int z, IIcon[] icons) {
        hintParticleTinted(w, x, y, z, icons, new short[]{255, 255, 255, 0});
    }

    @Override
    public void hintParticle(World w, int x, int y, int z, Block block, int meta) {
        hintParticleTinted(w, x, y, z, createIIconFromBlock(block, meta), new short[]{255, 255, 255, 0});
    }

    private static IIcon[] createIIconFromBlock(Block block, int meta) {
        IIcon[] ret = new IIcon[6];
        for (int i = 0; i < 6; i++) {
            ret[i] = block.getIcon(i, meta);
        }
        return ret;
    }

    @Override
    public void addClientSideChatMessages(String... messages) {
        GuiNewChat chat = Minecraft.getMinecraft().ingameGUI.getChatGUI();
        for (String s : messages) {
            chat.printChatMessage(new ChatComponentText(s));
        }
    }

    @Override
    public EntityPlayer getCurrentPlayer() {
        return Minecraft.getMinecraft().thePlayer;
    }

    @Override
    public boolean isCurrentPlayer(EntityPlayer player) {
        return player == Minecraft.getMinecraft().thePlayer;
    }

    private static final BiMap<EntityFXBlockHint, HintParticleInfo> allHints = HashBiMap.create();
    private static final List<List<EntityFXBlockHint>> hintOwners = new ArrayList<>();
    private static List<EntityFXBlockHint> currentHints;

    public static void onHintDead(EntityFXBlockHint fx) {
        if (ConfigurationHandler.INSTANCE.isRemoveCollidingHologram())
            allHints.remove(fx);
        for (Iterator<List<EntityFXBlockHint>> iterator = hintOwners.iterator(); iterator.hasNext(); ) {
            List<EntityFXBlockHint> list = iterator.next();
            if (list.remove(fx)) {
                if (list.isEmpty())
                    iterator.remove();
                break;
            }
        }
    }

    @Override
    public void startHinting(World w) {
        if (!w.isRemote)
            return;
        if (currentHints != null)
            hintOwners.add(currentHints);
        currentHints = new LinkedList<>();
    }

    private void ensureHinting() {
        if (currentHints == null)
            currentHints = new LinkedList<>();
    }

    @Override
    public void endHinting(World w) {
        if (!w.isRemote)
            return;
        while (!hintOwners.isEmpty() && hintOwners.size() >= ConfigurationHandler.INSTANCE.getMaxCoexistingHologram()) {
            List<EntityFXBlockHint> list = hintOwners.remove(0);
            list.forEach(EntityFXBlockHint::setDead);
            list.clear();
        }
        if (!currentHints.isEmpty())
            hintOwners.add(currentHints);
        currentHints = null;
    }

    @Override
    public void preInit(FMLPreInitializationEvent e) {
        MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());
    }

    private static class HintParticleInfo {
        private final int x, y, z;
        private final List<EntityFXBlockHint> owner;

        public HintParticleInfo(int x, int y, int z, List<EntityFXBlockHint> owner) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.owner = owner;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            HintParticleInfo that = (HintParticleInfo) o;

            if (x != that.x) return false;
            if (y != that.y) return false;
            return z == that.z;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            result = 31 * result + z;
            return result;
        }
    }

    public static class ForgeEventHandler {
        @SubscribeEvent
        public void onWorldLoad(WorldEvent.Load e) {
            if (e.world.isRemote) {
                allHints.clear();
                hintOwners.clear();
            }
        }
    }
}
