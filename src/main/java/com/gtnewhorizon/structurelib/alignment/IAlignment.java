package com.gtnewhorizon.structurelib.alignment;

import com.gtnewhorizon.structurelib.alignment.enumerable.Direction;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.alignment.enumerable.Rotation;
import net.minecraftforge.common.util.ForgeDirection;

public interface IAlignment extends IAlignmentLimits,IAlignmentProvider {
    int DIRECTIONS_COUNT= Direction.VALUES.length;
    int ROTATIONS_COUNT= Rotation.VALUES.length;
    int FLIPS_COUNT= Flip.VALUES.length;
    int STATES_COUNT = ExtendedFacing.STATES_COUNT;

    ExtendedFacing getExtendedFacing();

    void setExtendedFacing(ExtendedFacing alignment);

    IAlignmentLimits getAlignmentLimits();

    @Override
    default IAlignment getAlignment(){
        return this;
    }

    static int getAlignmentIndex(ForgeDirection direction, Rotation rotation, Flip flip){
        return (direction.ordinal()*ROTATIONS_COUNT+rotation.getIndex())*FLIPS_COUNT+flip.getIndex();
    }

    default ForgeDirection getDirection(){
        return getExtendedFacing().getDirection();
    }

    default void setDirection(ForgeDirection direction){
        setExtendedFacing(getExtendedFacing().with(direction));
    }

    default Rotation getRotation(){
        return getExtendedFacing().getRotation();
    }

    default void setRotation(Rotation rotation){
        setExtendedFacing(getExtendedFacing().with(rotation));
    }

    default Flip getFlip(){
        return getExtendedFacing().getFlip();
    }

    default void setFlip(Flip flip){
        setExtendedFacing(getExtendedFacing().with(flip));
    }

    default boolean toolSetDirection(ForgeDirection direction){
        if(direction==null || direction==ForgeDirection.UNKNOWN){
            for (int i = 0,j=getDirection().ordinal()+1, valuesLength = Direction.VALUES.length; i < valuesLength; i++) {
                if (toolSetDirection(Direction.VALUES[(j + i) % valuesLength].getForgeDirection())) {
                    return true;
                }
            }
        }else {
            for (ExtendedFacing extendedFacing : ExtendedFacing.FOR_FACING.get(direction)) {
                if(checkedSetExtendedFacing(extendedFacing)){
                    return true;
                }
            }
        }
        return false;
    }

    default boolean checkedSetDirection(ForgeDirection direction){
        if (isNewDirectionValid(direction)){
            setDirection(direction);
            return true;
        }
        return false;
    }

    default boolean canSetToDirectionAny(ForgeDirection direction){
        for (ExtendedFacing extendedFacing : ExtendedFacing.FOR_FACING.get(direction)) {
            if(isNewExtendedFacingValid(extendedFacing)){
                return true;
            }
        }
        return false;
    }

    default boolean toolSetRotation(Rotation rotation) {
        if(rotation==null){
            int flips = Flip.VALUES.length;
            int rotations = Rotation.VALUES.length;
            for (int ii = 0,jj=getFlip().ordinal(); ii < flips; ii++) {
                for (int i = 1, j = getRotation().ordinal(); i < rotations; i++) {
                    if (checkedSetExtendedFacing(ExtendedFacing.of(getDirection(), Rotation.VALUES[(j + i) % rotations], Flip.VALUES[(jj + ii) % flips]))) {
                        return true;
                    }
                }
            }
            return false;
        }else {
            return checkedSetRotation(rotation);
        }
    }

    default boolean checkedSetRotation(Rotation rotation){
        if (isNewRotationValid(rotation)){
            setRotation(rotation);
            return true;
        }
        return false;
    }

    default boolean toolSetFlip(Flip flip){
        if(flip==null){
            for (int i = 1, j = getFlip().ordinal(), valuesLength = Flip.VALUES.length; i < valuesLength; i++) {
                if (checkedSetFlip(Flip.VALUES[(j + i) % valuesLength])) {
                    return true;
                }
            }
            return false;
        }else {
            return checkedSetFlip(flip);
        }
    }

    default boolean checkedSetFlip(Flip flip){
        if (isNewFlipValid(flip)){
            setFlip(flip);
            return true;
        }
        return false;
    }

    default boolean toolSetExtendedFacing(ExtendedFacing extendedFacing){
        if(extendedFacing==null){
            for (int i = 0,j=getExtendedFacing().ordinal()+1, valuesLength = ExtendedFacing.VALUES.length; i < valuesLength; i++) {
                if (checkedSetExtendedFacing(ExtendedFacing.VALUES[(j + i)%valuesLength])){
                    return true;
                }
            }
            return false;
        }else {
            return checkedSetExtendedFacing(extendedFacing);
        }
    }

    default boolean checkedSetExtendedFacing(ExtendedFacing alignment){
        if (isNewExtendedFacingValid(alignment)){
            setExtendedFacing(alignment);
            return true;
        }
        return false;
    }

    default boolean isNewDirectionValid(ForgeDirection direction) {
        return isNewExtendedFacingValid(direction,getRotation(),getFlip());
    }

    default boolean isNewRotationValid(Rotation rotation){
        return isNewExtendedFacingValid(getDirection(),rotation,getFlip());
    }

    default boolean isNewFlipValid(Flip flip){
        return isNewExtendedFacingValid(getDirection(),getRotation(),flip);
    }

    default boolean isExtendedFacingValid() {
        return isNewExtendedFacingValid(getDirection(),getRotation(),getFlip());
    }

    @Override
    default boolean isNewExtendedFacingValid(ForgeDirection direction, Rotation rotation, Flip flip){
        return getAlignmentLimits().isNewExtendedFacingValid(direction, rotation, flip);
    }

    @Override
    default boolean isNewExtendedFacingValid(ExtendedFacing alignment){
        return getAlignmentLimits().isNewExtendedFacingValid(
                alignment.getDirection(),
                alignment.getRotation(),
                alignment.getFlip());
    }
}