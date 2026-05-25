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

public class ItemConstructableTrigger extends Item {

    public ItemConstructableTrigger() {
        setUnlocalizedName("structurelib.constructableTrigger");
        setTextureName(MOD_ID + ":itemConstructableTrigger");
        setCreativeTab(StructureLib.creativeTab);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (getMovingObjectPositionFromPlayer(world, player, true) == null) {
            // channel gui is opened from server and config gui from client
            if (!world.isRemote && !player.isSneaking()) {
                player.openGui(StructureLib.instance(), 0, world, player.inventory.currentItem, 0, 0);
            } else if (world.isRemote && player.isSneaking()) {
                StructureLib.instance().proxy().displayConfigGUI("registries");
            }
        }
        return super.onItemRightClick(stack, world, player);
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
        return ConstructableUtility.handle(stack, player, world, x, y, z, side);
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean adv) {
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
            tooltip.add(
                    translateToLocalFormatted(
                            "item.structurelib.constructableTrigger.desc.lshift.0",
                            ChannelDataAccessor.countChannelData(stack)));
            ChannelDataAccessor.iterateChannelData(stack).map(e -> e.getKey() + ": " + e.getValue())
                    .forEach(tooltip::add);
        } else {
            // Tooltip text:
            //   Triggers Constructable Interface
            //   Shows multiblock construction details,
            //   just Use on a multiblock controller.
            //   (Sneak Use in creative to build)
            //   Quantity affects tier/mode/type

            tooltip.add(translateToLocal("item.structurelib.constructableTrigger.desc.0"));
            tooltip.add(BLUE + translateToLocal("item.structurelib.constructableTrigger.desc.1"));
            tooltip.add(BLUE + translateToLocal("item.structurelib.constructableTrigger.desc.2"));
            tooltip.add(BLUE + translateToLocal("item.structurelib.constructableTrigger.desc.3"));
            tooltip.add(BLUE + translateToLocal("item.structurelib.constructableTrigger.desc.4"));
            if (ChannelDataAccessor.hasSubChannel(stack))
                tooltip.add(translateToLocal("item.structurelib.constructableTrigger.desc.5"));
        }
    }
}
