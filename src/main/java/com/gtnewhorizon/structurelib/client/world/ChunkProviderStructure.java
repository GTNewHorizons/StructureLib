package com.gtnewhorizon.structurelib.client.world;

import java.util.List;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class ChunkProviderStructure implements IChunkProvider {

    private final StructureWorld world;
    private final Long2ObjectMap<Chunk> chunks = new Long2ObjectOpenHashMap<>();

    public ChunkProviderStructure(StructureWorld world) {
        this.world = world;
    }

    @Override
    public boolean chunkExists(int x, int z) {
        return true;
    }

    @Override
    public Chunk provideChunk(int x, int z) {
        return chunks.computeIfAbsent(ChunkCoordIntPair.chunkXZ2Int(x, z), k -> new Chunk(world, x, z));
    }

    @Override
    public Chunk loadChunk(int x, int z) {
        return chunks.get(ChunkCoordIntPair.chunkXZ2Int(x, z));
    }

    @Override
    public void populate(IChunkProvider provider, int x, int z) {

    }

    @Override
    public boolean saveChunks(boolean saveAll, IProgressUpdate progress) {
        return false;
    }

    @Override
    public boolean unloadQueuedChunks() {
        return false;
    }

    @Override
    public boolean canSave() {
        return false;
    }

    @Override
    public String makeString() {
        return "ChunkProviderStructure";
    }

    @Override
    public List<BiomeGenBase.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, int x, int y, int z) {
        return null;
    }

    @Override
    public ChunkPosition func_147416_a(World world, String structure, int x, int y, int z) {
        return null;
    }

    @Override
    public int getLoadedChunkCount() {
        return 0;
    }

    @Override
    public void recreateStructures(int x, int z) {

    }

    @Override
    public void saveExtraData() {

    }
}
