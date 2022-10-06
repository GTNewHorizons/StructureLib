package com.gtnewhorizon.structurelib.commands;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;

public class CommandClear extends SubCommand {
    public CommandClear() {
        super("clear");

        this.aliases.add("reset");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            CommandData.reset();
        } else if (args.length == 1 && "help".equalsIgnoreCase(args[0])) {
            printHelp(sender, null);
        } else {
            throw new WrongUsageException("This command doesn't take any arguments");
        }
    }

    @Override
    public void printHelp(ICommandSender sender, String command) {
        super.printHelp(sender, command);
    }
}
