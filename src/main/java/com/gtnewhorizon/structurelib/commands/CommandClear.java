package com.gtnewhorizon.structurelib.commands;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.*;

public class CommandClear extends SubCommand {
    public CommandClear() {
        super("clear");

        this.aliases.add("reset");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            CommandData.data(sender).reset();
        } else if (args.length == 1 && "help".equalsIgnoreCase(args[0])) {
            printHelp(sender, null);
        } else {
            throw new WrongUsageException(StatCollector.translateToLocal("structurelib.command.errorMessage"));
        }
    }

    @Override
    public void printHelp(ICommandSender sender, String command) {
        ChatStyle header = new ChatStyle().setColor(EnumChatFormatting.AQUA);

        String aliases = String.join(" | ", this.name, String.join(" | ", this.aliases));
        sender.addChatMessage(new ChatComponentText("/structurelib < " + aliases + " >").setChatStyle(header));

        sender.addChatMessage(new ChatComponentTranslation("structurelib.command.clear.desc.0"));
    }
}
