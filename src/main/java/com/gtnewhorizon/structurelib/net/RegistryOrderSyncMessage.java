package com.gtnewhorizon.structurelib.net;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.gtnewhorizon.structurelib.SortedRegistry;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class RegistryOrderSyncMessage implements IMessage {

    private String name;
    private List<String> order;
    private List<String> disabled;

    public RegistryOrderSyncMessage() {}

    public RegistryOrderSyncMessage(String name, List<String> order, List<String> disabled) {
        this.name = name;
        this.order = order;
        this.disabled = disabled;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        name = ByteBufUtils.readUTF8String(buf);
        int size = ByteBufUtils.readVarInt(buf, 2);
        order = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            order.add(ByteBufUtils.readUTF8String(buf));
        }
        size = ByteBufUtils.readVarInt(buf, 2);
        disabled = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            disabled.add(ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, name);
        ByteBufUtils.writeVarInt(buf, order.size(), 2);
        for (String s : order) {
            ByteBufUtils.writeUTF8String(buf, s);
        }
        ByteBufUtils.writeVarInt(buf, disabled.size(), 2);
        for (String s : disabled) {
            ByteBufUtils.writeUTF8String(buf, s);
        }
    }

    public static class Handler implements IMessageHandler<RegistryOrderSyncMessage, IMessage> {

        private static ConcurrentHashMap<String, WeakReference<SortedRegistry<?>>> ALL_REGISTRIES;

        public static void setAllRegistries(ConcurrentHashMap<String, WeakReference<SortedRegistry<?>>> r) {
            if (ALL_REGISTRIES == null) ALL_REGISTRIES = r;
        }

        @Override
        public IMessage onMessage(RegistryOrderSyncMessage message, MessageContext ctx) {
            if (ALL_REGISTRIES == null) return null;
            WeakReference<SortedRegistry<?>> ref = ALL_REGISTRIES.get(message.name);
            if (ref == null) return null;
            SortedRegistry<?> r = ref.get();
            if (r == null) return null;
            r.registerOrdering(ctx.getServerHandler().playerEntity, message.order, message.disabled);
            return null;
        }
    }
}
