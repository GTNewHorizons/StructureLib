package com.gtnewhorizon.structurelib.commands;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;

public class CommandSetFacing extends SubCommand {
    private static final Vec3 SOUTH = Vec3.createVectorHelper(0, 0, 1);
    private static final Vec3 EAST = Vec3.createVectorHelper(1, 0, 0);

    public CommandSetFacing() {
        super("facing");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        EntityPlayer player = getCommandSenderAsPlayer(sender);

        Vec3 playerDirection = player.getLookVec();

        CommandData.facing(getExtendedFacingFromLookVector(playerDirection));

        sender.addChatMessage(new ChatComponentText(CommandData.facing().toString()));
    }

    public ExtendedFacing getExtendedFacingFromLookVector(Vec3 lookVec) {
        double southScalarProjection = lookVec.dotProduct(SOUTH);
        Vec3 southVectorProjection = Vec3.createVectorHelper(SOUTH.xCoord * southScalarProjection,
                                                             SOUTH.yCoord * southScalarProjection,
                                                             SOUTH.zCoord * southScalarProjection);

        double eastScalarProjection = lookVec.dotProduct(EAST);
        Vec3 eastVectorProjection = Vec3.createVectorHelper(EAST.xCoord * eastScalarProjection,
                                                            EAST.yCoord * eastScalarProjection,
                                                            EAST.zCoord * eastScalarProjection);

        ExtendedFacing facing = null;

        //we want the facing opposite the player look vector
        if (southVectorProjection.lengthVector() > eastVectorProjection.lengthVector()) {
            facing = (southVectorProjection.zCoord > 0) ? ExtendedFacing.NORTH_NORMAL_NONE : ExtendedFacing.SOUTH_NORMAL_NONE;
        } else {
            facing = (eastVectorProjection.xCoord > 0) ? ExtendedFacing.WEST_NORMAL_NONE : ExtendedFacing.EAST_NORMAL_NONE;
        }

        return facing;
    }
}
