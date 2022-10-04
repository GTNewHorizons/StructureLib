package com.gtnewhorizon.structurelib.commands;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;

public class CommandStructureLib extends SubCommand {
    public CommandStructureLib() {
        super("structurelib");

        setPermLevel(PermLevel.ADMIN);
        aliases.add("slib");

        this.addChildCommand(new CommandPos1());
        this.addChildCommand(new CommandPos2());
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            printHelp(sender, null);
        } else {
            if (children.containsKey(args[0])) {
                processChildCommand(sender, args);
            } else {
                throw new WrongUsageException("Invalid Argument(s)");
            }
        }
    }

    @Override
    public void printHelp(ICommandSender sender, String subCommand) {
        sender.addChatMessage(new ChatComponentText("CommandStructureLib help"));
    }
}
