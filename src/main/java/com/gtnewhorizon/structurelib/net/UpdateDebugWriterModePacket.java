package com.gtnewhorizon.structurelib.net;

import com.gtnewhorizon.structurelib.item.ItemDebugStructureWriter;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagByte;

public class UpdateDebugWriterModePacket implements IMessage, IMessageHandler<UpdateDebugWriterModePacket, IMessage> {
    int damageValue;

    public UpdateDebugWriterModePacket() {}

    public UpdateDebugWriterModePacket(int damageValue) {
        this.damageValue = damageValue;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.damageValue = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.damageValue);
    }

    @Override
    public IMessage onMessage(UpdateDebugWriterModePacket message, MessageContext ctx) {
        ItemStack itemStack = ctx.getServerHandler().playerEntity.inventory.getCurrentItem();

//        if (itemStack.getItem() instanceof ItemDebugStructureWriter) {
//            itemStack.setItemDamage(message.damageValue);
//        }

        itemStack.setTagInfo("mode", new NBTTagByte((byte) message.damageValue));

        return null;
    }
}
