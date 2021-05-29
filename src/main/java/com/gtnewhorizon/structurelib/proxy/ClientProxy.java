package com.gtnewhorizon.structurelib.proxy;

import com.gtnewhorizon.structurelib.entity.fx.BlockHint;
import com.gtnewhorizon.structurelib.entity.fx.WeightlessParticleFX;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import static com.gtnewhorizon.structurelib.StructureLib.RANDOM;

public class ClientProxy extends CommonProxy {
	@Override
	public void hintParticleTinted(World w, int x, int y, int z, IIcon[] icons, short[] RGBa) {
		Minecraft.getMinecraft().effectRenderer.addEffect(new BlockHint(w, x, y, z, icons).withColorTint(RGBa));

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
			block.getIcon(i, meta);
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
	public boolean isCurrentPlayer(EntityPlayer player) {
		return player == Minecraft.getMinecraft().thePlayer;
	}
}
