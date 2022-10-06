package com.gtnewhorizon.structurelib.commands;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;
import org.apache.commons.lang3.tuple.Pair;

public class CommandSetFacing extends SubCommand {
    private static final Vec3 SOUTH = Vec3.createVectorHelper(0, 0, 1);
    private static final Vec3 EAST = Vec3.createVectorHelper(1, 0, 0);

    private static final Vec3 UP = Vec3.createVectorHelper(0, 1, 0);

    private enum Axis {
        X, Y, Z;
    };

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

        double upScalarProjection = lookVec.dotProduct(UP);
        Vec3 upVectorProjection = Vec3.createVectorHelper(UP.xCoord * upScalarProjection,
                                                          UP.yCoord * upScalarProjection,
                                                          UP.zCoord * upScalarProjection);

        ExtendedFacing facing = null;

        //we want the facing opposite the player look vector
        int max = maxOrdinal(southVectorProjection.lengthVector(),
                             eastVectorProjection.lengthVector(),
                             upVectorProjection.lengthVector());

        switch(max) {
            case 0:
                facing = (southVectorProjection.zCoord > 0) ? ExtendedFacing.NORTH_NORMAL_NONE : ExtendedFacing.SOUTH_NORMAL_NONE;
                break;
            case 1:
                facing = (eastVectorProjection.xCoord > 0) ? ExtendedFacing.WEST_NORMAL_NONE : ExtendedFacing.EAST_NORMAL_NONE;
                break;
            case 2:
                facing = (upVectorProjection.yCoord > 0) ? ExtendedFacing.DOWN_NORMAL_NONE : ExtendedFacing.UP_NORMAL_NONE;
                break;
        }

        return facing;
    }

    private int maxOrdinal(double... values) {
        int maxOrdinal = 0;

        for (int i = 0; i < values.length; i++) {
            if (values[i] > values[maxOrdinal]) {
                maxOrdinal = i;
            }
        }

        return maxOrdinal;
    }
}
