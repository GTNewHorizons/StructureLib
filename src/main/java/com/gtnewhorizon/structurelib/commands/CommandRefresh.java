package com.gtnewhorizon.structurelib.commands;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;

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
            throw new WrongUsageException("This command doesn't take any arguments.");
        }
    }

    @Override
    public void printHelp(ICommandSender sender, String command) {

    }
}
