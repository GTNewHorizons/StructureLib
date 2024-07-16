package com.gtnewhorizon.structurelib.util;

import org.joml.Vector3i;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureElement;

public class PositionedIStructureElement extends Vector3i {

    public final IStructureElement<IConstructable> element;

    public PositionedIStructureElement(int x, int y, int z, IStructureElement<IConstructable> element) {
        super(x, y, z);
        this.element = element;
    }

}
