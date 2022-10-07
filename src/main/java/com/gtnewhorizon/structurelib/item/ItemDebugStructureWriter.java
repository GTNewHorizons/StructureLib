package com.gtnewhorizon.structurelib.item;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.gtnewhorizon.structurelib.util.Box;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import static com.gtnewhorizon.structurelib.StructureLibAPI.MOD_ID;
import static com.gtnewhorizon.structurelib.StructureLibAPI.getBlockHint;

public class ItemDebugStructureWriter extends Item {
    private final Vec3Impl[] corners = new Vec3Impl[2];
    private int index = 0;

    private Box box;
    public ItemDebugStructureWriter() {
        setMaxStackSize(1);
        setUnlocalizedName("structurelib.debugStructureWriter");
        setTextureName(MOD_ID + ":itemDebugStructureWriter");
        setCreativeTab(StructureLib.creativeTab);

        box = null;
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (player.isSneaking() == false) {
            corners[index] = new Vec3Impl(x, y, z);

            StructureLibAPI.hintParticle(player.worldObj, corners[index].get0(), corners[index].get1(), corners[index].get2(), getBlockHint(), index);

            index = (index + 1) % 2;

            this.tryMakeAndDrawBox(player.worldObj);
        }
        return true;
    }

    private void tryMakeAndDrawBox(World world) {
        if (corners[0] != null && corners[1] != null) {
            this.box = new Box(corners[0], corners[1]);

            box.drawBoundingBox(world);
        }
    }

    @Override
    public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        if (player.worldObj.isRemote) {
            return itemStack;
        }

        if (!player.isSneaking()) {
                corners[index] = new Vec3Impl((int) Math.floor(player.posX), (int) Math.floor(player.posY), (int) Math.floor(player.posZ));

                StructureLibAPI.hintParticle(player.worldObj, corners[index].get0(), corners[index].get1(), corners[index].get2(), getBlockHint(), index);

                index = (index + 1) % 2;

                tryMakeAndDrawBox(world);
        } else {
            writeStructure(player);
        }

        return itemStack;
    }

    private void writeStructure(EntityPlayer player) {
        if (corners[0] != null && corners[1] != null) {
            ExtendedFacing facing = StructureUtility.getExtendedFacingFromLookVector(player.getLookVec());

            Vec3Impl basePosition = StructureUtility.getBasePos(box, facing);

            String structureDefinition = StructureUtility.getPseudoJavaCode(player.getEntityWorld(),
                                                                            facing,
                                                                            basePosition.get0(),
                                                                            basePosition.get1(),
                                                                            basePosition.get2(),
                                                                            0,
                                                                            0,
                                                                            0,
                                                                            te -> te.getClass().getCanonicalName(),
                                                                            box.xSize(),
                                                                            box.ySize(),
                                                                            box.zSize(),
                                                                            false);

            StructureLib.LOGGER.info(structureDefinition);
        }
    }
}
