package com.gtnewhorizon.structurelib.commands;

import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.gtnewhorizon.structurelib.util.StructureData;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;

import java.util.List;

import static com.gtnewhorizon.structurelib.util.StructureData.StructureDataEntry;

public class CommandSetFacing extends SubCommand {
    public CommandSetFacing() {
        super("facing");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            EntityPlayer player = getCommandSenderAsPlayer(sender);

            Vec3 playerDirection = player.getLookVec();

            StructureDataEntry data = StructureData.data(sender);

            data.facing(StructureUtility.getExtendedFacingFromLookVector(playerDirection));

            sender.addChatMessage(new ChatComponentText(data.facing().toString()));
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
