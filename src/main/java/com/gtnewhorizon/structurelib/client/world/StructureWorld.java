package com.gtnewhorizon.structurelib.client.world;

import java.util.Collections;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructableProvider;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.mojang.authlib.GameProfile;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.interfaces.tileentity.ITurnable;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class StructureWorld extends World {

    // Static variables
    public static final Vector3i PLACE_POSITION = new Vector3i(0, 64, 0);
    public static final int MAX_PLACE_ROUNDS = 2000;

    private static final ISaveHandler STRUCTURE_SAVE_HANDLER = new StructureSaveHandler();
    private static final WorldSettings WORLD_SETTINGS = new WorldSettings(
            0,
            WorldSettings.GameType.CREATIVE,
            false,
            false,
            WorldType.FLAT);

    // Instance variables
    private StructureFakePlayer fakeMultiblockBuilder;
    public final LongSet placedBlocks = new LongOpenHashSet();

    private final Vector3f minPos = new Vector3f(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    private final Vector3f maxPos = new Vector3f(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);

    private final Vector3f size = new Vector3f();

    public StructureFakePlayer getFakeMultiblockBuilder() {
        return fakeMultiblockBuilder;
    }

    private void updateSize() {
        size.set(maxPos.x - minPos.x + 1, maxPos.y - minPos.y + 1, maxPos.z - minPos.z + 1);

    }

    public Vector3f getSize() {
        return size;
    }

    public Vector3f getMinPos() {
        return minPos;
    }

    public Vector3f getMaxPos() {
        return maxPos;
    }

    public StructureWorld() {
        super(STRUCTURE_SAVE_HANDLER, "StructureLibFakeWorld", WORLD_SETTINGS, null, new Profiler());
        // Guarantee the dimension ID was not reset by the provider
        this.provider.setDimension(Integer.MAX_VALUE);
        int providerDim = this.provider.dimensionId;
        this.provider.worldObj = this;
        this.provider.setDimension(providerDim);
        this.chunkProvider = this.createChunkProvider();
        this.calculateInitialSkylight();
        this.calculateInitialWeatherBody();
        this.fakeMultiblockBuilder = createFakeBuilder();
        this.unloadEntities(Collections.singletonList(fakeMultiblockBuilder));

        updateEntitiesForNEI();
    }

    protected StructureFakePlayer createFakeBuilder() {
        final String name = StructureLibAPI.MOD_NAME;
        return new StructureFakePlayer(this, new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
    }

    @Override
    public void updateEntities() {}

    public void updateEntitiesForNEI() {
        super.updateEntities();
    }

    @Override
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {}

    @Override
    protected IChunkProvider createChunkProvider() {
        return new ChunkProviderStructure(this);
    }

    @Override
    protected int func_152379_p() {
        return 0;
    }

    @Override
    public Entity getEntityByID(int p_73045_1_) {
        return null;
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        if (x > maxPos.x || y > maxPos.y || z > maxPos.z || x < minPos.x || y < minPos.y || z < minPos.z) {
            return Blocks.air;
        }
        final Block block = super.getBlock(x, y, z);
        return block != null ? block : Blocks.air;
    }

    @Override
    public boolean setBlock(int x, int y, int z, Block blockIn, int metadataIn, int flags) {
        if (x < -30000000 || z < -30000000 || x >= 30000000 || z >= 30000000 || y < 0 || y > 255) {
            return false;
        }

        final long longPos = CoordinatePacker.pack(x, y, z);
        if (blockIn == Blocks.air) {
            placedBlocks.remove(longPos);
        } else {
            placedBlocks.add(longPos);
        }

        minPos.x = Math.min(minPos.x, x);
        minPos.y = Math.min(minPos.y, y);
        minPos.z = Math.min(minPos.z, z);
        maxPos.x = Math.max(maxPos.x, x);
        maxPos.y = Math.max(maxPos.y, y);
        maxPos.z = Math.max(maxPos.z, z);
        updateSize();

        return super.setBlock(x, y, z, blockIn, metadataIn, flags);
    }

    @Override
    public boolean updateLightByType(EnumSkyBlock skyBlock, int x, int y, int z) {
        return true;
    }

    private final int tierIndex = 1;
    private ItemStack triggerStack = null;

    public ItemStack getBuildTriggerStack() {
        if (triggerStack == null) {
            triggerStack = new ItemStack(StructureLibAPI.getDefaultHologramItem(), tierIndex);
        }
        return triggerStack;
    }

    public boolean placeMultiBlock(ItemStack stack) {
        final Block block = Block.getBlockFromItem(stack.getItem());
        final int metadata = stack.getItemDamage();

        IConstructable constructable = null;
        final ItemStack stackCopy = stack.copy();
        // GT Multiblock only
        stackCopy.getItem().onItemUse(
                stackCopy,
                fakeMultiblockBuilder,
                this,
                PLACE_POSITION.x,
                PLACE_POSITION.y,
                PLACE_POSITION.z,
                0,
                PLACE_POSITION.x,
                PLACE_POSITION.y,
                PLACE_POSITION.z);
        // Other Multiblocks
        // setBlock(PLACE_LOCATION.x, PLACE_LOCATION.y, PLACE_LOCATION.z, block, metadata, 3);
        final TileEntity te = getTileEntity(PLACE_POSITION.x, PLACE_POSITION.y, PLACE_POSITION.z);
        if (te == null) return false;

        if (te instanceof ITurnable turnable) {
            turnable.setFrontFacing(ForgeDirection.SOUTH);
        }

        if (!StructureLibAPI.isInstrumentEnabled()) {
            StructureLibAPI.enableInstrument(StructureLibAPI.MOD_ID);
        }

        if (te instanceof IGregTechTileEntity gregTechTileEntity) {
            final IMetaTileEntity mte = gregTechTileEntity.getMetaTileEntity();
            if (mte instanceof ISurvivalConstructable survivalConstructable) {
                constructable = survivalConstructable;
                // int result, iterations = 0;
                // do {
                // result = survivalConstructable.survivalConstruct(
                // getBuildTriggerStack(),
                // Integer.MAX_VALUE,
                // ISurvivalBuildEnvironment.create(CreativeItemSource.instance, fakeMultiblockBuilder));
                // iterations++;
                // } while (result > 0 && iterations < MAX_PLACE_ROUNDS);
            } else if (te instanceof IConstructableProvider iConstructableProvider) {
                constructable = iConstructableProvider.getConstructable();
            } else if (te instanceof IConstructable iConstructable) {
                constructable = iConstructable;
            }
        }

        if (constructable != null) {
            constructable.construct(getBuildTriggerStack(), false);
        }

        if (StructureLibAPI.isInstrumentEnabled()) {
            StructureLibAPI.disableInstrument();
        }

        // A single tick is needed for some non GT multiblocks to complete
        updateEntitiesForNEI();

        return true;
    }

    public MovingObjectPosition rayTraceBlockswithTargetMap(Vec3 start, Vec3 end, LongSet targetedBlocks) {
        return rayTraceBlockswithTargetMap(start, end, targetedBlocks, false, false, false);
    }

    public MovingObjectPosition rayTraceBlockswithTargetMap(Vec3 start, Vec3 end, LongSet targetedBlocks,
            boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock) {
        if (!Double.isNaN(start.xCoord) && !Double.isNaN(start.yCoord) && !Double.isNaN(start.zCoord)) {
            if (!Double.isNaN(end.xCoord) && !Double.isNaN(end.yCoord) && !Double.isNaN(end.zCoord)) {
                int i = MathHelper.floor_double(end.xCoord);
                int j = MathHelper.floor_double(end.yCoord);
                int k = MathHelper.floor_double(end.zCoord);
                int l = MathHelper.floor_double(start.xCoord);
                int i1 = MathHelper.floor_double(start.yCoord);
                int j1 = MathHelper.floor_double(start.zCoord);
                Block block = this.getBlock(l, i1, j1);
                int k1 = this.getBlockMetadata(l, i1, j1);

                if ((!ignoreBlockWithoutBoundingBox || block.getCollisionBoundingBoxFromPool(this, l, i1, j1) != null)
                        && block.canCollideCheck(k1, stopOnLiquid)) {
                    MovingObjectPosition movingobjectposition = block.collisionRayTrace(this, l, i1, j1, start, end);

                    if (movingobjectposition != null && isBlockTargeted(movingobjectposition, targetedBlocks)) {
                        return movingobjectposition;
                    }
                }

                MovingObjectPosition movingobjectposition2 = null;
                k1 = 200;

                while (k1-- >= 0) {
                    if (Double.isNaN(start.xCoord) || Double.isNaN(start.yCoord) || Double.isNaN(start.zCoord)) {
                        return null;
                    }

                    if (l == i && i1 == j && j1 == k) {
                        return returnLastUncollidableBlock ? movingobjectposition2 : null;
                    }

                    boolean flag6 = true;
                    boolean flag3 = true;
                    boolean flag4 = true;
                    double d0 = 999.0D;
                    double d1 = 999.0D;
                    double d2 = 999.0D;

                    if (i > l) {
                        d0 = (double) l + 1.0D;
                    } else if (i < l) {
                        d0 = (double) l + 0.0D;
                    } else {
                        flag6 = false;
                    }

                    if (j > i1) {
                        d1 = (double) i1 + 1.0D;
                    } else if (j < i1) {
                        d1 = (double) i1 + 0.0D;
                    } else {
                        flag3 = false;
                    }

                    if (k > j1) {
                        d2 = (double) j1 + 1.0D;
                    } else if (k < j1) {
                        d2 = (double) j1 + 0.0D;
                    } else {
                        flag4 = false;
                    }

                    double d3 = 999.0D;
                    double d4 = 999.0D;
                    double d5 = 999.0D;
                    double d6 = end.xCoord - start.xCoord;
                    double d7 = end.yCoord - start.yCoord;
                    double d8 = end.zCoord - start.zCoord;

                    if (flag6) {
                        d3 = (d0 - start.xCoord) / d6;
                    }

                    if (flag3) {
                        d4 = (d1 - start.yCoord) / d7;
                    }

                    if (flag4) {
                        d5 = (d2 - start.zCoord) / d8;
                    }

                    boolean flag5 = false;
                    byte b0;

                    if (d3 < d4 && d3 < d5) {
                        if (i > l) {
                            b0 = 4;
                        } else {
                            b0 = 5;
                        }

                        start.xCoord = d0;
                        start.yCoord += d7 * d3;
                        start.zCoord += d8 * d3;
                    } else if (d4 < d5) {
                        if (j > i1) {
                            b0 = 0;
                        } else {
                            b0 = 1;
                        }

                        start.xCoord += d6 * d4;
                        start.yCoord = d1;
                        start.zCoord += d8 * d4;
                    } else {
                        if (k > j1) {
                            b0 = 2;
                        } else {
                            b0 = 3;
                        }

                        start.xCoord += d6 * d5;
                        start.yCoord += d7 * d5;
                        start.zCoord = d2;
                    }

                    Vector3d vec32 = new Vector3d(start.xCoord, start.yCoord, start.zCoord);
                    l = (int) (vec32.x = MathHelper.floor_double(start.xCoord));

                    if (b0 == 5) {
                        --l;
                        ++vec32.x;
                    }

                    i1 = (int) (vec32.y = MathHelper.floor_double(start.yCoord));

                    if (b0 == 1) {
                        --i1;
                        ++vec32.y;
                    }

                    j1 = (int) (vec32.z = MathHelper.floor_double(start.zCoord));

                    if (b0 == 3) {
                        --j1;
                        ++vec32.z;
                    }

                    Block block1 = this.getBlock(l, i1, j1);
                    int l1 = this.getBlockMetadata(l, i1, j1);

                    if (!ignoreBlockWithoutBoundingBox
                            || block1.getCollisionBoundingBoxFromPool(this, l, i1, j1) != null) {
                        if (block1.canCollideCheck(l1, stopOnLiquid)) {
                            MovingObjectPosition movingobjectposition1 = block1
                                    .collisionRayTrace(this, l, i1, j1, start, end);

                            if (movingobjectposition1 != null
                                    && isBlockTargeted(movingobjectposition1, targetedBlocks)) {
                                return movingobjectposition1;
                            }
                        } else {
                            movingobjectposition2 = new MovingObjectPosition(l, i1, j1, b0, start, false);
                        }
                    }
                }

                return returnLastUncollidableBlock ? movingobjectposition2 : null;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private boolean isBlockTargeted(MovingObjectPosition result, LongSet targetedBlocks) {
        return targetedBlocks.contains(CoordinatePacker.pack(result.blockX, result.blockY, result.blockZ));
    }
}
