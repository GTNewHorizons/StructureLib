package com.gtnewhorizon.structurelib.block;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import java.util.List;

public class BlockHint extends Block {
	private static final IIcon[] hint = new IIcon[16];

	public BlockHint() {
		super(Material.iron);
		setBlockName("structurelib.blockhint");
		setCreativeTab(StructureLib.creativeTab);
	}

	@Override
	public void registerBlockIcons(IIconRegister aIconRegister) {
		//super.registerBlockIcons(aIconRegister);
		hint[0] = aIconRegister.registerIcon(StructureLibAPI.MOD_ID + ":iconsets/HINT_0");
		hint[1] = aIconRegister.registerIcon(StructureLibAPI.MOD_ID + ":iconsets/HINT_1");
		hint[2] = aIconRegister.registerIcon(StructureLibAPI.MOD_ID + ":iconsets/HINT_2");
		hint[3] = aIconRegister.registerIcon(StructureLibAPI.MOD_ID + ":iconsets/HINT_3");
		hint[4] = aIconRegister.registerIcon(StructureLibAPI.MOD_ID + ":iconsets/HINT_4");
		hint[5] = aIconRegister.registerIcon(StructureLibAPI.MOD_ID + ":iconsets/HINT_5");
		hint[6] = aIconRegister.registerIcon(StructureLibAPI.MOD_ID + ":iconsets/HINT_6");
		hint[7] = aIconRegister.registerIcon(StructureLibAPI.MOD_ID + ":iconsets/HINT_7");
		hint[8] = aIconRegister.registerIcon(StructureLibAPI.MOD_ID + ":iconsets/HINT_8");
		hint[9] = aIconRegister.registerIcon(StructureLibAPI.MOD_ID + ":iconsets/HINT_9");
		hint[10] = aIconRegister.registerIcon(StructureLibAPI.MOD_ID + ":iconsets/HINT_10");
		hint[11] = aIconRegister.registerIcon(StructureLibAPI.MOD_ID + ":iconsets/HINT_11");
		hint[12] = aIconRegister.registerIcon(StructureLibAPI.MOD_ID + ":iconsets/HINT_DEFAULT");
		hint[13] = aIconRegister.registerIcon(StructureLibAPI.MOD_ID + ":iconsets/HINT_AIR");
		hint[14] = aIconRegister.registerIcon(StructureLibAPI.MOD_ID + ":iconsets/HINT_NOAIR");
		hint[15] = aIconRegister.registerIcon(StructureLibAPI.MOD_ID + ":iconsets/HINT_ERROR");
	}

	@Override
	public IIcon getIcon(int aSide, int aMeta) {
		return hint[aMeta];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(IBlockAccess aWorld, int xCoord, int yCoord, int zCoord, int aSide) {
		int tMeta = aWorld.getBlockMetadata(xCoord, yCoord, zCoord);
		return getIcon(aSide, tMeta);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void getSubBlocks(Item aItem, CreativeTabs par2CreativeTabs, List aList) {
		for (int i = 0; i <= 15; i++) {
			aList.add(new ItemStack(aItem, 1, i));
		}
	}
}
