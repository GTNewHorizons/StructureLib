package com.gtnewhorizon.structurelib.item;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.gtnewhorizon.structurelib.util.Box;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import java.util.List;

import static com.gtnewhorizon.structurelib.StructureLibAPI.MOD_ID;
import static com.gtnewhorizon.structurelib.StructureLibAPI.getBlockHint;

public class ItemDebugStructureWriter extends Item {
    public enum Mode {
        SetCorners, SetController, Build, Refresh, Clear
    }

    @SideOnly(Side.CLIENT)
    private IIcon eraserIcon;

    private static final String TAG_POS_1 = "pos1";
    private static final String TAG_POS_2 = "pos2";
    private static final String TAG_POS_CONTROLLER = "pos3";
    private static final String TAG_MODE = "mode";

    public ItemDebugStructureWriter() {
        setMaxStackSize(1);
        setUnlocalizedName("structurelib.debugStructureWriter");
        setTextureName(MOD_ID + ":itemDebugStructureWriter");
        setHasSubtypes(true);
        setCreativeTab(StructureLib.creativeTab);
    }

    public static void checkNBT(ItemStack itemStack) {
        if (!itemStack.hasTagCompound()) {
            itemStack.setTagCompound(new NBTTagCompound());
        }
    }

    public static Mode readModeFromNBT(ItemStack itemStack) {
        checkNBT(itemStack);
        return Mode.values()[itemStack.stackTagCompound.getByte(TAG_MODE)];
    }

    public static void writeModeToNBT(ItemStack itemStack, Mode mode) {
        checkNBT(itemStack);
        itemStack.getTagCompound().setByte(TAG_MODE, (byte) mode.ordinal());
    }

    public static Vec3Impl readPosFromNBT(ItemStack itemStack, String NBTTag) {
        checkNBT(itemStack);

        int[] components = itemStack.getTagCompound().getIntArray(NBTTag);

        return components.length == 3 ? new Vec3Impl(components) : null;
    }

    public static void writePosToNBT(ItemStack itemStack, Vec3Impl pos, String NBTTag) {
        checkNBT(itemStack);
        itemStack.getTagCompound().setIntArray(NBTTag, pos.components());
    }

    @Override
    public boolean onItemUseFirst(ItemStack itemStack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        Vec3Impl pos = new Vec3Impl(x, y, z);
        doStuff(itemStack, world, pos, player);
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        Mode mode = readModeFromNBT(itemStack);
        switch (mode) {
            case Build:
            case Clear:
            case Refresh:
                doStuff(itemStack, world, Vec3Impl.NULL_VECTOR, player);
        }
        return itemStack;
    }

    private void doStuff(ItemStack itemStack, World world, Vec3Impl pos, EntityPlayer player) {
        int index = player.isSneaking() ? 1 : 0;

        Mode mode = readModeFromNBT(itemStack);

        switch (mode) {
            case SetCorners:
                writePosToNBT(itemStack, pos, index == 0 ? TAG_POS_1 : TAG_POS_2);

                if (world.isRemote) {
                    StructureLibAPI.hintParticleTinted(world, pos.get0(), pos.get1(), pos.get2(), getBlockHint(), index, new short[]{255, 0, 255});
                }
            case Refresh:
                Vec3Impl pos1 = readPosFromNBT(itemStack, TAG_POS_1);
                Vec3Impl pos2 = readPosFromNBT(itemStack, TAG_POS_2);

                if (pos1 != null && pos2 != null) {
                    Box box = new Box(pos1, pos2);
                    box.drawBoundingBox(world);
                }
                break;
            case SetController:
                writePosToNBT(itemStack, pos, TAG_POS_CONTROLLER);
                break;
            case Build:
                writeStructure(itemStack, player);
                break;
            case Clear:
                itemStack.setTagCompound(null);
                writeModeToNBT(itemStack, mode);
                StructureLib.proxy.clearHints(world);
                break;
        }
    }

    private void writeStructure(ItemStack itemStack, EntityPlayer player) {
        Vec3Impl pos1 = readPosFromNBT(itemStack, TAG_POS_1);
        Vec3Impl pos2 = readPosFromNBT(itemStack, TAG_POS_2);
        Vec3Impl posController = readPosFromNBT(itemStack, TAG_POS_CONTROLLER);

        if (pos1 == null || pos2 == null || posController == null) {
            return;
        }

        Box box = new Box(pos1, pos2);

        ExtendedFacing facing = StructureUtility.getExtendedFacingFromLookVector(player.getLookVec());

        String structureDefinition = StructureUtility.getPseudoJavaCode(player.getEntityWorld(),
                facing,
                box,
                posController,
                player.isSneaking());

        StructureLib.LOGGER.info(structureDefinition);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerIcons(IIconRegister iconRegister) {
        super.registerIcons(iconRegister);
        this.eraserIcon = iconRegister.registerIcon(MOD_ID + ":itemDebugStructureEraser");
    }

    @Override
    public IIcon getIconIndex(ItemStack itemStack) {
        return readModeFromNBT(itemStack) != Mode.Clear ? this.itemIcon : this.eraserIcon;
    }

    @Override
    public IIcon getIconFromDamage(int damage) {
        Mode mode = Mode.values()[damage];

        return mode != Mode.Clear ? this.itemIcon : this.eraserIcon;
    }

    @Override
    public String getItemStackDisplayName(ItemStack itemStack) {
        return String.format("%s (%s)", super.getItemStackDisplayName(itemStack),
                StatCollector.translateToLocal("item.structurelib.debugStructureWriter.mode." + readModeFromNBT(itemStack).ordinal()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack itemStack, EntityPlayer player, List description, boolean p_77624_4_) {
        Mode mode = readModeFromNBT(itemStack);

        String modeString = String.format("%s (%s)", EnumChatFormatting.DARK_AQUA + StatCollector.translateToLocal("item.structurelib.debugStructureWriter.desc.0"),
                StatCollector.translateToLocal("item.structurelib.debugStructureWriter.mode." + mode.ordinal()));
        description.add(modeString);

        switch (mode) {
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
            case Clear:
                description.add(StatCollector.translateToLocal("item.structurelib.debugStructureWriter.desc.7"));
                break;
        }
        description.add("");
        description.add(StatCollector.translateToLocal("item.structurelib.debugStructureWriter.desc.12"));
    }
}
