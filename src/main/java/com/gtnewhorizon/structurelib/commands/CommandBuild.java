package com.gtnewhorizon.structurelib.commands;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;

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
        super.printHelp(sender, command);
    }
}
