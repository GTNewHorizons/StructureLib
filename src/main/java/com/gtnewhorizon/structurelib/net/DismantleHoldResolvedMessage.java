package com.gtnewhorizon.structurelib.net;

import com.gtnewhorizon.structurelib.StructureLib;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Stops the client-only bow pose when the server resolves a dismantle confirmation hold. */
public class DismantleHoldResolvedMessage implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<DismantleHoldResolvedMessage, IMessage> {

        @Override
        public IMessage onMessage(DismantleHoldResolvedMessage message, MessageContext ctx) {
            StructureLib.instance().proxy().acceptDismantleHoldResolved();
            return null;
        }
    }
}
