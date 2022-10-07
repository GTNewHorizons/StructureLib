package com.gtnewhorizon.structurelib.commands;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

public class CommandBuild extends SubCommand {
    public CommandBuild() {
        super("build");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!CommandData.isReady()) {
                return;
            }

            Vec3Impl basePosition = CommandData.getBasePos();

            String structureDefinition = StructureUtility.getPseudoJavaCode(sender.getEntityWorld(),
                                                                            CommandData.facing(),
                                                                            basePosition.get0(),
                                                                            basePosition.get1(),
                                                                            basePosition.get2(),
                                                                            0,
                                                                            0,
                                                                            0,
                                                                            te -> te.getClass().getCanonicalName(),
                                                                            CommandData.box().xSize(),
                                                                            CommandData.box().ySize(),
                                                                            CommandData.box().zSize(),
                                                                            false);

            StructureLib.LOGGER.info(structureDefinition);
        } else if (args.length == 1 && "help".equalsIgnoreCase(args[0])) {
            printHelp(sender, null);
        } else {
            throw new WrongUsageException("This command does not take any arguments.");
        }
    }

    @Override
    public void printHelp(ICommandSender sender, String command) {
        ChatStyle header = new ChatStyle().setColor(EnumChatFormatting.AQUA);
        sender.addChatMessage(new ChatComponentText("/structurelib build").setChatStyle(header));

        sender.addChatMessage(new ChatComponentText("Use to write out the structure you've selected."));

        ChatStyle requirements = new ChatStyle().setColor(EnumChatFormatting.RED);
        sender.addChatMessage(new ChatComponentText("Requirements: The bounding box needs to be created and the facing needs to be selected using the following commands:").setChatStyle(requirements));
        sender.addChatMessage(new ChatComponentText("/structurelib pos1"));
        sender.addChatMessage(new ChatComponentText("/structurelib pos2"));
        sender.addChatMessage(new ChatComponentText("/structurelib facing"));
    }
}
