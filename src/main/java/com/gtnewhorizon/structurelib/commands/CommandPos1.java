package com.gtnewhorizon.structurelib.commands;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;

public class CommandPos1 extends SubCommand {
    public CommandPos1() {
        super("pos1");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            CommandData.corners(0,
                                new Vec3Impl(sender.getPlayerCoordinates().posX,
                                             sender.getPlayerCoordinates().posY,
                                             sender.getPlayerCoordinates().posZ),
                                sender.getEntityWorld());

            StructureLibAPI.hintParticle(sender.getEntityWorld(),
                                         CommandData.corners()[0].get0(),
                                         CommandData.corners()[0].get1(),
                                         CommandData.corners()[0].get2(),
                                         StructureLibAPI.getBlockHint(),
                                         0);
        } else if (args.length == 1 && "help".equalsIgnoreCase(args[0])) {
            printHelp(sender, null);
        } else {
            throw new WrongUsageException("idk what to put here yet");
        }
    }

    @Override
    public void printHelp(ICommandSender sender, String subCommand) {
        sender.addChatMessage(new ChatComponentText("CommandPos1 help"));
    }
}
