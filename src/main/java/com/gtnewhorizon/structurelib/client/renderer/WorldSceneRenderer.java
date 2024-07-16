package com.gtnewhorizon.structurelib.client.renderer;

import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.ForgeHooksClient;

import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.util.glu.GLU;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.gtnewhorizon.structurelib.client.world.StructureWorld;
import com.gtnewhorizon.structurelib.util.PositionedRect;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: KilaBash, backported by Quarri6343
 * @Date: 2021/08/23
 * @Description: Abstract class, and extend a lot of features compared with the original one.
 */
public abstract class WorldSceneRenderer {

    // you have to place blocks in the world before use
    public final StructureWorld world;
    // the Blocks which this renderer needs to render
    protected final LongSet renderedBlocks = new LongOpenHashSet();
    private Consumer<WorldSceneRenderer> beforeRender;
    private Consumer<WorldSceneRenderer> onRender;
    private Consumer<MovingObjectPosition> onLookingAt;
    private int clearColor;
    private MovingObjectPosition lastTraceResult;
    private final Vector3f eyePos = new Vector3f(0, 0, -10f);
    private final Vector3f lookAt = new Vector3f(0, 0, 0);
    private final Vector3f worldUp = new Vector3f(0, 1, 0);
    private boolean renderAllFaces = false;

    public WorldSceneRenderer(StructureWorld world) {
        this.world = world;
    }

    public WorldSceneRenderer setBeforeWorldRender(Consumer<WorldSceneRenderer> callback) {
        this.beforeRender = callback;
        return this;
    }

    public WorldSceneRenderer setOnWorldRender(Consumer<WorldSceneRenderer> callback) {
        this.onRender = callback;
        return this;
    }

    public WorldSceneRenderer addRenderedBlocks(LongSet blocks) {
        if (blocks != null) this.renderedBlocks.addAll(blocks);
        return this;
    }

    public WorldSceneRenderer addRenderedBlock(long pos) {
        this.renderedBlocks.add(pos);
        return this;
    }

    public WorldSceneRenderer setOnLookingAt(Consumer<MovingObjectPosition> onLookingAt) {
        this.onLookingAt = onLookingAt;
        return this;
    }

    public void setRenderAllFaces(boolean renderAllFaces) {
        this.renderAllFaces = renderAllFaces;
    }

    public void setClearColor(int clearColor) {
        this.clearColor = clearColor;
    }

    public MovingObjectPosition getLastTraceResult() {
        return lastTraceResult;
    }

    /**
     * Renders scene on given coordinates with given width and height, and RGB background color Note that this will
     * ignore any transformations applied currently to projection/view matrix, so specified coordinates are scaled MC
     * gui coordinates. It will return matrices of projection and view in previous state after rendering
     */
    public void render(int x, int y, int width, int height, int mouseX, int mouseY) {

        PositionedRect positionedRect = getPositionedRect(x, y, width, height);
        PositionedRect mouse = getPositionedRect(mouseX, mouseY, 0, 0);
        mouseX = mouse.x;
        mouseY = mouse.y;
        // setupCamera
        setupCamera(positionedRect);

        // render TrackedDummyWorld
        drawWorld();

        // check lookingAt
        this.lastTraceResult = null;
        if (onLookingAt != null && mouseX > positionedRect.getLeft()
                && mouseX < positionedRect.getRight()
                && mouseY > positionedRect.getTop()
                && mouseY < positionedRect.getBottom()) {
            Vector3f lookVec = ProjectionUtils.unProject(positionedRect, eyePos, lookAt, mouseX, mouseY);
            MovingObjectPosition result = rayTrace(lookVec);
            if (result != null) {
                this.lastTraceResult = result;
                onLookingAt.accept(result);
            }
        }

        // resetcamera
        resetCamera();
    }

    public Vector3f getEyePos() {
        return eyePos;
    }

    public Vector3f getLookAt() {
        return lookAt;
    }

    public Vector3f getWorldUp() {
        return worldUp;
    }

    public void setCameraLookAt(Vector3f eyePos, Vector3f lookAt, Vector3f worldUp) {
        this.eyePos.set(eyePos);
        this.lookAt.set(lookAt);
        this.worldUp.set(worldUp);
    }

    public void setCameraLookAt(Vector3f lookAt, double radius, double rotationPitch, double rotationYaw) {
        this.lookAt.set(lookAt);
        Vector3d vecX = new Vector3d(Math.cos(rotationPitch), 0, Math.sin(rotationPitch));
        Vector3d vecY = new Vector3d(0, Math.tan(rotationYaw) * vecX.length(), 0);
        Vector3d pos = new Vector3d(vecX).add(vecY).normalize().mul(radius);
        this.eyePos.set(pos.add(lookAt.x, lookAt.y, lookAt.z));
    }

    public LongSet getRenderedBlocks() {
        return renderedBlocks;
    }

    public void clearRenderedBlocks() {
        renderedBlocks.clear();
    }

    protected PositionedRect getPositionedRect(int x, int y, int width, int height) {
        return new PositionedRect(x, y, width, height);
    }

    public void setupCamera(PositionedRect positionedRect) {
        final int x = positionedRect.x;
        final int y = positionedRect.y;
        final int width = positionedRect.getWidth();
        final int height = positionedRect.getHeight();
        final float aspectRatio = width / (height * 1.0f);

        Minecraft mc = Minecraft.getMinecraft();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushClientAttrib(GL11.GL_ALL_CLIENT_ATTRIB_BITS);
        mc.entityRenderer.disableLightmap(0);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);

        // setup viewport and clear GL buffers
        GL11.glViewport(x, y, width, height);

        clearView(x, y, width, height);

        // setup projection matrix to perspective
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GLU.gluPerspective(60.0f, aspectRatio, 0.1f, 10000.0f);

        // setup modelview matrix
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GLU.gluLookAt(eyePos.x, eyePos.y, eyePos.z, lookAt.x, lookAt.y, lookAt.z, worldUp.x, worldUp.y, worldUp.z);
    }

    public static void setGlClearColorFromInt(int colorValue, int opacity) {
        int i = (colorValue & 16711680) >> 16;
        int j = (colorValue & 65280) >> 8;
        int k = (colorValue & 255);
        GL11.glClearColor(i / 255.0f, j / 255.0f, k / 255.0f, opacity / 255.0f);
    }

    protected void clearView(int x, int y, int width, int height) {
        setGlClearColorFromInt(clearColor, clearColor >> 24);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    public static void resetCamera() {
        // reset viewport
        Minecraft minecraft = Minecraft.getMinecraft();
        GL11.glViewport(0, 0, minecraft.displayWidth, minecraft.displayHeight);

        // reset modelview matrix
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();

        // reset projection matrix
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();

        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        // reset attributes
        GL11.glPopClientAttrib();
        GL11.glPopAttrib();
    }

    protected void drawWorld() {
        final Vector3i pos = new Vector3i();

        if (beforeRender != null) {
            beforeRender.accept(this);
        }

        Minecraft mc = Minecraft.getMinecraft();
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        mc.entityRenderer.disableLightmap(0);
        mc.renderEngine.bindTexture(TextureMap.locationBlocksTexture);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_ALPHA_TEST);

        final int savedAo = mc.gameSettings.ambientOcclusion;
        mc.gameSettings.ambientOcclusion = 0;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        try {
            tessellator.setBrightness(15 << 20 | 15 << 4);
            RenderBlocks renderBlocks = new RenderBlocks(world);
            for (final long longPos : renderedBlocks) {
                CoordinatePacker.unpack(longPos, pos);
                Block block = world.getBlock(pos.x, pos.y, pos.z);
                if (block.equals(Blocks.air)) continue;

                renderBlocks.setRenderBounds(0, 0, 0, 1, 1, 1);
                renderBlocks.renderAllFaces = renderAllFaces;
                renderBlocks.renderBlockByRenderType(block, pos.x, pos.y, pos.z);
            }
            if (onRender != null) {
                onRender.accept(this);
            }
        } finally {
            mc.gameSettings.ambientOcclusion = savedAo;
            tessellator.draw();
            tessellator.setTranslation(0, 0, 0);
        }

        RenderHelper.enableStandardItemLighting();
        GL11.glEnable(GL11.GL_LIGHTING);

        // render TESR
        TileEntityRendererDispatcher tesr = TileEntityRendererDispatcher.instance;
        for (int pass = 0; pass < 2; pass++) {
            ForgeHooksClient.setRenderPass(pass);
            int finalPass = pass;
            renderedBlocks.forEach(longPos -> {
                CoordinatePacker.unpack(longPos, pos);
                setDefaultPassRenderState(finalPass);
                TileEntity tile = world.getTileEntity(pos.x, pos.y, pos.z);
                if (tile != null && tesr.hasSpecialRenderer(tile)) {
                    if (tile.shouldRenderInPass(finalPass)) {
                        tesr.renderTileEntityAt(tile, pos.x, pos.y, pos.z, 0);
                    }
                }
            });
        }
        ForgeHooksClient.setRenderPass(-1);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
    }

    public static void setDefaultPassRenderState(int pass) {
        GL11.glColor4f(1, 1, 1, 1);
        if (pass == 0) { // SOLID
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDepthMask(true);
        } else { // TRANSLUCENT
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDepthMask(false);
        }
    }

    private final MutVec3 startPos = new MutVec3(0, 0, 0);
    private final MutVec3 endPos = new MutVec3(0, 0, 0);

    public MovingObjectPosition rayTrace(Vector3f lookVec) {
        startPos.set(eyePos);
        // range: 100 Blocks
        endPos.set(lookVec.mul(100)).add(eyePos);
        return this.world.rayTraceBlockswithTargetMap(startPos, endPos, renderedBlocks);
    }
}
