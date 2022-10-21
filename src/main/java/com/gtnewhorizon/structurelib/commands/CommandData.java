package com.gtnewhorizon.structurelib.commands;

import com.gtnewhorizon.structurelib.util.StructureData;
import net.minecraft.command.ICommandSender;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CommandData {
    private static final Map<UUID, StructureData> data = new HashMap<>();

    public static StructureData data(ICommandSender sender) {
        UUID uuid = sender.getEntityWorld().getPlayerEntityByName(sender.getCommandSenderName()).getUniqueID();

        if (!data.containsKey(uuid)) {
            data.put(uuid, new StructureData());
        }

        return data.get(uuid);
    }
}
