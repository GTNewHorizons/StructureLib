package com.gtnewhorizon.structurelib.net;

import static com.gtnewhorizon.structurelib.StructureLib.LOGGER;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.StructureLibAPI;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class ErrorHintParticleMessage implements IMessage {

    private int x;
    private short y;
    private int z;

    public ErrorHintParticleMessage() {}

    public ErrorHintParticleMessage(int x, short y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readShort();
        z = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeShort(y);
        buf.writeInt(z);
    }

    public static class Handler implements IMessageHandler<ErrorHintParticleMessage, IMessage> {

        @Override
        public IMessage onMessage(ErrorHintParticleMessage msg, MessageContext ctx) {
            boolean updateResult = StructureLibAPI.markHintParticleError(
                    StructureLib.getCurrentPlayer(),
                    StructureLib.getCurrentPlayer().worldObj,
                    msg.x,
                    msg.y,
                    msg.z);
            if (StructureLibAPI.isDebugEnabled()) LOGGER.debug(
                    "Server instructed to mark hint particle at ({}, {}, {}) error, result {}!",
                    msg.x,
                    msg.y,
                    msg.z,
                    updateResult);
            return null;
        }
    }
}
