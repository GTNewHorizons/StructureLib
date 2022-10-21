package com.gtnewhorizon.structurelib.item;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.gtnewhorizon.structurelib.util.StructureData;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import java.util.List;

import static com.gtnewhorizon.structurelib.StructureLibAPI.MOD_ID;
import static com.gtnewhorizon.structurelib.StructureLibAPI.getBlockHint;

public class ItemDebugStructureWriter extends Item {
    public enum Usage {
        SetCorners, SetController, Build, Refresh, Clear
    }

    private Usage mode = Usage.SetCorners;

    StructureData data = new StructureData();

    @SideOnly(Side.CLIENT)
    private IIcon eraser;

    public ItemDebugStructureWriter() {
        setMaxStackSize(1);
        setUnlocalizedName("structurelib.debugStructureWriter");
        setTextureName(MOD_ID + ":itemDebugStructureWriter");
        setHasSubtypes(true);
        setCreativeTab(StructureLib.creativeTab);

        this.data.box(null);
    }

    public Usage mode() {
        return this.mode;
    }

    public void mode(Usage mode) {
        this.mode = mode;
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        Vec3Impl pos = new Vec3Impl(x, y, z);
        doStuff(world, pos, player);

        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        if (player.worldObj.isRemote) {
            return itemStack;
        }
        Vec3Impl pos = new Vec3Impl((int) Math.floor(player.posX),
                                    (int) Math.floor(player.posY),
                                    (int) Math.floor(player.posZ));

        doStuff(world, pos, player);

        return itemStack;
    }

    private void doStuff(World world, Vec3Impl pos, EntityPlayer player) {
        int index = player.isSneaking() ? 1 : 0;

        switch (this.mode) {
            case SetCorners:
                this.data.corners(index, pos, world);

                Vec3Impl hintPos = this.data.corners()[index];
                StructureLibAPI.hintParticle(world, hintPos.get0(), hintPos.get1(), hintPos.get2(), getBlockHint(), index);

            case Refresh:
                if (data.box() != null) {
                    data.box().drawBoundingBox(world);
                }
                break;
            case SetController:
                this.data.controller(pos);
                break;
            case Clear:
                this.data.reset();
                break;
            case Build:
                writeStructure(player);
                break;
        }
    }

    private void writeStructure(EntityPlayer player) {
        if (this.data.cornersSet() && this.data.controller() != null) {
            ExtendedFacing facing = StructureUtility.getExtendedFacingFromLookVector(player.getLookVec());

            String structureDefinition = StructureUtility.getPseudoJavaCode(player.getEntityWorld(),
                                                                            facing,
                                                                            this.data.box(),
                                                                            this.data.controller() != null ? this.data.controller() : Vec3Impl.NULL_VECTOR,
                                                                            player.isSneaking());

            StructureLib.LOGGER.info(structureDefinition);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIconFromDamage(int damage)
    {
        return damage == Usage.Clear.ordinal() ? this.eraser : super.getIconFromDamage(damage);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerIcons(IIconRegister iconRegister)
    {
        super.registerIcons(iconRegister);
        this.eraser = iconRegister.registerIcon(MOD_ID + ":itemDebugStructureEraser");
    }

    @Override
    public String getItemStackDisplayName(ItemStack itemStack) {
        return super.getItemStackDisplayName(itemStack) + " (" + StatCollector.translateToLocal("item.structurelib.debugStructureWriter.mode." + this.mode.ordinal()) + ")";
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack itemStack, EntityPlayer player, List description, boolean p_77624_4_) {
        String mode = EnumChatFormatting.DARK_AQUA +
                      StatCollector.translateToLocal("item.structurelib.debugStructureWriter.desc.0") +
                      " (" + StatCollector.translateToLocal("item.structurelib.debugStructureWriter.mode." + this.mode.ordinal()) + ")";
        description.add(mode);

        switch (this.mode) {
            case SetCorners:
                description.add(StatCollector.translateToLocal("item.structurelib.debugStructureWriter.desc.1"));
                description.add(StatCollector.translateToLocal("item.structurelib.debugStructureWriter.desc.2"));
                description.add(StatCollector.translateToLocal("item.structurelib.debugStructureWriter.desc.3"));
                description.add("");
                description.add(StatCollector.translateToLocal("item.structurelib.debugStructureWriter.desc.4"));
                description.add(StatCollector.translateToLocal("item.structurelib.debugStructureWriter.desc.5"));
                break;
            case SetController:
                description.add(StatCollector.translateToLocal("item.structurelib.debugStructureWriter.desc.6"));
                description.add("");
                description.add(StatCollector.translateToLocal("item.structurelib.debugStructureWriter.desc.4"));
                description.add(StatCollector.translateToLocal("item.structurelib.debugStructureWriter.desc.5"));
                break;
            case Clear:
                description.add(StatCollector.translateToLocal("item.structurelib.debugStructureWriter.desc.7"));
                break;
            case Build:
                description.add(StatCollector.translateToLocal("item.structurelib.debugStructureWriter.desc.8"));
                description.add(StatCollector.translateToLocal("item.structurelib.debugStructureWriter.desc.9"));
                description.add("");
                description.add(StatCollector.translateToLocal("item.structurelib.debugStructureWriter.desc.10"));
                description.add(StatCollector.translateToLocal("item.structurelib.debugStructureWriter.desc.11"));
                break;
            case Refresh:
                description.add(StatCollector.translateToLocal("item.structurelib.debugStructureWriter.desc.13"));
                break;
        }
        description.add("");
        description.add(StatCollector.translateToLocal("item.structurelib.debugStructureWriter.desc.12"));
    }
}
