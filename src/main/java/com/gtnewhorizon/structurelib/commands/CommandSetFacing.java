package com.gtnewhorizon.structurelib.commands;

import com.gtnewhorizon.structurelib.structure.StructureUtility;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;

import java.util.List;

public class CommandSetFacing extends SubCommand {
    public CommandSetFacing() {
        super("facing");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            EntityPlayer player = getCommandSenderAsPlayer(sender);

            Vec3 playerDirection = player.getLookVec();

            CommandData.facing(StructureUtility.getExtendedFacingFromLookVector(playerDirection));

            sender.addChatMessage(new ChatComponentText(CommandData.facing().toString()));
        } else if (args.length == 1 && "help".equalsIgnoreCase(args[0])) {
            printHelp(sender, null);
        } else {
            throw new WrongUsageException(StatCollector.translateToLocal("structurelib.command.errorMessage"));
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void printHelp(ICommandSender sender, String command) {
        ChatStyle header = new ChatStyle().setColor(EnumChatFormatting.AQUA);

        sender.addChatMessage(new ChatComponentText("/structurelib facing").setChatStyle(header));

        sender.addChatMessage(new ChatComponentTranslation("structurelib.command.setFacing.desc.0"));
        sender.addChatMessage(new ChatComponentTranslation("structurelib.command.setFacing.desc.1"));
    }
}
