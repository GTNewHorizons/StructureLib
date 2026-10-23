package com.gtnewhorizon.structurelib.net;

import com.gtnewhorizon.structurelib.StructureLib;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Server-authoritative target information for client-side wireless previews. */
public class WirelessLinkMessage implements IMessage {

    private boolean linked;
    private int x;
    private int y;
    private int z;
    private int dimension;
    private int side;

    public WirelessLinkMessage() {}

    public WirelessLinkMessage(boolean linked, int x, int y, int z, int dimension, int side) {
        this.linked = linked;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.side = side;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        linked = buf.readBoolean();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        dimension = buf.readInt();
        side = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(linked);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(dimension);
        buf.writeInt(side);
    }

    public static class Handler implements IMessageHandler<WirelessLinkMessage, IMessage> {

        @Override
        public IMessage onMessage(WirelessLinkMessage message, MessageContext ctx) {
            StructureLib.instance().proxy().acceptWirelessLink(
                    message.linked,
                    message.x,
                    message.y,
                    message.z,
                    message.dimension,
                    message.side);
            return null;
        }
    }
}
