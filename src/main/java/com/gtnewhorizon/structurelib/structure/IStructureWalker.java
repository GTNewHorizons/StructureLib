package com.gtnewhorizon.structurelib.structure;

import net.minecraft.world.World;

public interface IStructureWalker<T> {
    boolean visit(IStructureElement<T> element, World world, int x, int y, int z);
}
