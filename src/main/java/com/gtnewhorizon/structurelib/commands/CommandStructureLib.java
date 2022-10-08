package com.gtnewhorizon.structurelib.commands;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class CommandStructureLib extends SubCommand {
    public CommandStructureLib() {
        super("structurelib");

        setPermLevel(PermLevel.ADMIN);
        aliases.add("slib");

        this.addChildCommand(new CommandPos(CommandPos.Variant.pos1));
        this.addChildCommand(new CommandPos(CommandPos.Variant.pos2));
        this.addChildCommand(new CommandSetFacing());
        this.addChildCommand(new CommandBuild());
        this.addChildCommand(new CommandClear());
        this.addChildCommand(new CommandRefresh());
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1 || "help".equalsIgnoreCase(args[0])) {
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
    @SuppressWarnings("unchecked")
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        List<String> options = super.addTabCompletionOptions(sender, args);

        if (options != null) {
            options = (List<String>) getListOfStringsMatchingLastWord(args, options.toArray(new String[0]));
        }

        return options;
    }

    @Override
    public void printHelp(ICommandSender sender, String subCommand) {
        if (subCommand == null) {
            ChatStyle header = new ChatStyle().setColor(EnumChatFormatting.AQUA).setBold(true);
            sender.addChatMessage(new ChatComponentText("Structure Lib Commands").setChatStyle(header));

            sender.addChatMessage(new ChatComponentText("Format: \"/structurelib <command>\" or \"/slib <command>\""));

            ChatStyle bodyHeader = new ChatStyle().setColor(EnumChatFormatting.GRAY);
            sender.addChatMessage(new ChatComponentText(String.format("%-10s%10s", "Command", "Aliases")).setChatStyle(bodyHeader));

            ChatStyle body = new ChatStyle().setColor(EnumChatFormatting.DARK_GRAY);
            new HashSet<>(this.children.values()).stream()
                                  .sorted()
                                  .forEach(command -> {
                                      ChatComponentText cct = new ChatComponentText(String.format("%-10s%10s",
                                                                                    command.name,
                                                                                    String.join(", ", command.aliases)));
                                      cct.setChatStyle(body);
                                      sender.addChatMessage(cct);
            });

            ChatStyle footer = new ChatStyle().setColor(EnumChatFormatting.GRAY);
            sender.addChatMessage(new ChatComponentText("Run \"help\" after any command to see more information about it."));
        }
    }
}
