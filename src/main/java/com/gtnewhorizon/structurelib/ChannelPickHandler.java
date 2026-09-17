package com.gtnewhorizon.structurelib;

import java.util.Collection;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.net.SetChannelDataMessage;

/**
 * Client-side handler for middle-click channel picking. When the player holds a hologram projector on the cursor and
 * middle-clicks a registered indicator item, the corresponding channel is set on the projector. Shift+middle-click
 * wipes all channels.
 */
public final class ChannelPickHandler {

    private ChannelPickHandler() {}

    /**
     * Handle a middle-click event while holding a hologram projector on the cursor.
     *
     * @param holoStack   the hologram projector on the cursor (already validated)
     * @param targetStack the item stack under the mouse from a vanilla slot, may be null
     * @return true if the event was handled (caller should cancel)
     */
    public static boolean handleMiddleClick(ItemStack holoStack, ItemStack targetStack, final boolean isSneaking) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;

        // Clear channels on shift+middle-click
        if (isSneaking) {
            ChannelDataAccessor.wipeChannelData(holoStack);
            sendCursorChannelData(holoStack);
            player.addChatMessage(new ChatComponentText(I18n.format("structurelib.pickchannel.cleared")));
            return true;
        }

        if (targetStack == null) return true; // nothing hovered, cancel silently

        Collection<Map.Entry<String, Integer>> channels = ChannelDescription.iterate(targetStack);
        if (channels.isEmpty()) return true; // no channel mapping, cancel silently

        // Assuming there's only 1 channel per item for now (same behavior as drag and drop)
        Map.Entry<String, Integer> first = channels.iterator().next();
        ChannelDataAccessor.setChannelData(holoStack, first.getKey(), first.getValue());
        sendCursorChannelData(holoStack);
        player.addChatMessage(
                new ChatComponentText(
                        I18n.format("structurelib.pickchannel.success", first.getKey(), first.getValue())));
        return true;
    }

    private static void sendCursorChannelData(ItemStack holoStack) {
        StructureLib.net.sendToServer(new SetChannelDataMessage(holoStack, true));
    }
}
