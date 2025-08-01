package com.gtnewhorizon.structurelib.item;

import static com.gtnewhorizon.structurelib.StructureLibAPI.MOD_ID;
import static net.minecraft.util.EnumChatFormatting.BLUE;
import static net.minecraft.util.StatCollector.translateToLocal;
import static net.minecraft.util.StatCollector.translateToLocalFormatted;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.constructable.ConstructableUtility;

public class ItemConstructableTrigger extends ModeToggleableItem {

    private static final IIcon[] textures = new IIcon[3];

    public ItemConstructableTrigger() {
        setUnlocalizedName("structurelib.constructableTrigger");
        setCreativeTab(StructureLib.creativeTab);
    }

    @Override
    public void registerIcons(IIconRegister register) {
        textures[0] = register.registerIcon(MOD_ID + ":constructableTrigger/BUILDING");
        textures[1] = register.registerIcon(MOD_ID + ":constructableTrigger/UPDATING");
        textures[2] = register.registerIcon(MOD_ID + ":constructableTrigger/REMOVING");
    }

    @Override
    public IIcon getIcon(ItemStack stack, int renderPass, EntityPlayer player, ItemStack usingItem, int useRemaining) {
        return getIcon(stack, renderPass);
    }

    @Override
    public IIcon getIcon(ItemStack stack, int pass) {
        if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey(TAG_MODE)) {
            return textures[0];
        }
        return textures[stack.getTagCompound().getInteger(TAG_MODE)];
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey(TAG_MODE)) {
            return getDisplayName(0);
        }
        return getDisplayName(stack.getTagCompound().getInteger(TAG_MODE));
    }

    @Override
    public IIcon getIconIndex(ItemStack stack) {
        return getIcon(stack, 0);
    }

    @Override
    public IIcon getIconFromDamage(int damage) {
        return textures[damage];
    }

    private String getDisplayName(int mode) {
        return StatCollector.translateToLocal("item.structurelib.constructableTrigger.name") + " - "
                + StatCollector.translateToLocal("item.structurelib.constructableTrigger.modes." + mode);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote && getMovingObjectPositionFromPlayer(world, player, true) == null) {
            if (player.isSneaking()) {
                StructureLib.instance().proxy().displayConfigGUI("registries");
            } else {
                player.openGui(StructureLib.instance(), 0, world, player.inventory.currentItem, 0, 0);
            }
        }
        return super.onItemRightClick(stack, world, player);
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
        return ConstructableUtility.handle(stack, player, world, x, y, z, side);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addInformation(ItemStack aStack, EntityPlayer ep, List aList, boolean boo) {
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
            aList.add(
                    translateToLocalFormatted(
                            "item.structurelib.constructableTrigger.desc.lshift.0",
                            ChannelDataAccessor.countChannelData(aStack)));
            ChannelDataAccessor.iterateChannelData(aStack).map(e -> e.getKey() + ": " + e.getValue())
                    .forEach(aList::add);
        } else {
            aList.add(translateToLocal("item.structurelib.constructableTrigger.desc.0")); // Triggers Constructable
                                                                                          // Interface
            aList.add(BLUE + translateToLocal("item.structurelib.constructableTrigger.desc.1")); // Shows multiblock
                                                                                                 // construction
                                                                                                 // details,
            aList.add(BLUE + translateToLocal("item.structurelib.constructableTrigger.desc.2")); // just Use on a
                                                                                                 // multiblock
                                                                                                 // controller.
            aList.add(BLUE + translateToLocal("item.structurelib.constructableTrigger.desc.3")); // (Sneak Use in
                                                                                                 // creative to build)
            aList.add(BLUE + translateToLocal("item.structurelib.constructableTrigger.desc.4")); // Quantity affects
                                                                                                 // tier/mode/type
            if (ChannelDataAccessor.hasSubChannel(aStack))
                aList.add(translateToLocal("item.structurelib.constructableTrigger.desc.5"));
        }
    }
}
