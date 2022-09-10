package com.gtnewhorizon.structurelib.structure;

import net.minecraft.world.World;

interface IStructureWalker<T> {
    boolean visit(IStructureElement<T> element, World world, int x, int y, int z, int a, int b, int c);

    default boolean blockNotLoaded(
            IStructureElement<T> element, World world, int x, int y, int z, int a, int b, int c) {
        return false;
    }

    static <T> IStructureWalker<T> ignoreBlockUnloaded(IStructureWalker<T> walker) {
        return new IStructureWalker<T>() {
            @Override
            public boolean visit(IStructureElement<T> element, World world, int x, int y, int z, int a, int b, int c) {
                return walker.visit(element, world, x, y, z, a, b, c);
            }

            @Override
            public boolean blockNotLoaded(
                    IStructureElement<T> element, World world, int x, int y, int z, int a, int b, int c) {
                return walker.visit(element, world, x, y, z, a, b, c);
            }
        };
    }

    static <T> IStructureWalker<T> skipBlockUnloaded(IStructureWalker<T> walker) {
        return new IStructureWalker<T>() {
            @Override
            public boolean visit(IStructureElement<T> element, World world, int x, int y, int z, int a, int b, int c) {
                return walker.visit(element, world, x, y, z, a, b, c);
            }

            @Override
            public boolean blockNotLoaded(
                    IStructureElement<T> element, World world, int x, int y, int z, int a, int b, int c) {
                return true;
            }
        };
    }
}
