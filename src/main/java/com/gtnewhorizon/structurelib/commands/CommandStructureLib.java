package com.gtnewhorizon.structurelib.commands;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.*;

import java.util.HashSet;
import java.util.List;

import static net.minecraft.util.StatCollector.translateToLocal;

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
        this.addChildCommand(new CommandSetController());
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1 || "help".equalsIgnoreCase(args[0])) {
            printHelp(sender, null);
        } else {
            if (children.containsKey(args[0])) {
                processChildCommand(sender, args);
            } else {
                throw new WrongUsageException(StatCollector.translateToLocal("structurelib.command.errorMessage"));
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
            sender.addChatMessage(new ChatComponentTranslation("structurelib.command.structureLib.desc.0").setChatStyle(header));

            sender.addChatMessage(new ChatComponentTranslation("structurelib.command.structureLib.desc.1"));

            ChatStyle bodyHeader = new ChatStyle().setColor(EnumChatFormatting.GRAY);
            sender.addChatMessage(new ChatComponentText(String.format("%-10s%10s",
                                                                      translateToLocal("structurelib.command.structureLib.desc.2"),
                                                                      translateToLocal("structurelib.command.structureLib.desc.3"))).setChatStyle(bodyHeader));

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

            sender.addChatMessage(new ChatComponentTranslation("structurelib.command.structureLib.desc.4"));
        }
    }
}
