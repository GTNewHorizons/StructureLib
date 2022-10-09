package com.gtnewhorizon.structurelib.item;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.gtnewhorizon.structurelib.util.Box;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import static com.gtnewhorizon.structurelib.StructureLibAPI.MOD_ID;
import static com.gtnewhorizon.structurelib.StructureLibAPI.getBlockHint;

public class ItemDebugStructureWriter extends Item {
    public enum Usage {
        SetCorners, SetCenter, Clear;

        public static Usage getUsageFromOrdinal(int ordinal) {
            Usage value = null;
            switch (ordinal) {
                case 0:
                    value = SetCorners;
                    break;
                case 1:
                    value = SetCenter;
                    break;
                case 2:
                    value = Clear;
                    break;
            }
            return value;
        }
    }

    private Usage mode = Usage.SetCorners;
    private final Vec3Impl[] corners = new Vec3Impl[2];
    private Vec3Impl center = null;
    private int index = 0;

    private Box box;

    public ItemDebugStructureWriter() {
        setMaxStackSize(1);
        setUnlocalizedName("structurelib.debugStructureWriter");
        setTextureName(MOD_ID + ":itemDebugStructureWriter");
        setCreativeTab(StructureLib.creativeTab);

        box = null;
    }

    public Usage mode() {
        return this.mode;
    }

    public void mode(Usage mode) {
        this.mode = mode;
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (player.isSneaking() == false) {
            doStuff(world, new Vec3Impl(x, y, z));
        }
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        if (player.worldObj.isRemote) {
            return itemStack;
        }

        if (player.isSneaking() == false) {
                doStuff(world, new Vec3Impl((int) Math.floor(player.posX),
                                            (int) Math.floor(player.posY),
                                            (int) Math.floor(player.posZ)));
        } else {
            writeStructure(player);
        }

        return itemStack;
    }

    private void doStuff(World world, Vec3Impl pos) {
        switch (this.mode) {
            case SetCorners:
                corners[index] = pos;

                StructureLibAPI.hintParticle(world, corners[index].get0(), corners[index].get1(), corners[index].get2(), getBlockHint(), index);

                index = (index + 1) % 2;

                tryMakeAndDrawBox(world);
                break;
            case SetCenter:
                center = pos;
                break;
            case Clear:
                StructureLib.proxy.clearHints(world);
                corners[0] = null;
                corners[1] = null;
                center = null;
                index = 0;
                break;
        }
    }

    private void tryMakeAndDrawBox(World world) {
        if (corners[0] != null && corners[1] != null) {
            this.box = new Box(corners[0], corners[1]);

            box.drawBoundingBox(world);
        }
    }

    private void writeStructure(EntityPlayer player) {
        if (corners[0] != null && corners[1] != null) {
            ExtendedFacing facing = StructureUtility.getExtendedFacingFromLookVector(player.getLookVec());

            String structureDefinition = StructureUtility.getPseudoJavaCode(player.getEntityWorld(), facing, box, false);

            StructureLib.LOGGER.info(structureDefinition);
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack itemStack) {
        String name = "";
        switch (itemStack.getItemDamage()) {
            case 0:
                name = StatCollector.translateToLocal("item.structurelib.debugStructureWriter.mode.0");
                break;
            case 1:
                name = StatCollector.translateToLocal("item.structurelib.debugStructureWriter.mode.1");
                break;
            case 2:
                name = StatCollector.translateToLocal("item.structurelib.debugStructureWriter.mode.2");
                break;
        }
        return name;
    }
}
