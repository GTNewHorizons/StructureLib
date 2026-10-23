package com.gtnewhorizon.structurelib.structure;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
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
    /** Occupied positions that no visited structure piece has recognized yet. */
    private final Set<Position> unmatched = new LinkedHashSet<>();
    private final World world;
    private final Position controller;
    private boolean invalid;
    private int visits;
    private String failureReason;

    private StructureCapture(World world, Position controller) {
        this.world = world;
        this.controller = controller;
    }

    public static StructureCapture collect(IConstructable constructable, ItemStack trigger, World world,
            Position controller) {
        if (world.isRemote || ACTIVE.get() != null) return null;
        StructureCapture capture = new StructureCapture(world, controller);
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
        // Some multiblocks (e.g. a God Forge with additional rings) describe the same position in
        // several pieces: an air placeholder in one piece can be a casing in another. Only reject
        // occupied positions after every piece has had a chance to identify them. Never dismantle
        // a position that was not positively checked by at least one structure element.
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
                if (pos.equals(capture.controller) || element instanceof IStructureElementNoPlacement) return true;
                Block block = world.getBlock(px, py, pz);
                if (block == Blocks.air) return true;
                if (!element.check(object, w, px, py, pz)) {
                    if (!capture.candidates.containsKey(pos)) capture.unmatched.add(pos);
                    return true;
                }
                capture.unmatched.remove(pos);
                capture.candidates.putIfAbsent(pos, new Candidate(pos, block, world.getBlockMetadata(px, py, pz)));
                return true;
            }

            @Override
            public boolean blockNotLoaded(IStructureElement<T> element, World w, int px, int py, int pz, int pa, int pb,
                    int pc) {
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
