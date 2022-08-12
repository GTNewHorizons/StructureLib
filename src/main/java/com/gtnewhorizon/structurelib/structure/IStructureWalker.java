package com.gtnewhorizon.structurelib.structure;

import net.minecraft.world.World;

public interface IStructureWalker<T> {
    boolean visit(IStructureElement<T> element, World world, int x, int y, int z);

    default boolean blockNotLoaded(IStructureElement<T> element, World world, int x, int y, int z) {
        return false;
    }

    static <T> IStructureWalker<T> ignoreBlockUnloaded(IStructureWalker<T> walker) {
        return new IStructureWalker<T>() {
            @Override
            public boolean visit(IStructureElement<T> element, World world, int x, int y, int z) {
                return walker.visit(element, world, x, y, z);
            }

            @Override
            public boolean blockNotLoaded(IStructureElement<T> element, World world, int x, int y, int z) {
                return walker.visit(element, world, x, y, z);
            }
        };
    }

    static <T> IStructureWalker<T> skipBlockUnloaded(IStructureWalker<T> walker) {
        return new IStructureWalker<T>() {
            @Override
            public boolean visit(IStructureElement<T> element, World world, int x, int y, int z) {
                return walker.visit(element, world, x, y, z);
            }

            @Override
            public boolean blockNotLoaded(IStructureElement<T> element, World world, int x, int y, int z) {
                return true;
            }
        };
    }
}
