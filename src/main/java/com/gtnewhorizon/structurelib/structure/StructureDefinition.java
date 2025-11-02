package com.gtnewhorizon.structurelib.structure;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.isAir;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.notAir;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.step;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.gtnewhorizon.structurelib.coords.Position;
import com.gtnewhorizon.structurelib.coords.StructureDefinitionCoords;
import com.gtnewhorizon.structurelib.util.Vec3Impl;

import it.unimi.dsi.fastutil.chars.Char2CharOpenHashMap;
import it.unimi.dsi.fastutil.chars.CharOpenHashSet;

public class StructureDefinition<T> implements IStructureDefinition<T> {

    private final Map<Character, IStructureElement<T>> elements;
    private final Map<String, String> shapes;
    private final Map<String, IStructureElement<T>[]> structures;
    private final Map<String, Set<Vec3Impl>> occupiedSpaces;
    private final Map<String, Position<StructureDefinitionCoords>> controllers;
    private final Map<String, ListMultimap<Character, Position<StructureDefinitionCoords>>> socketLocations;

    public static <B> Builder<B> builder() {
        return new Builder<>();
    }

    private StructureDefinition(Map<Character, IStructureElement<T>> elements, Map<String, String> shapes,
            Map<String, IStructureElement<T>[]> structures, Map<String, Set<Vec3Impl>> occupiedSpaces,
            Map<String, Position<StructureDefinitionCoords>> controllers,
            Map<String, ListMultimap<Character, Position<StructureDefinitionCoords>>> socketLocations) {
        this.elements = elements;
        this.shapes = shapes;
        this.structures = structures;
        this.occupiedSpaces = occupiedSpaces;
        this.controllers = controllers;
        this.socketLocations = socketLocations;
    }

    public static class Builder<T> {

        private static final char A = '\uA000';
        private static final char B = '\uB000';
        private static final char C = '\uC000';
        private char d = '\uD000';
        private final Map<Vec3Impl, Character> navigates;
        private final Map<Character, IStructureElement<T>> elements;
        private final Map<String, String[][]> uncompiledShapes;
        private final Map<String, String> shapes;
        private final Map<String, Set<Vec3Impl>> occupiedSpaces;

        private final Map<String, Position<StructureDefinitionCoords>> controllers = new HashMap<>();
        private final Char2CharOpenHashMap socketTranslations = new Char2CharOpenHashMap();
        private final Map<String, ListMultimap<Character, Position<StructureDefinitionCoords>>> socketLocations = new HashMap<>();

        private Builder() {
            navigates = new HashMap<>();
            elements = new HashMap<>();
            uncompiledShapes = new HashMap<>();
            shapes = new HashMap<>();
            occupiedSpaces = new HashMap<>();
        }

        public Map<Character, IStructureElement<T>> getElements() {
            return elements;
        }

        public Map<String, String> getShapes() {
            return shapes;
        }

        /**
         * Adds shape. Shape is a two-dimensional string array. Each <b>character</b> inside any of these strings will
         * be later mapped to a particular type of {@link IStructureElement} as indicated by
         * {@link #addElement(Character, IStructureElement)}.
         * <p>
         * next char is next block(a) next string is next line(b) next string[] is next slice(c)
         * <p>
         * There are a few special reserved characters that are pre-mapped to some common structure elements
         * <ul>
         * <li>+ is anything but air</li>
         * <li>- is air checks</li>
         * <li>space bar is skip</li>
         * <li>~ is also skip (but marks controller position, not optional)</li>
         * <li>char {@code A000-FFFF} range is reserved for generated skips</li>
         * </ul>
         * <p>
         *
         * @param name           unlocalized/code name
         * @param structurePiece generated or written struct - DO NOT STORE IT ANYWHERE, or at least set them to null
         *                       afterwards
         * @return this builder
         */
        public Builder<T> addShape(String name, String[][] structurePiece) {
            uncompiledShapes.put(name, structurePiece);

            return this;
        }

        /**
         * @deprecated use the unboxed version
         */
        @Deprecated
        public Builder<T> addElement(Character name, IStructureElement<T> structurePiece) {
            elements.putIfAbsent(name, structurePiece);
            return this;
        }

        public Builder<T> addElement(char name, IStructureElement<T> structurePiece) {
            elements.putIfAbsent(name, structurePiece);
            return this;
        }

        /**
         * Denotes {@code name} as a socket. Sockets are virtual structure elements that record their position in the
         * structure for future retrieval. They are transformed into real structure elements and do not need their own
         * element (see {@code replacement}).
         *
         * @param name        The socket name character
         * @param replacement The replacement character
         * @see IStructureDefinition#getSocket(String, char)
         * @see IStructureDefinition#getAllSockets(String, char)
         */
        public Builder<T> addSocket(char name, char replacement) {
            socketTranslations.put(name, replacement);
            return this;
        }

        /**
         * Like {@link #addSocket(char, char)} but without the replacement. The structure must have a structure element
         * with the given character.
         *
         * @param name The socket name character
         */
        public Builder<T> addSocket(char name) {
            // Adding a name -> name entry like this isn't ideal but this is the cleanest solution
            socketTranslations.put(name, name);
            return this;
        }

        public IStructureDefinition<T> build() {
            uncompiledShapes.forEach(this::compileShape);

            Map<String, IStructureElement<T>[]> structures = compileStructureMap();

            return new StructureDefinition<>(
                    new HashMap<>(elements),
                    new HashMap<>(shapes),
                    structures,
                    occupiedSpaces,
                    controllers,
                    socketLocations);
        }

        private void compileShape(String name, String[][] structurePiece) {
            StringBuilder builder = new StringBuilder();

            if (structurePiece.length > 0) {
                for (String[] strings : structurePiece) {
                    if (strings.length > 0) {
                        for (String string : strings) {
                            builder.append(string).append(B);
                        }
                        builder.setLength(builder.length() - 1);
                    }
                    builder.append(C);
                }
                builder.setLength(builder.length() - 1);
            }

            // Always create a map for this piece, even if it doesn't have any sockets
            // noinspection UnstableApiUsage
            socketLocations.put(name, MultimapBuilder.hashKeys().arrayListValues().build());

            Set<Vec3Impl> occupiedSpace = new HashSet<>();
            // these track the global current location
            int aa = 0, bb = 0, cc = 0;
            // these track the vec3 towards next meaningful element
            int a = 0, b = 0, c = 0;
            // I do know these are pretty bad variable naming, but I have no better idea
            for (int i = 0; i < builder.length(); i++) {
                char ch = builder.charAt(i);

                // Translate the element char if it's a socket, and record the socket position
                if (socketTranslations.containsKey(ch)) {
                    socketLocations.get(name).put(ch, new Position<>(aa, bb, cc));

                    ch = socketTranslations.get(ch);
                    builder.setCharAt(i, ch);
                }

                if (ch == ' ') {
                    builder.setCharAt(i, A);
                    ch = A;
                } else if (ch == '~') {
                    builder.setCharAt(i, A);
                    ch = A;
                    occupiedSpace.add(Vec3Impl.getFromPool(aa, bb, cc));
                    controllers.put(name, new Position<>(aa, bb, cc));
                }
                if (ch == A) {
                    aa++;
                    a++;
                } else if (ch == B) {
                    aa = 0;
                    a = 0;
                    bb++;
                    b++;
                } else if (ch == C) {
                    aa = 0;
                    bb = 0;
                    a = 0;
                    b = 0;
                    c++;
                    cc++;
                } else if (a != 0 || b != 0 || c != 0) {
                    Vec3Impl vec3 = new Vec3Impl(a, b, c);
                    Character navigate = navigates.get(vec3);
                    if (navigate == null) {
                        navigate = d++;
                        navigates.put(vec3, navigate);
                        addElement(navigate, step(vec3));
                    }
                    builder.setCharAt(i - 1, navigate);
                    occupiedSpace.add(Vec3Impl.getFromPool(aa, bb, cc));
                    aa++;
                    a = 0;
                    b = 0;
                    c = 0;
                } else {
                    occupiedSpace.add(Vec3Impl.getFromPool(aa, bb, cc));
                    aa++;
                }
            }

            occupiedSpaces.put(name, occupiedSpace);

            String built = builder.toString().replaceAll("[\\uA000\\uB000\\uC000]", "");

            if (built.contains("+")) {
                addElement('+', notAir());
            }
            if (built.contains("-")) {
                addElement('-', isAir());
            }

            shapes.put(name, built);
        }

        @SuppressWarnings("unchecked")
        private Map<String, IStructureElement<T>[]> compileStructureMap() {
            CharOpenHashSet missing = findMissing();

            if (!missing.isEmpty()) {
                throw new RuntimeException("Missing Structure Element bindings for: " + missing);
            }

            Map<String, IStructureElement<T>[]> map = new HashMap<>();

            for (Entry<String, String> e : shapes.entrySet()) {
                char[] chars = e.getValue().toCharArray();
                IStructureElement<T>[] compiled = new IStructureElement[chars.length];

                int a = 0, b = 0, c = 0;

                for (int i = 0; i < chars.length; i++) {
                    char ch = chars[i];

                    IStructureElement<T> element = this.elements.get(ch);

                    compiled[i] = element;

                    if (element.isNavigating()) {
                        a = (element.resetA() ? 0 : a) + element.getStepA();
                        b = (element.resetB() ? 0 : b) + element.getStepB();
                        c = (element.resetC() ? 0 : c) + element.getStepC();
                    } else {
                        a++;
                    }
                }

                if (map.put(e.getKey(), compiled) != null) {
                    throw new IllegalStateException("Duplicate shape: " + e.getKey());
                }
            }

            return map;
        }

        private CharOpenHashSet findMissing() {
            CharOpenHashSet missing = new CharOpenHashSet();

            for (String shape : shapes.values()) {
                for (char c : shape.toCharArray()) {
                    if (!elements.containsKey(c)) {
                        missing.add(c);
                    }
                }
            }

            return missing;
        }
    }

    public Map<Character, IStructureElement<T>> getElements() {
        return elements;
    }

    public Map<String, String> getShapes() {
        return shapes;
    }

    public Map<String, IStructureElement<T>[]> getStructures() {
        return structures;
    }

    @Override
    public IStructureElement<T>[] getStructureFor(String name) {
        IStructureElement<T>[] elements = structures.get(name);
        if (elements == null) throw new NoSuchElementException(name);
        return elements;
    }

    @Override
    public boolean isContainedInStructure(String name, int offsetA, int offsetB, int offsetC) {
        Set<Vec3Impl> occupiedSpace = occupiedSpaces.get(name);
        if (occupiedSpace == null) throw new NoSuchElementException(name);
        return occupiedSpace.contains(new Vec3Impl(offsetA, offsetB, offsetC));
    }

    @Override
    public Position<StructureDefinitionCoords> getControllerPosition(String piece) {
        return controllers.get(piece);
    }

    @Override
    public StructureDefinitionCoords getCoordinateSystem(String piece) {
        var controller = controllers.get(piece);

        if (controller == null) {
            throw new IllegalArgumentException("Invalid piece: " + piece);
        }

        return new StructureDefinitionCoords(controller.x, controller.y, controller.z);
    }

    @Override
    public Position<StructureDefinitionCoords> getSocket(String piece, char socket) {
        var piecePositions = socketLocations.get(piece);

        if (piecePositions == null) {
            throw new IllegalArgumentException("Invalid piece: " + piece);
        }

        var positions = piecePositions.get(socket);

        if (positions.isEmpty()) {
            throw new IllegalArgumentException("Socket " + socket + " for piece " + piece + " does not exist");
        }

        if (positions.size() > 1) {
            throw new IllegalStateException(
                    "Socket " + socket
                            + " for piece "
                            + piece
                            + " has several positions, but expected one and only one");
        }

        return positions.get(0).copy();
    }

    @Override
    @Nonnull
    public List<Position<StructureDefinitionCoords>> getAllSockets(String piece, char socket) {
        var piecePositions = socketLocations.get(piece);

        if (piecePositions == null) {
            throw new IllegalArgumentException("Invalid piece: " + piece);
        }

        var list = new ArrayList<>(piecePositions.get(socket));

        list.replaceAll(Position::copy);

        return list;
    }
}
