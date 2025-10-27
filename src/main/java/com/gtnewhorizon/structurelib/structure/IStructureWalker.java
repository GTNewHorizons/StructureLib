package com.gtnewhorizon.structurelib.structure;

import net.minecraft.world.World;

/// A functional interface that is used to iterate over an arbitrary structure.
@FunctionalInterface
public interface IStructureWalker<T> {

    /// Called once per structure element.
    ///
    /// @param element The current element
    /// @param world The current world
    /// @param x The absolute X position of the element in world coordinates
    /// @param y The absolute Y position of the element in world coordinates
    /// @param z The absolute Z position of the element in world coordinates
    /// @param a The relative A position of the element in structure-local coordinates (pre transform)
    /// @param b The relative B position of the element in structure-local coordinates (pre transform)
    /// @param c The relative C position of the element in structure-local coordinates (pre transform)
    /// @return False to stop iterating, true to continue.
    boolean visit(IStructureElement<T> element, World world, int x, int y, int z, int a, int b, int c);

    default boolean blockNotLoaded(IStructureElement<T> element, World world, int x, int y, int z, int a, int b,
            int c) {
        return false;
    }

    static <T> IStructureWalker<T> ignoreBlockUnloaded(IStructureWalker<T> walker) {
        return new IStructureWalker<T>() {

            @Override
            public boolean visit(IStructureElement<T> element, World world, int x, int y, int z, int a, int b, int c) {
                return walker.visit(element, world, x, y, z, a, b, c);
            }

            @Override
            public boolean blockNotLoaded(IStructureElement<T> element, World world, int x, int y, int z, int a, int b,
                    int c) {
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
            public boolean blockNotLoaded(IStructureElement<T> element, World world, int x, int y, int z, int a, int b,
                    int c) {
                return true;
            }
        };
    }
}
