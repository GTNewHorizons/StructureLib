package com.gtnewhorizon.structurelib.commands;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.*;

public class CommandRefresh extends SubCommand {
    public CommandRefresh() {
        super("refresh");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            if (CommandData.box() != null) {
                CommandData.box().drawBoundingBox(sender.getEntityWorld());
            } else {
                throw new WrongUsageException(StatCollector.translateToLocal("structurelib.command.refresh.errorMessage"));
            }
        } else if (args.length == 1 && "help".equalsIgnoreCase(args[0])) {
            printHelp(sender, null);
        } else {
            throw new WrongUsageException(StatCollector.translateToLocal("structurelib.command.errorMessage"));
        }
    }

    @Override
    public void printHelp(ICommandSender sender, String command) {
        ChatStyle header = new ChatStyle().setColor(EnumChatFormatting.AQUA);

        sender.addChatMessage(new ChatComponentText("/structurelib refresh").setChatStyle(header));

        sender.addChatMessage(new ChatComponentTranslation("structurelib.command.refresh.desc.0"));

        ChatStyle requirements = new ChatStyle().setColor(EnumChatFormatting.RED);
        sender.addChatMessage(new ChatComponentTranslation("structurelib.command.refresh.desc.1").setChatStyle(requirements));
        sender.addChatMessage(new ChatComponentText("/structurelib pos1"));
        sender.addChatMessage(new ChatComponentText("/structurelib pos2"));
    }
}
