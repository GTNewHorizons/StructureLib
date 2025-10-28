package com.gtnewhorizon.structurelib.coords;

import com.gtnewhorizon.structurelib.alignment.IntegerAxisSwap;

/**
 * Coordinates relative to the structure. 0,0,0 is the controller. 1,0,0 is the block immediately behind the controller
 * (looking at it from its face). 0,0,1 is the block to the right of the controller. 0,1,0 is the block below the
 * controller.
 */
public class StructureRelativeCoords implements CoordinateSystem<StructureRelativeCoords, ControllerRelativeCoords> {

    public final IntegerAxisSwap axisSwap;

    public StructureRelativeCoords(IntegerAxisSwap axisSwap) {
        this.axisSwap = axisSwap;
    }

    @Override
    public Position<StructureRelativeCoords> translate(Position<ControllerRelativeCoords> position) {
        position.set(axisSwap.translate(position));

        return CoordinateSystem.transmute(position);
    }

    @Override
    public Position<ControllerRelativeCoords> translateInverse(Position<StructureRelativeCoords> position) {
        position.set(axisSwap.inverseTranslate(position));

        return CoordinateSystem.transmute(position);
    }
}
