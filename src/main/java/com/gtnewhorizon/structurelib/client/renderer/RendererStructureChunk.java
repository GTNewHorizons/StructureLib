package com.gtnewhorizon.structurelib.client.renderer;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;

import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.client.world.StructureWorld;

public class RendererStructureChunk {

    private static final ShaderProgram alphaShader = initAlphaShader();
    private static final int alphaUniform = GL20.glGetUniformLocation(alphaShader.getProgram(), "alpha_multiplier");

    private static boolean canUpdate = false;

    public boolean isInFrustrum = false;

    public final Vector3d centerPosition = new Vector3d();

    private final Minecraft minecraft = Minecraft.getMinecraft();
    private final Profiler profiler = this.minecraft.mcProfiler;
    private final StructureWorld world;
    private final List<TileEntity> tileEntities = new ArrayList<>();
    private final Vector3d distance = new Vector3d();

    private final AxisAlignedBB boundingBox = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);

    private boolean needsUpdate = true;
    private int glList = -1;
    // TODO: move this away from GL lists
    private int glListHighlight = -1;

    private static ShaderProgram initAlphaShader() {
        ShaderProgram shader;
        try {
            shader = new ShaderProgram("structurelib", null, "shaders/alpha.frag");
            shader.use();
            GL20.glUniform1i(GL20.glGetUniformLocation(shader.getProgram(), "texture"), 0);
            ShaderProgram.clear();
        } catch (Exception e) {
            shader = null;
        }
        return shader;
    }

    private static final int SUBCHUNK_SIZE = 16;

    public RendererStructureChunk(StructureWorld world, int x, int y, int z) {
        this.world = world;

        this.boundingBox.setBounds(
                x * SUBCHUNK_SIZE,
                y * SUBCHUNK_SIZE,
                z * SUBCHUNK_SIZE,
                (x + 1) * SUBCHUNK_SIZE,
                (y + 1) * SUBCHUNK_SIZE,
                (z + 1) * SUBCHUNK_SIZE);
        this.centerPosition.set(
                (int) ((x + 0.5) * SUBCHUNK_SIZE),
                (int) ((y + 0.5) * SUBCHUNK_SIZE),
                (int) ((z + 0.5) * SUBCHUNK_SIZE));

        // TODO: Replace this with something less stupid
        int tx, ty, tz;
        for (TileEntity tileEntity : this.world.getTileEntities()) {
            tx = tileEntity.xCoord;
            ty = tileEntity.yCoord;
            tz = tileEntity.zCoord;

            if (tx < this.boundingBox.minX || tx >= this.boundingBox.maxX) {
                continue;
            } else if (tz < this.boundingBox.minZ || tz >= this.boundingBox.maxZ) {
                continue;
            } else if (ty < this.boundingBox.minY || ty >= this.boundingBox.maxY) {
                continue;
            }

            this.tileEntities.add(tileEntity);
        }

    }

    public void delete() {
        // Deallocate VBOs
    }

    public void updateRenderer(RenderBlocks renderBlocks) {
        if (this.needsUpdate) {
            this.needsUpdate = false;
            setCanUpdate(false);

            RenderHelper.createBuffers();

            for (int pass = 0; pass < 3; pass++) {
                RenderHelper.initBuffers();

                int minX, maxX, minY, maxY, minZ, maxZ;

                minX = (int) this.boundingBox.minX;
                maxX = (int) this.boundingBox.maxX;
                minY = (int) this.boundingBox.minY;
                maxY = (int) this.boundingBox.maxY;
                minZ = (int) this.boundingBox.minZ;
                maxZ = (int) this.boundingBox.maxZ;

                // TODO: Rendering layer
                // int renderingLayer = this.world.renderingLayer;
                // if (this.world.isRenderingLayer) {
                // if (renderingLayer >= minY && renderingLayer < maxY) {
                // minY = renderingLayer;
                // maxY = renderingLayer + 1;
                // } else {
                // minY = maxY = 0;
                // }
                // }

                GL11.glNewList(this.glList + pass, GL11.GL_COMPILE);
                renderBlocks(renderBlocks, pass, minX, minY, minZ, maxX, maxY, maxZ);
                GL11.glEndList();

                GL11.glNewList(this.glListHighlight + pass, GL11.GL_COMPILE);
                int quadCount = RenderHelper.getQuadCount();
                int lineCount = RenderHelper.getLineCount();

                if (quadCount > 0 || lineCount > 0) {
                    GL11.glDisable(GL11.GL_TEXTURE_2D);

                    GL11.glLineWidth(1.5f);

                    GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                    GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);

                    if (quadCount > 0) {
                        GL11.glVertexPointer(3, 0, RenderHelper.getQuadVertexBuffer());
                        GL11.glColorPointer(4, 0, RenderHelper.getQuadColorBuffer());
                        GL11.glDrawArrays(GL11.GL_QUADS, 0, quadCount);
                    }

                    if (lineCount > 0) {
                        GL11.glVertexPointer(3, 0, RenderHelper.getLineVertexBuffer());
                        GL11.glColorPointer(4, 0, RenderHelper.getLineColorBuffer());
                        GL11.glDrawArrays(GL11.GL_LINES, 0, lineCount);
                    }

                    GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
                    GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);

                    GL11.glEnable(GL11.GL_TEXTURE_2D);
                }

                GL11.glEndList();
            }

            RenderHelper.destroyBuffers();
        }
    }

    public void render(Vector3d playerPosition, int renderPass) {
        if (!this.isInFrustrum) {
            return;
        }

        if (this.distance.set(playerPosition)
                .sub(this.world.renderPosition.x, this.world.renderPosition.y, this.world.renderPosition.z)
                .sub(this.centerPosition).lengthSquared() > 25600) {
            return;
        }

        // some mods enable this, beats me why - it's supposed to be disabled!
        GL11.glDisable(GL11.GL_LIGHTING);

        this.profiler.startSection("blocks");
        this.minecraft.renderEngine.bindTexture(TextureMap.locationBlocksTexture);

        GL20.glUseProgram(alphaShader.getProgram());
        GL20.glUniform1f(alphaUniform, ALPHA);

        GL11.glCallList(this.glList + renderPass);

        GL20.glUseProgram(0);

        this.profiler.endStartSection("highlight");
        GL11.glCallList(this.glListHighlight + renderPass);

        this.profiler.endStartSection("tileEntities");
        renderTileEntities(renderPass);

        // re-enable blending... spawners disable it, somewhere...
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // re-set alpha func... beacons set it to (GL_GREATER, 0.5f)
        // EntityRenderer sets it to (GL_GREATER, 0.1f) before dispatching the event
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);

        this.profiler.endSection();
    }

    public void renderBlocks(RenderBlocks renderBlocks, int renderPass, int minX, int minY, int minZ, int maxX,
            int maxY, int maxZ) {
        IBlockAccess mcWorld = this.minecraft.theWorld;

        int x, y, z, wx, wy, wz;
        int sides;
        Block block, mcBlock;
        Vector3f zero = new Vector3f();
        Vector3f size = new Vector3f();

        int ambientOcclusion = this.minecraft.gameSettings.ambientOcclusion;
        this.minecraft.gameSettings.ambientOcclusion = 0;

        Tessellator.instance.startDrawingQuads();

        for (y = minY; y < maxY; y++) {
            for (z = minZ; z < maxZ; z++) {
                for (x = minX; x < maxX; x++) {
                    try {
                        block = this.world.getBlock(x, y, z);

                        wx = this.world.renderPosition.x + x;
                        wy = this.world.renderPosition.y + y;
                        wz = this.world.renderPosition.z + z;

                        mcBlock = mcWorld.getBlock(wx, wy, wz);

                        sides = 0;
                        if (block != null) {
                            if (block.shouldSideBeRendered(this.world, x, y - 1, z, 0)) {
                                sides |= RenderHelper.QUAD_DOWN;
                            }

                            if (block.shouldSideBeRendered(this.world, x, y + 1, z, 1)) {
                                sides |= RenderHelper.QUAD_UP;
                            }

                            if (block.shouldSideBeRendered(this.world, x, y, z - 1, 2)) {
                                sides |= RenderHelper.QUAD_NORTH;
                            }

                            if (block.shouldSideBeRendered(this.world, x, y, z + 1, 3)) {
                                sides |= RenderHelper.QUAD_SOUTH;
                            }

                            if (block.shouldSideBeRendered(this.world, x - 1, y, z, 4)) {
                                sides |= RenderHelper.QUAD_WEST;
                            }

                            if (block.shouldSideBeRendered(this.world, x + 1, y, z, 5)) {
                                sides |= RenderHelper.QUAD_EAST;
                            }
                        }

                        boolean isAirBlock = this.world.isAirBlock(x, y, z);
                        boolean isMcAirBlock = mcWorld.isAirBlock(wx, wy, wz);

                        if (!isMcAirBlock) {
                            // if ConfigurationHandler.highlight
                            if (renderPass == 2) {
                                // if ConfigurationHandler.highlightAir
                                if (isAirBlock) {
                                    zero.set(x, y, z);
                                    size.set(x + 1, y + 1, z + 1);
                                    // if (ConfigurationHandler.drawQuads)
                                    RenderHelper.drawCuboidSurface(
                                            zero,
                                            size,
                                            RenderHelper.QUAD_ALL,
                                            0.75f,
                                            0.0f,
                                            0.75f,
                                            0.25f);
                                    // if (ConfigurationHandler.drawLines)
                                    RenderHelper.drawCuboidOutline(
                                            zero,
                                            size,
                                            RenderHelper.LINE_ALL,
                                            0.75f,
                                            0.0f,
                                            0.75f,
                                            0.25f);
                                } else if (block != mcBlock) {
                                    zero.set(x, y, z);
                                    size.set(x + 1, y + 1, z + 1);
                                    // if (ConfigurationHandler.drawQuads)
                                    RenderHelper.drawCuboidSurface(zero, size, sides, 1.0f, 0.0f, 0.0f, 0.25f);
                                    // if (ConfigurationHandler.drawLines)
                                    RenderHelper.drawCuboidOutline(zero, size, sides, 1.0f, 0.0f, 0.0f, 0.25f);
                                } else
                                    if (this.world.getBlockMetadata(x, y, z) != mcWorld.getBlockMetadata(wx, wy, wz)) {
                                        zero.set(x, y, z);
                                        size.set(x + 1, y + 1, z + 1);
                                        // if (ConfigurationHandler.drawQuads)
                                        RenderHelper.drawCuboidSurface(zero, size, sides, 0.75f, 0.35f, 0.0f, 0.25f);
                                        // if (ConfigurationHandler.drawLines)
                                        RenderHelper.drawCuboidOutline(zero, size, sides, 0.75f, 0.35f, 0.0f, 0.25f);
                                    }
                            }
                        } else if (!isAirBlock) {
                            // if ConfigurationHandler.highlight
                            if (renderPass == 2) {
                                zero.set(x, y, z);
                                size.set(x + 1, y + 1, z + 1);
                                // if (ConfigurationHandler.drawQuads)
                                RenderHelper.drawCuboidSurface(zero, size, sides, 0.0f, 0.75f, 1.0f, 0.25f);
                                // if (ConfigurationHandler.drawLines)
                                RenderHelper.drawCuboidOutline(zero, size, sides, 0.0f, 0.75f, 1.0f, 0.25f);
                            }

                            if (block != null && block.canRenderInPass(renderPass)) {
                                renderBlocks.renderBlockByRenderType(block, x, y, z);
                            }
                        }
                    } catch (Exception e) {
                        StructureLib.LOGGER.error("Failed to render block!", e);
                    }
                }
            }
        }

        Tessellator.instance.draw();

        this.minecraft.gameSettings.ambientOcclusion = ambientOcclusion;
    }

    public static float ALPHA = 0.75f;

    public void renderTileEntities(int renderPass) {
        if (renderPass != 0) {
            return;
        }

        IBlockAccess mcWorld = this.minecraft.theWorld;

        int x, y, z;

        GL11.glColor4f(1.0f, 1.0f, 1.0f, ALPHA);

        try {
            for (TileEntity tileEntity : this.tileEntities) {
                x = tileEntity.xCoord;
                y = tileEntity.yCoord;
                z = tileEntity.zCoord;

                // TODO: Rendering layer
                // if (this.world.isRenderingLayer && this.world.renderingLayer != y) {
                // continue;
                // }

                final boolean isAirBlock = mcWorld.isAirBlock(
                        x + this.world.renderPosition.x,
                        y + this.world.renderPosition.y,
                        z + this.world.renderPosition.z);

                if (isAirBlock) {
                    TileEntitySpecialRenderer tileEntitySpecialRenderer = TileEntityRendererDispatcher.instance
                            .getSpecialRenderer(tileEntity);
                    if (tileEntitySpecialRenderer != null) {
                        try {
                            tileEntitySpecialRenderer.renderTileEntityAt(tileEntity, x, y, z, 0);

                            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
                            GL11.glDisable(GL11.GL_TEXTURE_2D);
                            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
                        } catch (Exception e) {
                            StructureLib.LOGGER.error("Failed to render a tile entity!", e);
                        }
                        GL11.glColor4f(1.0f, 1.0f, 1.0f, ALPHA);
                    }
                }
            }
        } catch (Exception ex) {
            StructureLib.LOGGER.error("Failed to render tile entities!", ex);
        }
    }

    public static void setCanUpdate(boolean parCanUpdate) {
        canUpdate = parCanUpdate;
    }

    public static boolean getCanUpdate() {
        return canUpdate;
    }

    public void setDirty() {
        this.needsUpdate = true;
    }

    public boolean getDirty() {
        return this.needsUpdate;
    }

    public AxisAlignedBB getBoundingBox() {
        return boundingBox;
    }
}
