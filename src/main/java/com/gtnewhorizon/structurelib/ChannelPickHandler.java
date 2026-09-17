package com.gtnewhorizon.structurelib;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.net.SetChannelDataMessage;

/**
 * Handler for middle-click channel picking. When the player holds a hologram projector on the cursor and middle-clicks
 * a registered indicator item, the corresponding channel is set on the projector. The same applies in world when the
 * projector is held in hand and a registered indicator block is middle-clicked. Shift+middle-click wipes all channels.
 * Was made side neutral to support server-side validation at some point if necessary.
 */
public final class ChannelPickHandler {

    private ChannelPickHandler() {}

    /**
     * Handle a middle-click event while holding a hologram projector on the cursor.
     */
    public static boolean handleMiddleClick(EntityPlayer player, ItemStack holoStack, @Nullable ItemStack targetStack,
            final boolean isSneaking) {
        return handlePick(player, holoStack, targetStack, isSneaking, true);
    }

    public static boolean handleWorldPick(World world, EntityPlayer player, ItemStack holoStack,
            @Nullable MovingObjectPosition hit, final boolean isSneaking) {
        ItemStack targetStack = null;

        if (hit != null) {
            Block block = world.getBlock(hit.blockX, hit.blockY, hit.blockZ);
            if (!block.isAir(world, hit.blockX, hit.blockY, hit.blockZ)) {
                targetStack = block.getPickBlock(hit, world, hit.blockX, hit.blockY, hit.blockZ, player);
            }
        }

        // A null target still goes through so that shift+middle-click wipes even when aiming at nothing
        return handlePick(player, holoStack, targetStack, isSneaking, false);
    }

    private static boolean handlePick(EntityPlayer player, ItemStack holoStack, @Nullable ItemStack targetStack,
            final boolean isSneaking, final boolean onCursor) {
        // Clear channels on shift+middle-click
        if (isSneaking) {
            ChannelDataAccessor.wipeChannelData(holoStack);
            sendChannelData(player, holoStack, onCursor);
            player.addChatMessage(new ChatComponentTranslation("structurelib.pickchannel.cleared"));
            return true;
        }

        if (targetStack == null) return false; // nothing hovered, cancel silently

        Collection<Map.Entry<String, Integer>> channels = ChannelDescription.iterate(targetStack);
        if (channels.isEmpty()) return false; // no channel mapping, cancel silently

        // Assuming there's only 1 channel per item for now (same behavior as drag and drop)
        Map.Entry<String, Integer> first = channels.iterator().next();
        ChannelDataAccessor.setChannelData(holoStack, first.getKey(), first.getValue());
        sendChannelData(player, holoStack, onCursor);
        player.addChatMessage(
                new ChatComponentTranslation("structurelib.pickchannel.success", first.getKey(), first.getValue()));
        return true;
    }

    /**
     * Player is the one triggering this update, so should already be in-sync.
     */
    private static void sendChannelData(EntityPlayer player, ItemStack holoStack, boolean onCursor) {
        if (!(player instanceof EntityPlayerMP)) {
            StructureLib.net.sendToServer(new SetChannelDataMessage(holoStack, onCursor));
        }
    }
}
