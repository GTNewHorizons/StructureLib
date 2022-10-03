package com.gtnewhorizon.structurelib.commands;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

public class CommandStructureLib extends SubCommand {
    public CommandStructureLib() {
        super("structurelib");

        setPermLevel(PermLevel.ADMIN);
        aliases.add("slib");

        this.addChildCommand(new CommandSetCorner());
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        sender.addChatMessage(new ChatComponentText("test"));
    }
}
