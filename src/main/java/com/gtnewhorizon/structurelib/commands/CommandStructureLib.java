package com.gtnewhorizon.structurelib.commands;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;

import java.util.List;

public class CommandStructureLib extends SubCommand {
    public CommandStructureLib() {
        super("structurelib");

        setPermLevel(PermLevel.ADMIN);
        aliases.add("slib");

        this.addChildCommand(new CommandPos1());
        this.addChildCommand(new CommandPos2());
        this.addChildCommand(new CommandSetFacing());
        this.addChildCommand(new CommandBuild());
        this.addChildCommand(new CommandClear());
        this.addChildCommand(new CommandRefresh());
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
        sender.addChatMessage(new ChatComponentText("CommandStructureLib help"));
    }
}
