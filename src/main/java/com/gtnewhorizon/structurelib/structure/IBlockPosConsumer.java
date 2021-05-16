package com.gtnewhorizon.structurelib.structure;

import net.minecraft.world.World;

public interface IBlockPosConsumer {
	void consume(World world, int x, int y, int z);
}
