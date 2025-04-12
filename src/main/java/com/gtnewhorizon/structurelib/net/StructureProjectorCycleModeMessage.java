package com.gtnewhorizon.structurelib.net;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.item.ItemConstructableTrigger;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class StructureProjectorCycleModeMessage implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {
        // no op
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // no op
    }

    public static class Handler implements IMessageHandler<StructureProjectorCycleModeMessage, IMessage> {

        @Override
        public IMessage onMessage(StructureProjectorCycleModeMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            final ItemStack held = player.getHeldItem();
            if (held != null && held.getItem() instanceof ItemConstructableTrigger) {
                ItemConstructableTrigger.cycleMode(held);
            }
            return null;
        }
    }
}
