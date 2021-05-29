package com.gtnewhorizon.structurelib;

import com.gtnewhorizon.structurelib.alignment.AlignmentMessage;
import com.gtnewhorizon.structurelib.alignment.IAlignmentProvider;
import cpw.mods.fml.common.network.NetworkRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import static com.gtnewhorizon.structurelib.StructureLib.proxy;

public class StructureLibAPI {
	public static void hintParticleTinted(World w, int x, int y, int z, IIcon[] icons, short[] RGBa) {
		proxy.hintParticleTinted(w, x, y, z, icons, RGBa);
	}

	public static void hintParticleTinted(World w, int x, int y, int z, Block block, int meta, short[] RGBa) {
		proxy.hintParticleTinted(w, x, y, z, block, meta, RGBa);
	}

	public static void hintParticle(World w, int x, int y, int z, IIcon[] icons) {
		proxy.hintParticle(w, x, y, z, icons);
	}

	public static void hintParticle(World w, int x, int y, int z, Block block, int meta) {
		proxy.hintParticle(w, x, y, z, block, meta);
	}

	public static void queryAlignment(IAlignmentProvider provider) {
		StructureLib.net.sendToServer(new AlignmentMessage.AlignmentQuery(provider));
	}

	public static void sendAlignment(IAlignmentProvider provider) {
		StructureLib.net.sendToAll(new AlignmentMessage.AlignmentData(provider));
	}

	public static void sendAlignment(IAlignmentProvider provider, EntityPlayerMP player) {
		StructureLib.net.sendTo(new AlignmentMessage.AlignmentData(provider), player);
	}

	public static void sendAlignment(IAlignmentProvider provider, NetworkRegistry.TargetPoint targetPoint) {
		StructureLib.net.sendToAllAround(new AlignmentMessage.AlignmentData(provider), targetPoint);
	}

	public static void sendAlignment(IAlignmentProvider provider, World dimension) {
		StructureLib.net.sendToDimension(new AlignmentMessage.AlignmentData(provider), dimension.provider.dimensionId);
	}

	public static Block getBlockHint() {
		return StructureLib.blockHint;
	}

	public static Item getItemBlockHint() {
		return StructureLib.itemBlockHint;
	}
}
