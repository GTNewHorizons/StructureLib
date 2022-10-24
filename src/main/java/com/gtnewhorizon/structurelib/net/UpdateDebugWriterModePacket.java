package com.gtnewhorizon.structurelib.net;

import com.gtnewhorizon.structurelib.item.ItemDebugStructureWriter;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import java.io.IOException;

public class UpdateDebugWriterModePacket implements IMessage, IMessageHandler<UpdateDebugWriterModePacket, IMessage> {
    NBTTagCompound tagCompound;

    public UpdateDebugWriterModePacket() {}

    public UpdateDebugWriterModePacket(ItemStack itemStack) {
        this.tagCompound = itemStack.copy().getTagCompound();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer pb = new PacketBuffer(buf);
        try {
            this.tagCompound = pb.readNBTTagCompoundFromBuffer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer pb = new PacketBuffer(buf);
        try {
            pb.writeNBTTagCompoundToBuffer(this.tagCompound);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IMessage onMessage(UpdateDebugWriterModePacket message, MessageContext ctx) {
        ItemStack itemStack = ctx.getServerHandler().playerEntity.inventory.getCurrentItem();

        if (!(itemStack.getItem() instanceof ItemDebugStructureWriter)) {
            return null;
        }

        itemStack.setTagCompound(message.tagCompound);
        itemStack.setItemDamage(ItemDebugStructureWriter.readModeFromNBT(itemStack).ordinal());

        return null;
    }
}
