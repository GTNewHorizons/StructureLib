package com.gtnewhorizon.structurelib.structure;

import com.gtnewhorizon.structurelib.util.Vec3Impl;

import java.util.*;
import java.util.stream.Collectors;

import static com.gtnewhorizon.structurelib.StructureLib.DEBUG_MODE;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.*;

public class StructureDefinition<T> implements IStructureDefinition<T> {
	private final Map<Character, IStructureElement<T>> elements;
	private final Map<String, String> shapes;
	private final Map<String, IStructureElement<T>[]> structures;

	public static <B> Builder<B> builder() {
		return new Builder<>();
	}

	private StructureDefinition(
			Map<Character, IStructureElement<T>> elements,
			Map<String, String> shapes,
			Map<String, IStructureElement<T>[]> structures) {
		this.elements = elements;
		this.shapes = shapes;
		this.structures = structures;
	}

	public static class Builder<T> {
		private static final char A = '\uA000';
		private static final char B = '\uB000';
		private static final char C = '\uC000';
		private char d = '\uD000';
		private final Map<Vec3Impl, Character> navigates;
		private final Map<Character, IStructureElement<T>> elements;
		private final Map<String, String> shapes;

		private Builder() {
			navigates = new HashMap<>();
			elements = new HashMap<>();
			shapes = new HashMap<>();
		}

		public Map<Character, IStructureElement<T>> getElements() {
			return elements;
		}

		public Map<String, String> getShapes() {
			return shapes;
		}

		/**
		 * Adds shape
		 * +- is air/no air checks
		 * space bar is skip
		 * ~ is also skip (but marks controller position, optional and logically it is a space...)
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
			int a = 0, b = 0, c = 0;
			for (int i = 0; i < builder.length(); i++) {
				char ch = builder.charAt(i);
				if (ch == ' ' || ch == '~') {
					builder.setCharAt(i, A);
					ch = A;
				}
				if (ch == A) {
					a++;
				} else if (ch == B) {
					a = 0;
					b++;
				} else if (ch == C) {
					a = 0;
					b = 0;
					c++;
				} else if (a != 0 || b != 0 || c != 0) {
					Vec3Impl vec3 = new Vec3Impl(a, b, c);
					Character navigate = navigates.get(vec3);
					if (navigate == null) {
						navigate = d++;
						navigates.put(vec3, navigate);
						addElement(navigate, step(vec3));
					}
					builder.setCharAt(i - 1, navigate);
					a = 0;
					b = 0;
					c = 0;
				}
			}

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
			if (DEBUG_MODE) {
				return new StructureDefinition<>(new HashMap<>(elements), new HashMap<>(shapes), structures);
			} else {
				return structures::get;
			}
		}

		@SuppressWarnings("unchecked")
		private Map<String, IStructureElement<T>[]> compileElementSetMap() {
			Set<Integer> missing = findMissing();
			if (missing.isEmpty()) {
				return shapes.entrySet().stream()
						.collect(Collectors.toMap(Map.Entry::getKey,
								e -> e.getValue().chars()
										.mapToObj(c -> (char) c)
										.distinct()
										.map(elements::get)
										.toArray(IStructureElement[]::new)
						));
			} else {
				throw new RuntimeException("Missing Structure Element bindings for (chars as integers): " +
						Arrays.toString(missing.toArray()));
			}
		}

		@SuppressWarnings("unchecked")
		private Map<String, IStructureElement<T>[]> compileStructureMap() {
			Set<Integer> missing = findMissing();
			if (missing.isEmpty()) {
				return shapes.entrySet().stream()
						.collect(Collectors.toMap(Map.Entry::getKey,
								e -> e.getValue().chars().mapToObj(c -> elements.get((char) c)).toArray(IStructureElement[]::new)
						));
			} else {
				throw new RuntimeException("Missing Structure Element bindings for (chars as integers): " +
						Arrays.toString(missing.toArray()));
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
		return structures.get(name);
	}
}