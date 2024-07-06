package com.gtnewhorizon.structurelib.event;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class BlockChangeNotifier {

    private static final List<BlockChangeListener> listeners = new ArrayList<>();

    public static synchronized void addListener(BlockChangeListener listener) {
        listeners.add(listener);
    }

    public static void onBlockChange(World world, Chunk chunk, int x, int y, int z, Block oldBlock, Block newBlock,
            int oldMeta, int newMeta) {
        if (oldBlock == newBlock && oldMeta == newMeta) return;

        if (chunk == null) {
            chunk = world.getChunkFromBlockCoords(x, z);
        }

        final int numListeners = listeners.size();
        for (int i = 0; i < numListeners; i++) {
            final BlockChangeListener listener = listeners.get(i);
            listener.onBlockChange(world, chunk, x, y, z, oldBlock, newBlock, oldMeta, newMeta);
        }
    }
}
