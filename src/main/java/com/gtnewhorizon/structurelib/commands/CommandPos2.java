package com.gtnewhorizon.structurelib.commands;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;

import java.util.List;

public class CommandPos2 extends SubCommand {
    public CommandPos2() {
        super("pos2");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0 || args.length == 3) {
            int xOffset = 0;
            int yOffset = 0;
            int zOffset = 0;

            if (args.length == 3) {
                try {
                    xOffset = Integer.parseInt(args[0]);
                    yOffset = Integer.parseInt(args[1]);
                    zOffset = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    throw new WrongUsageException("Optional arguments should be a positional offset represented by three integers.");
                }
            }

            CommandData.corners(1,
                                new Vec3Impl(sender.getPlayerCoordinates().posX + xOffset,
                                             sender.getPlayerCoordinates().posY + yOffset,
                                             sender.getPlayerCoordinates().posZ + zOffset),
                                             sender.getEntityWorld());

            StructureLibAPI.hintParticle(sender.getEntityWorld(),
                                         CommandData.corners()[1].get0(),
                                         CommandData.corners()[1].get1(),
                                         CommandData.corners()[1].get2(),
                                         StructureLibAPI.getBlockHint(),
                                         1);
        } else if (args.length == 1 && "help".equalsIgnoreCase(args[0])) {
            printHelp(sender, null);
        } else {
            throw new WrongUsageException("idk what to put here yet");
        }
    }

    @Override
    public void printHelp(ICommandSender sender, String subCommand) {
        sender.addChatMessage(new ChatComponentText("CommandPos2 help"));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return null;
    }
}
