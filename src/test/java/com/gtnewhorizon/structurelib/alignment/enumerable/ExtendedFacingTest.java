package com.gtnewhorizon.structurelib.alignment.enumerable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.Test;

class ExtendedFacingTest {

    @Test
    void ensureGetWorldDirectionDoesNotCrashNorReturnNull() {
        for (ExtendedFacing ef : ExtendedFacing.VALUES) {
            for (ForgeDirection src : ForgeDirection.VALID_DIRECTIONS) {
                assertNotNull(ef.getWorldDirection(src));
            }
        }
    }

    @Test
    void ensureGetWorldDirectionIsCorrect() {
        assertEquals(ForgeDirection.DOWN, ExtendedFacing.EAST_NORMAL_NONE.getWorldDirection(ForgeDirection.UP));
        assertEquals(ForgeDirection.DOWN, ExtendedFacing.NORTH_NORMAL_NONE.getWorldDirection(ForgeDirection.UP));
        assertEquals(ForgeDirection.NORTH, ExtendedFacing.NORTH_NORMAL_NONE.getWorldDirection(ForgeDirection.NORTH));
        assertEquals(ForgeDirection.NORTH, ExtendedFacing.NORTH_CLOCKWISE_NONE.getWorldDirection(ForgeDirection.NORTH));
        assertEquals(ForgeDirection.DOWN, ExtendedFacing.NORTH_CLOCKWISE_NONE.getWorldDirection(ForgeDirection.EAST));
    }
}
