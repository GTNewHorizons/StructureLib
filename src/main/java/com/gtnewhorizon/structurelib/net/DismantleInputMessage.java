package com.gtnewhorizon.structurelib.net;

import net.minecraft.entity.player.EntityPlayerMP;

import com.gtnewhorizon.structurelib.dismantle.DismantleManager;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** A heartbeat is never treated as a duration: the server counts distinct server ticks. */
public class DismantleInputMessage implements IMessage {

    public static final byte TOGGLE = 0;
    public static final byte HOLD = 1;
    public static final byte RELEASE = 2;
    public static final byte BIND = 3;
    public static final byte UNLINK = 4;
    public static final byte REMOTE_BUILD = 5;
    public static final byte PROMPT = 6;

    /** Distinguishes remote air input from a physically targeted controller (including 0,0,0). */
    public static final int REMOTE_TARGET_X = Integer.MIN_VALUE;

    private byte action;
    private int x;
    private int y;
    private int z;

    public DismantleInputMessage() {}

    public DismantleInputMessage(byte action, int x, int y, int z) {
        this.action = action;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readByte();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
    }

    public static class Handler implements IMessageHandler<DismantleInputMessage, IMessage> {

        @Override
        public IMessage onMessage(DismantleInputMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            DismantleManager.INSTANCE.enqueue(player, message.action, message.x, message.y, message.z);
            return null;
        }
    }
}
