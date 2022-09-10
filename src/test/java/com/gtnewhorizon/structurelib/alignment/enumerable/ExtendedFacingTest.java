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
        assertEquals(ForgeDirection.UP, ExtendedFacing.EAST_NORMAL_NONE.getWorldDirection(ForgeDirection.UP));
    }
}
