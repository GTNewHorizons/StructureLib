package com.gtnewhorizon.structurelib.client.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IWorldAccess;

import org.joml.Vector3i;

import com.gtnewhorizon.structurelib.ClientProxy;

public class StructurePreviewUpdater implements IWorldAccess {

    public static final StructurePreviewUpdater INSTANCE = new StructurePreviewUpdater();

    @Override
    public void markBlockForUpdate(int x, int y, int z) {
        markBlocksForUpdate(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
    }

    @Override
    public void markBlockForRenderUpdate(int x, int y, int z) {
        markBlocksForUpdate(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
    }

    @Override
    public void markBlockRangeForRenderUpdate(int x0, int y0, int z0, int x1, int y1, int z1) {
        markBlocksForUpdate(x0 - 1, y0 - 1, z0 - 1, x1 + 1, y1 + 1, z1 + 1);
    }

    private final AxisAlignedBB box = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);

    private void markBlocksForUpdate(int x0, int y0, int z0, int x1, int y1, int z1) {
        final StructureWorld structure = ClientProxy.getStructureWorld();
        if (structure == null) return;

        final Vector3i loc = structure.getRenderLocation();
        box.setBounds(x0 - loc.x, y0 - loc.y, z0 - loc.z, x1 - loc.x, y1 - loc.y, z1 - loc.z);
        // TODO: Update chunk renderers

    }

    @Override
    public void playSound(String soundName, double x, double y, double z, float volume, float pitch) {}

    @Override
    public void playSoundToNearExcept(EntityPlayer player, String soundName, double x, double y, double z, float volume,
            float piatch) {}

    @Override
    public void spawnParticle(final String type, double x, double y, double z, double velX, double velY, double velZ) {}

    @Override
    public void onEntityCreate(Entity entity) {}

    @Override
    public void onEntityDestroy(Entity entity) {}

    @Override
    public void playRecord(String recordName, int x, int y, int z) {}

    @Override
    public void broadcastSound(int id, int x, int y, int z, int par5) {}

    @Override
    public void playAuxSFX(EntityPlayer player, int od, int x, int y, int z, int par6) {}

    @Override
    public void destroyBlockPartially(int id, int x, int y, int z, int partialDamage) {}

    @Override
    public void onStaticEntitiesChanged() {}
}
