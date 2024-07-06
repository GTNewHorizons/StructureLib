package com.gtnewhorizon.structurelib.event;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public interface BlockChangeListener {

    void onBlockChange(World world, Chunk chunk, int x, int y, int z, Block oldBlock, Block newBlock, int oldMeta,
            int newMeta);
}
