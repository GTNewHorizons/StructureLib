package com.gtnewhorizon.structurelib.item;

import static com.gtnewhorizon.structurelib.StructureLibAPI.MOD_ID;
import static net.minecraft.util.EnumChatFormatting.BLUE;
import static net.minecraft.util.StatCollector.translateToLocal;
import static net.minecraft.util.StatCollector.translateToLocalFormatted;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;

import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.dismantle.DismantleManager;

/** Upgraded hologram projector. The original item retains its unchanged Build-only interactions. */
public class ItemConstructableTriggerV2 extends ItemConstructableTrigger {

    public ItemConstructableTriggerV2() {
        setUnlocalizedName("structurelib.constructableTriggerMKII");
        // Reuse the original icon until the upgraded projector gets its own art.
        setTextureName(MOD_ID + ":itemConstructableTrigger");
        setMaxStackSize(1);
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return ProjectorMode.get(stack) == ProjectorMode.DISMANTLE ? EnumAction.bow : EnumAction.none;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        // Rendering only: the actual hold duration is still enforced server-side.
        return ProjectorMode.get(stack) == ProjectorMode.DISMANTLE ? 72000 : 0;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        // The V2's new actions are driven by client input and validated on the server.
        // In unlinked Build mode the original channel/registry menus remain available.
        if (ProjectorMode.get(stack) != ProjectorMode.BUILD || ProjectorMode.isWireless(stack)) return stack;
        return super.onItemRightClick(stack, world, player);
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
        if (ProjectorMode.get(stack) == ProjectorMode.DISMANTLE) {
            // Never run the construction logic from the dismantle input path.
            return true;
        }
        if (!world.isRemote) DismantleManager.INSTANCE.onDirectControllerUse(player, world, x, y, z, side);
        return super.onItemUseFirst(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean adv) {
        boolean wireless = ProjectorMode.isWireless(stack);
        boolean dismantle = ProjectorMode.get(stack) == ProjectorMode.DISMANTLE;

        tooltip.add(translateToLocal(
                dismantle ? "item.structurelib.constructableTrigger.mode.selected.dismantle"
                        : "item.structurelib.constructableTrigger.mode.selected.build"));
        if (wireless) tooltip.add(translateToLocal("item.structurelib.constructableTrigger.mode.wireless"));

        tooltip.add(translateToLocalFormatted("item.structurelib.constructableTriggerV2.tier", ProjectorTier.getTier(stack)));

        tooltip.add(translateToLocal("item.structurelib.constructableTrigger.mode.separator"));
        tooltip.add(translateToLocal("item.structurelib.constructableTrigger.mode.build"));
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
            tooltip.add(
                    translateToLocalFormatted(
                            "item.structurelib.constructableTrigger.desc.lshift.0",
                            ChannelDataAccessor.countChannelData(stack)));
            ChannelDataAccessor.iterateChannelData(stack).map(e -> e.getKey() + ": " + e.getValue())
                    .forEach(tooltip::add);
        } else {
            tooltip.add(translateToLocal("item.structurelib.constructableTrigger.desc.0"));
            tooltip.add(BLUE + translateToLocal("item.structurelib.constructableTrigger.desc.1"));
            tooltip.add(BLUE + translateToLocal("item.structurelib.constructableTrigger.desc.2"));
            tooltip.add(BLUE + translateToLocal("item.structurelib.constructableTrigger.desc.3"));
            tooltip.add(BLUE + translateToLocal("item.structurelib.constructableTriggerV2.tier.desc"));
            if (ChannelDataAccessor.hasSubChannel(stack))
                tooltip.add(translateToLocal("item.structurelib.constructableTrigger.desc.5"));
        }
        if (wireless) tooltip.add(translateToLocal("item.structurelib.constructableTrigger.wireless.desc.1"));
        if (!dismantle && !wireless) {
            tooltip.add(translateToLocal("item.structurelib.constructableTrigger.build.menu.subchannels"));
            tooltip.add(translateToLocal("item.structurelib.constructableTrigger.build.menu.registries"));
        }

        tooltip.add(translateToLocal("item.structurelib.constructableTrigger.mode.separator"));
        tooltip.add(translateToLocal("item.structurelib.constructableTrigger.mode.dismantle"));
        tooltip.add(translateToLocal("item.structurelib.constructableTrigger.dismantle.desc.0"));
        if (wireless) tooltip.add(translateToLocal("item.structurelib.constructableTrigger.wireless.desc.2"));
        tooltip.add(translateToLocal("item.structurelib.constructableTrigger.dismantle.desc.1"));
        tooltip.add(translateToLocal("item.structurelib.constructableTrigger.dismantle.desc.3"));
        tooltip.add(translateToLocal("item.structurelib.constructableTrigger.dismantle.desc.4"));
        tooltip.add(translateToLocal("item.structurelib.constructableTrigger.dismantle.desc.5"));
        tooltip.add(translateToLocal("item.structurelib.constructableTrigger.dismantle.desc.6"));

        tooltip.add(translateToLocal("item.structurelib.constructableTrigger.mode.separator"));
        tooltip.add(translateToLocal("item.structurelib.constructableTrigger.mode.wireless.title"));
        tooltip.add(translateToLocal("item.structurelib.constructableTrigger.wireless.summary"));
        tooltip.add(translateToLocal("item.structurelib.constructableTrigger.wireless.bind"));
        tooltip.add(translateToLocal("item.structurelib.constructableTrigger.mode.toggle"));
        tooltip.add(translateToLocal("item.structurelib.constructableTrigger.wireless.desc.3"));
        tooltip.add(translateToLocal("item.structurelib.constructableTrigger.wireless.desc.4"));
    }
}
