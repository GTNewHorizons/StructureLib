package com.gtnewhorizon.structurelib.coords;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureUtility;

class CoordinateTests {

    private IStructureDefinition<?> getDefinition() {
        String[][] main = {
            { "     ", "     ", "     ", "     ", "     " },
            { "     ", "     ", "     ", "     ", "     " },
            { "     ", "     ", "  X  ", "     ", "     " },
            { "     ", "     ", "  YY ", "     ", "     " },
            { "  ~  ", "     ", "     ", "     ", "     " }
        };

        return IStructureDefinition.builder()
            .addShape("main", StructureUtility.transpose(main))
            .addSocket('X', ' ')
            .addSocket('Y', ' ')
            .build();
    }

    @Test
    void testSocketAndCoordSystemTranslationToWorld() {
        IStructureDefinition<?> def = getDefinition();

        // Note: the position is the same object between all of these transforms, it is never copied, its generic just
        // changes

        var p = def.getSocket("main", 'X');
        Assertions.assertEquals(new Position<>(2, 2, 2), p);

        var p2 = def.getCoordinateSystem("main").translateInverse(p);
        Assertions.assertEquals(new Position<>(0, -2, 2), p2);

        var p3 = ExtendedFacing.NORTH_NORMAL_NONE.asCoordinateSystem().translateInverse(p2);
        Assertions.assertEquals(new Position<>(0, 2, 2), p3);

        var p4 = ControllerRelativeCoords.translateInverse(p3, 5, 5, 5);
        Assertions.assertEquals(new Position<>(5, 7, 7), p4);
    }

    @Test
    void testSocketAndCoordSystemTranslationToStructureDef() {
        IStructureDefinition<?> def = getDefinition();

        // Note: the position is the same object between all of these transforms, it is never copied, its generic just
        // changes

        var p = new Position<WorldCoords>(5, 7, 7);

        var p2 = ControllerRelativeCoords.translate(p, 5, 5, 5);
        Assertions.assertEquals(new Position<>(0, 2, 2), p2);

        var p3 = ExtendedFacing.NORTH_NORMAL_NONE.asCoordinateSystem().translate(p2);
        Assertions.assertEquals(new Position<>(0, -2, 2), p3);

        var p4 = def.getCoordinateSystem("main").translate(p3);
        Assertions.assertEquals(new Position<>(2, 2, 2), p4);

        Assertions.assertEquals(def.getSocket("main", 'X'), p4);
    }

    @Test
    void testSocketGetters() {
        IStructureDefinition<?> def = getDefinition();

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> def.getSocket("invalid", 'X'));
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> def.getSocket("main", 'Y'));
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> def.getSocket("main", 'Z'));

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> def.getAllSockets("invalid", 'X'));

        Assertions.assertEquals(
            Collections.emptyList(),
            def.getAllSockets("main", 'Z'));
        Assertions.assertEquals(
            Arrays.asList(new Position<>(2, 2, 2)),
            def.getAllSockets("main", 'X'));
        Assertions.assertEquals(
            Arrays.asList(new Position<>(2, 3, 2), new Position<>(3, 3, 2)),
            def.getAllSockets("main", 'Y'));
    }
}
