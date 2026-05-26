package com.gtnewhorizon.structurelib.coords;

/**
 * Controller-relative coordinates. The controller is at 0,0,0. No axis swapping takes place, each axis points in the
 * same directions as the matching world axis.
 */
public class ControllerRelativeCoords implements CoordinateSystem<ControllerRelativeCoords, WorldCoords> {

    public final int controllerX, controllerY, controllerZ;

    public ControllerRelativeCoords(int controllerX, int controllerY, int controllerZ) {
        this.controllerX = controllerX;
        this.controllerY = controllerY;
        this.controllerZ = controllerZ;
    }

    @Override
    public Position<ControllerRelativeCoords> translate(Position<WorldCoords> position) {
        return translate(position, controllerX, controllerY, controllerZ);
    }

    @Override
    public Position<WorldCoords> translateInverse(Position<ControllerRelativeCoords> position) {
        return translateInverse(position, controllerX, controllerY, controllerZ);
    }

    public static Position<ControllerRelativeCoords> translate(Position<WorldCoords> position, int controllerX,
            int controllerY, int controllerZ) {
        position.sub(controllerX, controllerY, controllerZ);

        return CoordinateSystem.transmute(position);
    }

    public static Position<WorldCoords> translateInverse(Position<ControllerRelativeCoords> position, int controllerX,
            int controllerY, int controllerZ) {
        position.add(controllerX, controllerY, controllerZ);

        return CoordinateSystem.transmute(position);
    }
}
