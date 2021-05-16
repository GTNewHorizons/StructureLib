package com.gtnewhorizon.structurelib.alignment;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.alignment.enumerable.Rotation;
import net.minecraftforge.common.util.ForgeDirection;

public interface IAlignmentLimits {

    IAlignmentLimits UNLIMITED= (direction, rotation, flip) -> true;

    boolean isNewExtendedFacingValid(ForgeDirection direction, Rotation rotation, Flip flip);

    default boolean isNewExtendedFacingValid(ExtendedFacing alignment){
        return isNewExtendedFacingValid(
                alignment.getDirection(),
                alignment.getRotation(),
                alignment.getFlip());
    }
}
