package com.gtnewhorizon.structurelib.commands;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

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
                throw new WrongUsageException("The corners of the selection need to be set.");
            }
        } else if (args.length == 1 && "help".equalsIgnoreCase(args[0])) {
            printHelp(sender, null);
        } else {
            throw new WrongUsageException("This command does not take any arguments.");
        }
    }

    @Override
    public void printHelp(ICommandSender sender, String command) {
        ChatStyle header = new ChatStyle().setColor(EnumChatFormatting.AQUA);

        sender.addChatMessage(new ChatComponentText("/structurelib refresh").setChatStyle(header));

        sender.addChatMessage(new ChatComponentText("Use to redraw the bounding box you have selected."));

        ChatStyle requirements = new ChatStyle().setColor(EnumChatFormatting.RED);
        sender.addChatMessage(new ChatComponentText("Requirements: the bounding box needs to have been created through running:").setChatStyle(requirements));
        sender.addChatMessage(new ChatComponentText("/structurelib pos1"));
        sender.addChatMessage(new ChatComponentText("and").setChatStyle(requirements));
        sender.addChatMessage(new ChatComponentText("/structurelib pos2"));
    }
}
