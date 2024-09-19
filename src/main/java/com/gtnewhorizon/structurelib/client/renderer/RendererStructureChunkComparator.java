package com.gtnewhorizon.structurelib.client.renderer;

import java.util.Comparator;

import org.joml.Vector3d;
import org.joml.Vector3i;

public class RendererStructureChunkComparator implements Comparator<RendererStructureChunk> {

    private final Vector3d position = new Vector3d();
    private final Vector3d renderPosition = new Vector3d();

    @Override
    public int compare(RendererStructureChunk RendererStructureChunk1, RendererStructureChunk RendererStructureChunk2) {
        if (RendererStructureChunk1.isInFrustrum && !RendererStructureChunk2.isInFrustrum) {
            return -1;
        } else if (!RendererStructureChunk1.isInFrustrum && RendererStructureChunk2.isInFrustrum) {
            return 1;
        } else {
            final double dist1 = position.distanceSquared(RendererStructureChunk1.centerPosition);
            final double dist2 = position.distanceSquared(RendererStructureChunk2.centerPosition);
            return Double.compare(dist1, dist2);
        }
    }

    public void setPosition(Vector3d playerPosition, Vector3i position) {
        this.renderPosition.set(position);
        this.position.set(playerPosition).sub(this.renderPosition.set(position));
    }

}
