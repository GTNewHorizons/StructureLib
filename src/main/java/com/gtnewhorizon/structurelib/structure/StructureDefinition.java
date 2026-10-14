package com.gtnewhorizon.structurelib.structure;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.isAir;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.notAir;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.step;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import com.gtnewhorizon.structurelib.util.Vec3Impl;

public class StructureDefinition<T> implements IStructureDefinition<T> {

    private final Map<Character, IStructureElement<T>> elements;
    private final Map<String, String> shapes;
    private final Map<String, IStructureElement<T>[]> structures;
    private final Map<String, OccupiedSpace> occupiedSpaces;
    private final Map<String, IChannelDefinition> channelDefinitions;
    private final Set<String> explicitChannels;
    private final Collection<IChannelDefinition> channelDefinitionValues;
    private final Collection<IChannelDefinition> explicitChannelDefinitions;

    public static <B> Builder<B> builder() {
        return new Builder<>();
    }

    private StructureDefinition(Map<Character, IStructureElement<T>> elements, Map<String, String> shapes,
            Map<String, IStructureElement<T>[]> structures, Map<String, OccupiedSpace> occupiedSpaces,
            Map<String, IChannelDefinition> channelDefinitions, Set<String> explicitChannels) {
        this.elements = elements;
        this.shapes = shapes;
        this.structures = structures;
        this.occupiedSpaces = occupiedSpaces;
        this.channelDefinitions = Collections.unmodifiableMap(new HashMap<>(channelDefinitions));
        this.explicitChannels = Collections.unmodifiableSet(new HashSet<>(explicitChannels));
        this.channelDefinitionValues = Collections.unmodifiableCollection(this.channelDefinitions.values());
        this.explicitChannelDefinitions = Collections.unmodifiableList(
                this.explicitChannels.stream().map(this.channelDefinitions::get).collect(Collectors.toList()));
    }

    public static class Builder<T> {

        private static final char A = '\uA000';
        private static final char B = '\uB000';
        private static final char C = '\uC000';
        private char d = '\uD000';
        private final Map<Vec3Impl, Character> navigates;
        private final Map<Character, IStructureElement<T>> elements;
        private final Map<String, String> shapes;
        private final Map<String, OccupiedSpace> occupiedSpaces;
        private final Map<String, IChannelDefinition> channels;
        private final Set<String> explicitChannels;

        private Builder() {
            navigates = new HashMap<>();
            elements = new HashMap<>();
            shapes = new HashMap<>();
            occupiedSpaces = new HashMap<>();
            channels = new HashMap<>();
            explicitChannels = new HashSet<>();
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
            int sizeA = 0;
            int sizeB = 0;
            long occupiedCount = 0;
            for (String[] strings : structurePiece) {
                sizeB = Math.max(sizeB, strings.length);
                for (String string : strings) {
                    sizeA = Math.max(sizeA, string.length());
                    for (int i = 0; i < string.length(); i++) {
                        if (string.charAt(i) != ' ') occupiedCount++;
                    }
                }
            }
            OccupiedSpace occupiedSpace = new OccupiedSpace(sizeA, sizeB, structurePiece.length, occupiedCount);
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
                    occupiedSpace.add(aa, bb, cc);
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
                    occupiedSpace.add(aa, bb, cc);
                    aa++;
                    a = 0;
                    b = 0;
                    c = 0;
                } else {
                    occupiedSpace.add(aa, bb, cc);
                    aa++;
                }
            }

            occupiedSpaces.put(name, occupiedSpace);

            boolean hasPlus = false;
            boolean hasMinus = false;

            int write = 0;
            for (int read = 0; read < builder.length(); read++) {
                char ch = builder.charAt(read);
                if (ch == A || ch == B || ch == C) {
                    continue;
                }
                if (ch == '+') {
                    hasPlus = true;
                } else if (ch == '-') {
                    hasMinus = true;
                }
                if (read != write) builder.setCharAt(write, ch);
                write++;
            }
            builder.setLength(write);

            if (hasPlus) {
                addElement('+', notAir());
            }
            if (hasMinus) {
                addElement('-', isAir());
            }
            shapes.put(name, builder.toString());
            return this;
        }

        /**
         * @deprecated use the unboxed version
         */
        @Deprecated
        public Builder<T> addElement(Character name, IStructureElement<T> structurePiece) {
            if (elements.putIfAbsent(name, structurePiece) == null) registerElementChannel(structurePiece);
            return this;
        }

        public Builder<T> addElement(char name, IStructureElement<T> structurePiece) {
            if (elements.putIfAbsent(name, structurePiece) == null) registerElementChannel(structurePiece);
            return this;
        }

        public Builder<T> addChannel(IChannelDefinition definition) {
            validateChannelDefinition(definition);
            channels.put(definition.getChannel(), definition);
            explicitChannels.add(definition.getChannel());
            return this;
        }

        public Builder<T> addChannel(String channel, int minimumValue, int maximumValue) {
            return addChannel(IChannelDefinition.bounded(channel, minimumValue, maximumValue));
        }

        private void registerElementChannel(IStructureElement<T> structurePiece) {
            for (String channel : structurePiece.getChannels()) {
                validateChannelName(channel);
                if (!explicitChannels.contains(channel)) {
                    channels.putIfAbsent(channel, IChannelDefinition.unbounded(channel));
                }
            }
        }

        private void validateChannelDefinition(IChannelDefinition definition) {
            if (definition == null) throw new IllegalArgumentException("Channel definition cannot be null");
            validateChannelName(definition.getChannel());
            if (definition.getMinimumValue() < 1 || definition.getMaximumValue() < definition.getMinimumValue()) {
                throw new IllegalArgumentException("Invalid channel definition values");
            }
        }

        private void validateChannelName(String channel) {
            if (channel == null || channel.isEmpty() || !channel.toLowerCase(Locale.ROOT).equals(channel)) {
                throw new IllegalArgumentException("Channel name must be non-empty and lower case");
            }
        }

        public IStructureDefinition<T> build() {
            for (IStructureElement<T> element : elements.values()) {
                registerElementChannel(element);
            }
            Map<String, IStructureElement<T>[]> structures = compileStructureMap();
            return new StructureDefinition<>(
                    new HashMap<>(elements),
                    new HashMap<>(shapes),
                    structures,
                    occupiedSpaces,
                    channels,
                    explicitChannels);
        }

        @SuppressWarnings("unchecked")
        private Map<String, IStructureElement<T>[]> compileElementSetMap() {
            Set<Integer> missing = findMissing();
            if (missing.isEmpty()) {
                return shapes.entrySet().stream().collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().chars().mapToObj(c -> (char) c).distinct().map(elements::get)
                                        .toArray(IStructureElement[]::new)));
            } else {
                throw new RuntimeException(
                        "Missing Structure Element bindings for (chars as integers): "
                                + Arrays.toString(missing.toArray()));
            }
        }

        @SuppressWarnings("unchecked")
        private Map<String, IStructureElement<T>[]> compileStructureMap() {
            Set<Integer> missing = findMissing();
            if (missing.isEmpty()) {
                return shapes.entrySet().stream().collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().chars().mapToObj(c -> elements.get((char) c))
                                        .toArray(IStructureElement[]::new)));
            } else {
                throw new RuntimeException(
                        "Missing Structure Element bindings for (chars as integers): "
                                + Arrays.toString(missing.toArray()));
            }
        }

        private Set<Integer> findMissing() {
            return shapes.values().stream().flatMapToInt(CharSequence::chars)
                    .filter(i -> !elements.containsKey((char) i)).boxed().collect(Collectors.toSet());
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
    public Collection<IChannelDefinition> getChannelDefinitions() {
        return channelDefinitionValues;
    }

    @Override
    public Collection<IChannelDefinition> getExplicitChannelDefinitions() {
        return explicitChannelDefinitions;
    }

    @Override
    public IChannelDefinition getChannelDefinition(String channel) {
        return channelDefinitions.get(channel);
    }

    @Override
    public IStructureElement<T>[] getStructureFor(String name) {
        IStructureElement<T>[] elements = structures.get(name);
        if (elements == null) throw new NoSuchElementException(name);
        return elements;
    }

    @Override
    public boolean isContainedInStructure(String name, int offsetA, int offsetB, int offsetC) {
        OccupiedSpace occupiedSpace = occupiedSpaces.get(name);
        if (occupiedSpace == null) throw new NoSuchElementException(name);
        return occupiedSpace.contains(offsetA, offsetB, offsetC);
    }

    private static class OccupiedSpace {

        private final int sizeA;
        private final int sizeB;
        private final int sizeC;
        private final BitSet occupied;
        private final Set<Vec3Impl> sparseOccupied;

        private OccupiedSpace(int sizeA, int sizeB, int sizeC, long occupiedCount) {
            this.sizeA = sizeA;
            this.sizeB = sizeB;
            this.sizeC = sizeC;
            long volume = (long) sizeA * sizeB * sizeC;
            if (volume <= Integer.MAX_VALUE && volume <= Math.max(1024L, occupiedCount * 256L)) {
                occupied = new BitSet((int) volume);
                sparseOccupied = null;
            } else {
                occupied = null;
                sparseOccupied = new HashSet<>();
            }
        }

        private void add(int a, int b, int c) {
            if (occupied != null) {
                occupied.set(index(a, b, c));
            } else {
                sparseOccupied.add(new Vec3Impl(a, b, c));
            }
        }

        private boolean contains(int a, int b, int c) {
            if (a < 0 || b < 0 || c < 0 || a >= sizeA || b >= sizeB || c >= sizeC) return false;
            if (occupied != null) return occupied.get(index(a, b, c));
            return sparseOccupied.contains(new Vec3Impl(a, b, c));
        }

        private int index(int a, int b, int c) {
            return (c * sizeB + b) * sizeA + a;
        }
    }
}
