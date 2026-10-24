package com.gtnewhorizon.structurelib.structure;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;

/**
 * Record the actual structure pieces emitted by construct(hintsOnly=true). This is deliberately fail-closed:
 * unknown/unloaded/invalid positions abort the entire dismantle before even a single block can be removed.
 */
public final class StructureCapture {

    private static final ThreadLocal<StructureCapture> ACTIVE = new ThreadLocal<>();
    private final Map<Position, Candidate> candidates = new LinkedHashMap<>();
    /** Occupied primary positions visited by another controller, even if its check is inconclusive. */
    private final Set<Position> potentialOverlaps = new LinkedHashSet<>();
    /** Occupied positions that no visited structure piece has recognized yet. */
    private final Set<Position> unmatched = new LinkedHashSet<>();
    /** GT hatches/busses already registered to a formed primary controller. Identity matters here. */
    private final Set<Object> registeredHatches = Collections.newSetFromMap(new IdentityHashMap<>());
    private final World world;
    private final Position controller;
    /** When non-null, inspect only positions belonging to a different, already captured structure. */
    private final Set<Position> overlapFilter;
    private boolean invalid;
    private int visits;
    private String failureReason;

    private StructureCapture(World world, Position controller) {
        this(world, controller, null);
    }

    private StructureCapture(World world, Position controller, Set<Position> overlapFilter) {
        this.world = world;
        this.controller = controller;
        this.overlapFilter = overlapFilter;
    }

    public static StructureCapture collect(IConstructable constructable, ItemStack trigger, World world,
            Position controller) {
        if (world.isRemote || ACTIVE.get() != null) return null;
        StructureCapture capture = new StructureCapture(world, controller);
        // A GT hatch check can mutate the controller's hatch lists and may report false when
        // invoked again outside a normal structure check. Snapshot only proven, registered
        // hatches before running the projector's hint traversal.
        capture.snapshotRegisteredHatches();
        invokeCapture(capture, constructable, trigger);
        // Some multiblocks (e.g. a God Forge with additional rings) describe the same position in
        // several pieces: an air placeholder in one piece can be a casing in another. Only reject
        // occupied positions after every piece has had a chance to identify them. Never dismantle
        // a position that was not positively checked by at least one structure element.
        int recoveredHatches = capture.recoverRegisteredHatches();
        if (recoveredHatches > 0) {
            StructureLib.LOGGER.info(
                    "Structure capture at ({}, {}, {}) recovered {} registered GT hatch positions",
                    controller.x,
                    controller.y,
                    controller.z,
                    recoveredHatches);
        }
        if (!capture.unmatched.isEmpty()) {
            capture.invalid = true;
            Position unknown = capture.unmatched.iterator().next();
            Block block = world.getBlock(unknown.x, unknown.y, unknown.z);
            capture.failureReason = "No structure piece recognizes the block at " + unknown.x
                    + ","
                    + unknown.y
                    + ","
                    + unknown.z
                    + " ("
                    + Block.blockRegistry.getNameForObject(block)
                    + ":"
                    + world.getBlockMetadata(unknown.x, unknown.y, unknown.z)
                    + ")";
        }
        if (capture.invalid || capture.visits == 0 || capture.candidates.isEmpty()) {
            StructureLib.LOGGER.warn(
                    "Structure capture rejected at ({}, {}, {}), tier {}, visited {}, candidates {}: {}",
                    controller.x,
                    controller.y,
                    controller.z,
                    trigger.stackSize,
                    capture.visits,
                    capture.candidates.size(),
                    capture.failureReason == null ? "no candidates" : capture.failureReason);
            return null;
        }
        return capture;
    }

    /**
     * Snapshot only hatches already registered by a formed GT controller. This is intentionally stricter than matching
     * the generic gt.blockmachines block: an unrelated machine tile must never become a dismantle candidate merely
     * because it occupies a structure coordinate. StructureLib does not depend directly on GT classes, so read the
     * conventional fields via reflection and fail closed for anything we cannot identify.
     */
    private void snapshotRegisteredHatches() {
        TileEntity tile = world.getTileEntity(controller.x, controller.y, controller.z);
        Object meta = metaTileEntity(tile);
        if (meta == null) return;
        try {
            Field formed = meta.getClass().getField("mMachine");
            if (formed.getType() != boolean.class || !formed.getBoolean(meta)) return;
            for (Class<?> type = meta.getClass(); type != null; type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || !Collection.class.isAssignableFrom(field.getType()))
                        continue;
                    String name = field.getName().toLowerCase(Locale.ROOT);
                    if (!name.contains("hatch") && !name.contains("bus") && !name.contains("buss")) continue;
                    field.setAccessible(true);
                    Object list = field.get(meta);
                    if (list instanceof Collection<?>) {
                        for (Object member : (Collection<?>) list) {
                            if (member != null) registeredHatches.add(member);
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException | SecurityException error) {
            registeredHatches.clear();
            StructureLib.LOGGER.warn("Could not inspect registered GT hatches for structure capture", error);
        }
    }

    /** Apply the fallback only to exact registered GT meta-tile instances that failed check(). */
    private int recoverRegisteredHatches() {
        int recovered = 0;
        if (registeredHatches.isEmpty()) return recovered;
        for (Position pos : new ArrayList<>(unmatched)) {
            Object meta = metaTileEntity(world.getTileEntity(pos.x, pos.y, pos.z));
            if (meta == null || !registeredHatches.contains(meta)) continue;
            Block block = world.getBlock(pos.x, pos.y, pos.z);
            candidates.putIfAbsent(pos, new Candidate(pos, block, world.getBlockMetadata(pos.x, pos.y, pos.z)));
            unmatched.remove(pos);
            recovered++;
        }
        return recovered;
    }

    private static Object metaTileEntity(TileEntity tile) {
        if (tile == null) return null;
        try {
            return tile.getClass().getMethod("getMetaTileEntity").invoke(tile);
        } catch (ReflectiveOperationException | SecurityException error) {
            return null;
        }
    }

    /**
     * Locate positions from the primary structure visited by another controller's hint traversal. For dismantle safety,
     * a failed element check is NOT proof that the other multiblock does not own a block: its check may depend on
     * channel state or on elements visited earlier. Treat these positions as potential overlaps too, so we preserve
     * rather than destroy them. No blocks outside {@code positions} are evaluated or stored. Null means unsafe
     * traversal.
     */
    public static Set<Position> collectOverlaps(IConstructable constructable, ItemStack trigger, World world,
            Position controller, Set<Position> positions) {
        if (world.isRemote || ACTIVE.get() != null || positions.isEmpty()) return null;
        StructureCapture capture = new StructureCapture(world, controller, positions);
        invokeCapture(capture, constructable, trigger);
        if (capture.invalid || capture.visits == 0) return null;
        return new LinkedHashSet<>(capture.potentialOverlaps);
    }

    private static void invokeCapture(StructureCapture capture, IConstructable constructable, ItemStack trigger) {
        ACTIVE.set(capture);
        try {
            constructable.construct(trigger.copy(), true);
        } catch (RuntimeException | LinkageError exception) {
            capture.invalid = true;
            capture.failureReason = "Constructable threw an exception";
            StructureLib.LOGGER.warn("Structure capture failed while invoking construct()", exception);
        } finally {
            ACTIVE.remove();
        }
    }

    /** Only called from the server's real, normally client-only hint traversal. */
    public static <T> boolean intercept(T object, IStructureElement<T>[] elements, World world, ExtendedFacing facing,
            int x, int y, int z, int a, int b, int c) {
        StructureCapture capture = ACTIVE.get();
        if (capture == null || capture.world != world) return false;
        boolean full = StructureUtility.iterateV2(elements, world, facing, x, y, z, a, b, c, new IStructureWalker<T>() {

            @Override
            public boolean visit(IStructureElement<T> element, World w, int px, int py, int pz, int pa, int pb,
                    int pc) {
                if (++capture.visits > 100000) {
                    capture.invalid = true;
                    capture.failureReason = "Traversal exceeded the safety limit at " + px + "," + py + "," + pz;
                    return false;
                }
                Position pos = new Position(px, py, pz);
                if (pos.equals(capture.controller) || element instanceof IStructureElementNoPlacement
                        || (capture.overlapFilter != null && !capture.overlapFilter.contains(pos)))
                    return true;
                Block block = world.getBlock(px, py, pz);
                if (block == Blocks.air) return true;
                if (capture.overlapFilter != null) {
                    // The coordinates are part of the other controller's structure piece,
                    // regardless of whether its context-sensitive check currently passes.
                    capture.potentialOverlaps.add(pos);
                    return true;
                }
                if (!element.check(object, w, px, py, pz)) {
                    if (capture.overlapFilter == null && !capture.candidates.containsKey(pos))
                        capture.unmatched.add(pos);
                    return true;
                }
                capture.unmatched.remove(pos);
                capture.candidates.putIfAbsent(pos, new Candidate(pos, block, world.getBlockMetadata(px, py, pz)));
                return true;
            }

            @Override
            public boolean blockNotLoaded(IStructureElement<T> element, World w, int px, int py, int pz, int pa, int pb,
                    int pc) {
                if (capture.overlapFilter != null && !capture.overlapFilter.contains(new Position(px, py, pz)))
                    return true;
                capture.invalid = true;
                capture.failureReason = "Unloaded chunk at " + px + "," + py + "," + pz;
                return false;
            }
        }, "dismantleCapture");
        if (!full) capture.invalid = true;
        return true;
    }

    public Iterable<Candidate> getCandidates() {
        return candidates.values();
    }

    public static final class Position {

        public final int x;
        public final int y;
        public final int z;

        public Position(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Position)) return false;
            Position p = (Position) other;
            return p.x == x && p.y == y && p.z == z;
        }

        @Override
        public int hashCode() {
            return 31 * (31 * x + y) + z;
        }
    }

    public static final class Candidate {

        public final Position pos;
        public final Block block;
        public final int metadata;

        Candidate(Position pos, Block block, int metadata) {
            this.pos = pos;
            this.block = block;
            this.metadata = metadata;
        }
    }
}
