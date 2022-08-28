package com.gtnewhorizon.structurelib.net;

import static com.gtnewhorizon.structurelib.StructureLib.LOGGER;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class UpdateHintParticleMessage implements IMessage {
    private int x;
    private short y;
    private int z;
    private short r;
    private short g;
    private short b;
    private short a;

    public UpdateHintParticleMessage() {}

    public UpdateHintParticleMessage(int x, short y, int z, short r, short g, short b, short a) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readShort();
        z = buf.readInt();
        r = buf.readUnsignedByte();
        g = buf.readUnsignedByte();
        b = buf.readUnsignedByte();
        a = buf.readUnsignedByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeShort(y);
        buf.writeInt(z);
        buf.writeByte(r);
        buf.writeByte(g);
        buf.writeByte(b);
        buf.writeByte(a);
    }

    public static class Handler implements IMessageHandler<UpdateHintParticleMessage, IMessage> {
        @Override
        public IMessage onMessage(UpdateHintParticleMessage msg, MessageContext ctx) {
            boolean updateResult = StructureLibAPI.updateHintParticleTint(
                    StructureLib.getCurrentPlayer(),
                    StructureLib.getCurrentPlayer().worldObj,
                    msg.x,
                    msg.y,
                    msg.z,
                    new short[] {
                        msg.r, msg.g, msg.b, msg.a,
                    });
            if (StructureLibAPI.isDebugEnabled())
                LOGGER.debug(
                        "Server instructed to update hint particle at ({}, {}, {}), result {}!",
                        msg.x,
                        msg.y,
                        msg.z,
                        updateResult);
            return null;
        }
    }
}
