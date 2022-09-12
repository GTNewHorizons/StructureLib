package com.gtnewhorizon.structurelib.structure;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.*;

import com.gtnewhorizon.structurelib.util.Vec3Impl;
import java.util.*;
import java.util.stream.Collectors;

public class StructureDefinition<T> implements IStructureDefinition<T> {
    private final Map<Character, IStructureElement<T>> elements;
    private final Map<String, String> shapes;
    private final Map<String, IStructureElement<T>[]> structures;
    private final Map<String, Set<Vec3Impl>> occupiedSpaces;

    public static <B> Builder<B> builder() {
        return new Builder<>();
    }

    private StructureDefinition(
            Map<Character, IStructureElement<T>> elements,
            Map<String, String> shapes,
            Map<String, IStructureElement<T>[]> structures,
            Map<String, Set<Vec3Impl>> occupiedSpaces) {
        this.elements = elements;
        this.shapes = shapes;
        this.structures = structures;
        this.occupiedSpaces = occupiedSpaces;
    }

    public static class Builder<T> {
        private static final char A = '\uA000';
        private static final char B = '\uB000';
        private static final char C = '\uC000';
        private char d = '\uD000';
        private final Map<Vec3Impl, Character> navigates;
        private final Map<Character, IStructureElement<T>> elements;
        private final Map<String, String> shapes;
        private final Map<String, Set<Vec3Impl>> occupiedSpaces;

        private Builder() {
            navigates = new HashMap<>();
            elements = new HashMap<>();
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
         * Adds shape
         * + is anything but air
         * - is air checks
         * space bar is skip
         * ~ is also skip (but marks controller position, optional. implementation wise it is a space...)
         * rest needs to be defined
         * <p>
         * next char is next block(a)
         * next string is next line(b)
         * next string[] is next slice(c)
         * <p>
         * char A000-FFFF range is reserved for generated skips
         *
         * @param name           unlocalized/code name
         * @param structurePiece generated or written struct - DO NOT STORE IT ANYWHERE, or at least set them to null afterwards
         * @return this builder
         */
        public Builder<T> addShape(String name, String[][] structurePiece) {
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
            Set<Vec3Impl> occupiedSpace = new HashSet<>();
            // these track the global current location
            int aa = 0, bb = 0, cc = 0;
            // these track the vec3 towards next meaningful element
            int a = 0, b = 0, c = 0;
            // I do know these are pretty bad variable naming, but I have no better idea
            for (int i = 0; i < builder.length(); i++) {
                char ch = builder.charAt(i);
                if (ch == ' ') {
                    builder.setCharAt(i, A);
                    ch = A;
                } else if (ch == '~') {
                    builder.setCharAt(i, A);
                    ch = A;
                    occupiedSpace.add(Vec3Impl.getFromPool(aa, bb, cc));
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
            return this;
        }

        public Builder<T> addElement(Character name, IStructureElement<T> structurePiece) {
            elements.putIfAbsent(name, structurePiece);
            return this;
        }

        public IStructureDefinition<T> build() {
            Map<String, IStructureElement<T>[]> structures = compileStructureMap();
            return new StructureDefinition<>(
                    new HashMap<>(elements), new HashMap<>(shapes), structures, occupiedSpaces);
        }

        @SuppressWarnings("unchecked")
        private Map<String, IStructureElement<T>[]> compileElementSetMap() {
            Set<Integer> missing = findMissing();
            if (missing.isEmpty()) {
                return shapes.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()
                        .chars()
                        .mapToObj(c -> (char) c)
                        .distinct()
                        .map(elements::get)
                        .toArray(IStructureElement[]::new)));
            } else {
                throw new RuntimeException("Missing Structure Element bindings for (chars as integers): "
                        + Arrays.toString(missing.toArray()));
            }
        }

        @SuppressWarnings("unchecked")
        private Map<String, IStructureElement<T>[]> compileStructureMap() {
            Set<Integer> missing = findMissing();
            if (missing.isEmpty()) {
                return shapes.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()
                        .chars()
                        .mapToObj(c -> elements.get((char) c))
                        .toArray(IStructureElement[]::new)));
            } else {
                throw new RuntimeException("Missing Structure Element bindings for (chars as integers): "
                        + Arrays.toString(missing.toArray()));
            }
        }

        private Set<Integer> findMissing() {
            return shapes.values().stream()
                    .flatMapToInt(CharSequence::chars)
                    .filter(i -> !elements.containsKey((char) i))
                    .boxed()
                    .collect(Collectors.toSet());
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
}
