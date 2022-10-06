package com.gtnewhorizon.structurelib.commands;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
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
        this.addChildCommand(new CommandSetFacing());
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            printHelp(sender, null);
        } else if ("refresh".equalsIgnoreCase(args[0])) {
            CommandData.box().drawBoundingBox(sender.getEntityWorld());
        } else if ("clear".equalsIgnoreCase(args[0])) {
            CommandData.reset();
        } else if ("build".equalsIgnoreCase(args[0])) {
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
