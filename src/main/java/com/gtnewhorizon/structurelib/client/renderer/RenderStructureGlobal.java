package com.gtnewhorizon.structurelib.client.renderer;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.profiler.Profiler;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Vector3d;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.structurelib.client.world.StructureWorld;

public class RenderStructureGlobal {

    private final Vector3d playerPosition = new Vector3d();
    private ForgeDirection orientation = ForgeDirection.UNKNOWN;
    private int rotationRender = 0;

    private final Frustrum frustrum = new Frustrum();
    public RenderBlocks renderBlocks = null;
    public final List<RendererStructureChunk> sortedRendererStructureChunk = new ArrayList<>();
    private final RendererStructureChunkComparator rendererStructureChunkComparator = new RendererStructureChunkComparator();

    public void setPlayerPosition(Vector3d playerPosistion) {
        this.playerPosition.set(playerPosistion);
    }

    public void setPlayerPosition(double x, double y, double z) {
        this.playerPosition.set(x, y, z);
    }

    public void setOrientation(ForgeDirection orientation) {
        this.orientation = orientation;
    }

    public void setRotationRender(int rotationRender) {
        this.rotationRender = rotationRender;
    }

    public void render(StructureWorld structureWorld) {
        final Minecraft minecraft = Minecraft.getMinecraft();
        final Profiler profiler = minecraft.mcProfiler;

        GL11.glPushMatrix();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_BLEND);

        Vector3d extra = new Vector3d();
        if (structureWorld != null) {
            extra.add(
                    structureWorld.renderPosition.x,
                    structureWorld.renderPosition.y,
                    structureWorld.renderPosition.z);
            playerPosition.sub(extra);
        }

        GL11.glTranslated(-playerPosition.x, -playerPosition.y, -playerPosition.z);

        profiler.startSection("schematic");
        if (structureWorld != null && structureWorld.isRendering()) {
            profiler.startSection("updateFrustrum");
            updateFrustrum(structureWorld);

            profiler.endStartSection("sortAndUpdate");
            if (RendererStructureChunk.getCanUpdate()) {
                sortAndUpdate(structureWorld);
            }

            profiler.endStartSection("render");
            int pass;
            for (pass = 0; pass < 3; pass++) {
                for (RendererStructureChunk renderer : this.sortedRendererStructureChunk) {
                    renderer.render(playerPosition, pass);
                }
            }
            profiler.endSection();
        }

        profiler.endStartSection("guide");

        RenderHelper.createBuffers();

        profiler.startSection("dataPrep");

        int quadCount = RenderHelper.getQuadCount();
        int lineCount = RenderHelper.getLineCount();

        if (quadCount > 0 || lineCount > 0) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            GL11.glLineWidth(1.5f);

            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);

            profiler.endStartSection("quad");
            if (quadCount > 0) {
                GL11.glVertexPointer(3, 0, RenderHelper.getQuadVertexBuffer());
                GL11.glColorPointer(4, 0, RenderHelper.getQuadColorBuffer());
                GL11.glDrawArrays(GL11.GL_QUADS, 0, quadCount);
            }

            profiler.endStartSection("line");
            if (lineCount > 0) {
                GL11.glVertexPointer(3, 0, RenderHelper.getLineVertexBuffer());
                GL11.glColorPointer(4, 0, RenderHelper.getLineColorBuffer());
                GL11.glDrawArrays(GL11.GL_LINES, 0, lineCount);
            }

            profiler.endSection();

            GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
            GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);

            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }

        profiler.endSection();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glPopMatrix();
    }

    private void updateFrustrum(StructureWorld schematic) {
        this.frustrum.setPosition(
                playerPosition.x - schematic.renderPosition.x,
                playerPosition.y - schematic.renderPosition.y,
                playerPosition.z - schematic.renderPosition.z);
        for (RendererStructureChunk RendererStructureChunk : this.sortedRendererStructureChunk) {
            RendererStructureChunk.isInFrustrum = this.frustrum
                    .isBoundingBoxInFrustum(RendererStructureChunk.getBoundingBox());
        }
    }

    private void sortAndUpdate(StructureWorld world) {
        this.rendererStructureChunkComparator.setPosition(playerPosition, world.renderPosition);
        this.sortedRendererStructureChunk.sort(this.rendererStructureChunkComparator);

        for (RendererStructureChunk RendererStructureChunk : this.sortedRendererStructureChunk) {
            if (RendererStructureChunk.getDirty()) {
                RendererStructureChunk.updateRenderer(renderBlocks);
                break;
            }
        }
    }

    public void createRendererStructureChunks(StructureWorld structureWorld) {
        int width = (structureWorld.getWidth() - 1) / 16 + 1;
        int height = (structureWorld.getHeight() - 1) / 16 + 1;
        int length = (structureWorld.getLength() - 1) / 16 + 1;

        destroyRendererStructureChunks();

        this.renderBlocks = new RenderBlocks(structureWorld);
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    this.sortedRendererStructureChunk.add(new RendererStructureChunk(structureWorld, x, y, z));
                }
            }
        }
    }

    public void destroyRendererStructureChunks() {
        this.renderBlocks = null;
        while (!this.sortedRendererStructureChunk.isEmpty()) {
            this.sortedRendererStructureChunk.remove(0).delete();
        }
    }

    public void refresh() {
        for (RendererStructureChunk renderer : this.sortedRendererStructureChunk) {
            renderer.setDirty();
        }
    }

}
