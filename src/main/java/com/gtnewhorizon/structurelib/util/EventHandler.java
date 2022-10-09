package com.gtnewhorizon.structurelib.util;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.item.ItemDebugStructureWriter;
import com.gtnewhorizon.structurelib.net.UpdateDebugWriterModePacket;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.MouseEvent;

public class EventHandler {
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onMouseEvent(MouseEvent event) {
        if (event.button < 0) {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;

            if (player != null && player.isSneaking()) {
                ItemStack itemStack = player.getHeldItem();

                if (itemStack != null && itemStack.getItem() instanceof ItemDebugStructureWriter) {
                    ItemDebugStructureWriter idsw = (ItemDebugStructureWriter) itemStack.getItem();

                    if (event.dwheel != 0) {
                        int value = idsw.mode().ordinal() + Math.max(-1, Math.min(event.dwheel, 1));

                        if (value > ItemDebugStructureWriter.Usage.values().length)
                            value = 0;
                        if (value < 0)
                            value = ItemDebugStructureWriter.Usage.values().length - 1;

                        idsw.mode(ItemDebugStructureWriter.Usage.getUsageFromOrdinal(value));

                        StructureLib.net.sendToServer(new UpdateDebugWriterModePacket(value));

                        event.setCanceled(true);
                    }
                }
            }
        }
    }
}
