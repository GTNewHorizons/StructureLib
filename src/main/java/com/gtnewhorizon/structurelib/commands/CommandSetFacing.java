package com.gtnewhorizon.structurelib.commands;

import com.gtnewhorizon.structurelib.structure.StructureUtility;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;

import java.util.List;

public class CommandSetFacing extends SubCommand {
    public CommandSetFacing() {
        super("facing");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        EntityPlayer player = getCommandSenderAsPlayer(sender);

        Vec3 playerDirection = player.getLookVec();

        CommandData.facing(StructureUtility.getExtendedFacingFromLookVector(playerDirection));

        sender.addChatMessage(new ChatComponentText(CommandData.facing().toString()));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return null;
    }
}
