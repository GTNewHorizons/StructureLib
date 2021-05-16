package com.gtnewhorizon.structurelib.proxy;

import com.gtnewhorizon.structurelib.entity.fx.BlockHint;
import com.gtnewhorizon.structurelib.entity.fx.WeightlessParticleFX;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import static com.gtnewhorizon.structurelib.StructureLib.RANDOM;

public class ClientProxy extends CommonProxy {
	@Override
	public void hint_particle_tinted(World w, int x, int y, int z, IIcon[] icons, short[] RGBa) {
		Minecraft.getMinecraft().effectRenderer.addEffect(new BlockHint(w, x, y, z, icons).withColorTint(RGBa));
		createParticleFX(w, x, y, z);
	}

	@Override
	public void hint_particle_tinted(World w, int x, int y, int z, Block block, int meta, short[] RGBa) {
		Minecraft.getMinecraft().effectRenderer.addEffect(new BlockHint(w, x, y, z, block, meta).withColorTint(RGBa));
		createParticleFX(w, x, y, z);
	}

	@Override
	public void hint_particle(World w, int x, int y, int z, IIcon[] icons) {
		Minecraft.getMinecraft().effectRenderer.addEffect(new BlockHint(w, x, y, z, icons));
		createParticleFX(w, x, y, z);
	}

	@Override
	public void hint_particle(World w, int x, int y, int z, Block block, int meta) {
		Minecraft.getMinecraft().effectRenderer.addEffect(new BlockHint(w, x, y, z, block, meta));
		createParticleFX(w, x, y, z);
	}

	private static void createParticleFX(World w, int x, int y, int z) {
		EntityFX particle = new WeightlessParticleFX(w, x + RANDOM.nextFloat() * 0.5F, y + RANDOM.nextFloat() * 0.5F, z + RANDOM.nextFloat() * 0.5F, 0, 0, 0);
		particle.setRBGColorF(0, 0.6F * RANDOM.nextFloat(), 0.8f);
		Minecraft.getMinecraft().effectRenderer.addEffect(particle);
	}
}
