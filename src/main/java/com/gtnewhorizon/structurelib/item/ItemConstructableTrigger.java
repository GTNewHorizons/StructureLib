package com.gtnewhorizon.structurelib.item;

import static com.gtnewhorizon.structurelib.StructureLibAPI.MOD_ID;
import static net.minecraft.util.EnumChatFormatting.BLUE;
import static net.minecraft.util.StatCollector.translateToLocal;
import static net.minecraft.util.StatCollector.translateToLocalFormatted;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.constructable.ConstructableUtility;
import com.gtnewhorizon.structurelib.gui.GuiScreenConfigureChannels;

import cpw.mods.fml.common.FMLCommonHandler;

public class ItemConstructableTrigger extends Item {

    public ItemConstructableTrigger() {
        setUnlocalizedName("structurelib.constructableTrigger");
        setTextureName(MOD_ID + ":itemConstructableTrigger");
        setCreativeTab(StructureLib.creativeTab);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote && getMovingObjectPositionFromPlayer(world, player, true) == null) {
            if (player.isSneaking()) {
                StructureLib.instance().proxy().displayConfigGUI("registries");
            } else {
                FMLCommonHandler.instance().showGuiScreen(new GuiScreenConfigureChannels(stack));
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
