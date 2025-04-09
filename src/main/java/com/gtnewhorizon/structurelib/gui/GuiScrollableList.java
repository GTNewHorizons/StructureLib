package com.gtnewhorizon.structurelib.gui;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/**
 * Shamelessly stolen from EnderCore, with some amount of edit to fit in one file. Original code from CrazyPants.
 * <p>
 * As the code has been released into public domain, it is now LGPL as part of this library as a whole. You can go to
 * EnderCore's repository to obtain a public domain copy though
 * <p>
 * Major edits
 * <ul>
 * <li>Separate dwheel handling to handleDWheel. The next event calls often cause the mouse click event to not fire on
 * other widgets</li>
 * <li>scrollbar are now rendered within width</li>
 * <li>added prepareDrawElements</li>
 * </ul>
 *
 * @author CrazyPants
 * @author glee8e
 */
public abstract class GuiScrollableList<T> {

    protected final int slotHeight;
    private final Minecraft mc = Minecraft.getMinecraft();
    protected final int originX;
    protected final int originY;
    protected final int width;
    protected final int height;
    protected int minY;
    protected int maxY;
    protected int minX;
    protected int maxX;
    protected int mouseX;
    protected int mouseY;
    protected int selectedIndex = -1;
    protected int margin = 4;
    protected final List<ListSelectionListener<T>> listeners = new CopyOnWriteArrayList<>();
    private int scrollUpButtonID;
    private int scrollDownButtonID;
    private float initialClickY = -2.0F;
    private float scrollMultiplier;
    private float amountScrolled;
    private long lastClickedTime;

    private boolean showSelectionBox = true;

    public GuiScrollableList(int width, int height, int originX, int originY, int slotHeight) {
        this.width = width;
        this.height = height;
        this.originX = originX;
        this.originY = originY;
        this.slotHeight = slotHeight;

        minY = originY;
        maxY = minY + height;
        minX = originX;
        maxX = minX + width;
    }

    public void onGuiInit(IGuiScreen gui) {
        minY = originY + gui.getGuiTop();
        maxY = minY + height;
        minX = originX + gui.getGuiLeft();
        maxX = minX + width;
    }

    public void addSelectionListener(ListSelectionListener<T> listener) {
        listeners.add(listener);
    }

    public void removeSelectionListener(ListSelectionListener<T> listener) {
        listeners.remove(listener);
    }

    public T getSelectedElement() {
        return getElementAt(selectedIndex);
    }

    public void setSelection(T selection) {
        setSelection(getIndexOf(selection));
    }

    public void setSelection(int index) {
        if (index == selectedIndex) {
            return;
        }
        selectedIndex = index;
        for (ListSelectionListener<T> listener : listeners) {
            listener.selectionChanged(this, selectedIndex);
        }
    }

    public int getIndexOf(T element) {
        if (element == null) {
            return -1;
        }
        for (int i = 0; i < getNumElements(); i++) {
            if (element.equals(getElementAt(i))) {
                return i;
            }
        }
        return -1;
    }

    public abstract T getElementAt(int index);

    public abstract int getNumElements();

    protected abstract void drawElement(int elementIndex, int x, int y, int height, Tessellator tessellator,
            boolean isHovering);

    protected boolean elementClicked(int elementIndex, boolean doubleClick, int mXRelative, int mYRelative) {
        return true;
    }

    public void setShowSelectionBox(boolean val) {
        showSelectionBox = val;
    }

    protected int getContentHeight() {
        return getNumElements() * slotHeight;
    }

    public void setScrollButtonIds(int scrollUpButtonID, int scrollDownButtonID) {
        this.scrollUpButtonID = scrollUpButtonID;
        this.scrollDownButtonID = scrollDownButtonID;
    }

    private void clampScrollToBounds() {
        int i = getContentOverhang();
        if (i < 0) {
            i *= -1;
        }
        if (amountScrolled < 0.0F) {
            amountScrolled = 0.0F;
        }
        if (amountScrolled > i) {
            amountScrolled = i;
        }
    }

    public int getContentOverhang() {
        return getContentHeight() - (height - margin);
    }

    public void actionPerformed(GuiButton b) {
        if (b.enabled) {
            if (b.id == scrollUpButtonID) {
                amountScrolled -= slotHeight * 2f / 3;
                initialClickY = -2.0F;
                clampScrollToBounds();
            } else if (b.id == scrollDownButtonID) {
                amountScrolled += slotHeight * 2f / 3;
                initialClickY = -2.0F;
                clampScrollToBounds();
            }
        }
    }

    protected void resetScroll() {
        amountScrolled = 0;
    }

    /**
     * draws the slot to the screen, pass in mouse's current x and y and partial ticks
     */
    public void drawScreen(int mX, int mY, float partialTick) {
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glTranslatef(0.0F, 0.0F, -2000.0F);
        this.mouseX = mX;
        this.mouseY = mY;

        processMouseEvents();

        if (getContentOverhang() > 0) maxX = minX + width - 6;
        else maxX = minX + width;

        clampScrollToBounds();

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);

        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int sx = minX * sr.getScaleFactor();
        int sw = width * sr.getScaleFactor();
        int sy = mc.displayHeight - (maxY * sr.getScaleFactor());
        int sh = height * sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sx, sy, sw, sh);

        Tessellator tessellator = Tessellator.instance;
        drawContainerBackground(tessellator);

        int amountScrolled = (int) this.amountScrolled;
        int contentYOffset = this.minY + margin - amountScrolled;

        boolean hoveringPrecondition = mX >= minX && mX <= maxX && mY >= minY && mY <= maxY;
        int slotHeight = this.slotHeight - margin;

        if (selectedIndex != -1) {
            int elementY = contentYOffset + selectedIndex * this.slotHeight;
            if (elementY + slotHeight > maxY) {
                this.amountScrolled = (selectedIndex + 1) * this.slotHeight - this.height + margin;
                amountScrolled = (int) this.amountScrolled;
                contentYOffset = this.minY + margin - amountScrolled;
            } else if (elementY < minY) {
                this.amountScrolled = selectedIndex * this.slotHeight;
                amountScrolled = (int) this.amountScrolled;
                contentYOffset = this.minY + margin - amountScrolled;
            }
        }

        prepareDrawElements();

        for (int i = 0; i < getNumElements(); ++i) {

            int elementY = contentYOffset + i * this.slotHeight;

            if (elementY <= maxY && elementY + slotHeight >= minY) {

                if (showSelectionBox && i == selectedIndex) {
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    GL11.glDisable(GL11.GL_TEXTURE_2D);
                    tessellator.startDrawingQuads();
                    tessellator.setColorOpaque_I(8421504);
                    tessellator.addVertexWithUV(minX, elementY + slotHeight + 2, 0.0D, 0.0D, 1.0D);
                    tessellator.addVertexWithUV(maxX, elementY + slotHeight + 2, 0.0D, 1.0D, 1.0D);
                    tessellator.addVertexWithUV(maxX, elementY - 2, 0.0D, 1.0D, 0.0D);
                    tessellator.addVertexWithUV(minX, elementY - 2, 0.0D, 0.0D, 0.0D);
                    tessellator.setColorOpaque_I(0);
                    tessellator.addVertexWithUV(minX + 1, elementY + slotHeight + 1, 0.0D, 0.0D, 1.0D);
                    tessellator.addVertexWithUV(maxX - 1, elementY + slotHeight + 1, 0.0D, 1.0D, 1.0D);
                    tessellator.addVertexWithUV(maxX - 1, elementY - 1, 0.0D, 1.0D, 0.0D);
                    tessellator.addVertexWithUV(minX + 1, elementY - 1, 0.0D, 0.0D, 0.0D);
                    tessellator.draw();
                    GL11.glEnable(GL11.GL_TEXTURE_2D);
                }

                boolean isHovering = hoveringPrecondition && elementY - 2 <= mY && mY < elementY + slotHeight + 2;
                drawElement(i, minX, elementY, slotHeight, tessellator, isHovering);
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        tessellator.startDrawingQuads();
        tessellator.setColorRGBA_I(0, 0);
        tessellator.addVertexWithUV(this.minX, this.minY + margin, 0.0D, 0.0D, 1.0D);
        tessellator.addVertexWithUV(this.maxX, this.minY + margin, 0.0D, 1.0D, 1.0D);
        tessellator.setColorRGBA_I(0, 255);
        tessellator.addVertexWithUV(this.maxX, this.minY, 0.0D, 1.0D, 0.0D);
        tessellator.addVertexWithUV(this.minX, this.minY, 0.0D, 0.0D, 0.0D);
        tessellator.draw();
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA_I(0, 255);
        tessellator.addVertexWithUV(this.minX, this.maxY, 0.0D, 0.0D, 1.0D);
        tessellator.addVertexWithUV(this.maxX, this.maxY, 0.0D, 1.0D, 1.0D);
        tessellator.setColorRGBA_I(0, 0);
        tessellator.addVertexWithUV(this.maxX, this.maxY - margin, 0.0D, 1.0D, 0.0D);
        tessellator.addVertexWithUV(this.minX, this.maxY - margin, 0.0D, 0.0D, 0.0D);
        tessellator.draw();

        renderScrollBar(tessellator);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    protected void prepareDrawElements() {}

    protected void renderScrollBar(Tessellator tessellator) {

        int contentHeightOverBounds = getContentOverhang();
        if (contentHeightOverBounds > 0) {

            int clear = (maxY - minY) * (maxY - minY) / getContentHeight();

            if (clear < 32) {
                clear = 32;
            }

            if (clear > maxY - minY - 8) {
                clear = maxY - minY - 8;
            }

            int y = (int) this.amountScrolled * (maxY - minY - clear) / contentHeightOverBounds + minY;
            if (y < minY) {
                y = minY;
            }

            GL11.glDisable(GL11.GL_TEXTURE_2D);
            int scrollBarMinX = getScrollBarX();
            int scrollBarMaxX = scrollBarMinX + 6;
            tessellator.startDrawingQuads();

            tessellator.setColorRGBA_I(0, 255);
            tessellator.addVertexWithUV(scrollBarMinX, maxY, 0.0D, 0.0D, 1.0D);
            tessellator.addVertexWithUV(scrollBarMaxX, maxY, 0.0D, 1.0D, 1.0D);
            tessellator.addVertexWithUV(scrollBarMaxX, minY, 0.0D, 1.0D, 0.0D);
            tessellator.addVertexWithUV(scrollBarMinX, minY, 0.0D, 0.0D, 0.0D);

            tessellator.setColorRGBA_F(0.3f, 0.3f, 0.3f, 1);
            tessellator.addVertexWithUV(scrollBarMinX, (y + clear), 0.0D, 0.0D, 1.0D);
            tessellator.addVertexWithUV(scrollBarMaxX, (y + clear), 0.0D, 1.0D, 1.0D);
            tessellator.addVertexWithUV(scrollBarMaxX, y, 0.0D, 1.0D, 0.0D);
            tessellator.addVertexWithUV(scrollBarMinX, y, 0.0D, 0.0D, 0.0D);

            tessellator.setColorRGBA_F(0.7f, 0.7f, 0.7f, 1);
            tessellator.addVertexWithUV(scrollBarMinX, (y + clear - 1), 0.0D, 0.0D, 1.0D);
            tessellator.addVertexWithUV((scrollBarMaxX - 1), (y + clear - 1), 0.0D, 1.0D, 1.0D);
            tessellator.addVertexWithUV((scrollBarMaxX - 1), y, 0.0D, 1.0D, 0.0D);
            tessellator.addVertexWithUV(scrollBarMinX, y, 0.0D, 0.0D, 0.0D);

            tessellator.draw();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
    }

    private void processMouseEvents() {
        if (Mouse.isButtonDown(0)) {
            processMouseBown();
        } else {
            initialClickY = -1.0F;
        }
    }

    public void handleDWheel(int mouseWheelDelta) {
        if (!Mouse.isButtonDown(0)) {
            if (mouseWheelDelta > 0) {
                amountScrolled -= slotHeight / 2f;
            } else /* if (mouseWheelDelta < 0) */ {
                amountScrolled += slotHeight / 2f;
            }
        }
    }

    private void processMouseBown() {
        int contentHeightOverBounds;
        if (initialClickY == -1.0F) {

            if (mouseY >= minY && mouseY <= maxY && mouseX >= minX && mouseX <= maxX + 6) {

                boolean clickInBounds = true;

                int y = mouseY - minY + (int) amountScrolled - margin;
                int mouseOverElement = y / slotHeight;

                if (mouseX >= minX && mouseX <= maxX
                        && mouseOverElement >= 0
                        && y >= 0
                        && mouseOverElement < getNumElements()) {
                    boolean doubleClick = mouseOverElement == selectedIndex
                            && Minecraft.getSystemTime() - lastClickedTime < 250L;
                    if (elementClicked(
                            mouseOverElement,
                            doubleClick,
                            mouseX - minX,
                            y - slotHeight * mouseOverElement)) {
                        setSelection(mouseOverElement);
                        lastClickedTime = Minecraft.getSystemTime();
                    }
                } else if (mouseX >= minX && mouseX <= maxX && y < 0) {
                    clickInBounds = false;
                }

                int scrollBarMinX = getScrollBarX();
                int scrollBarMaxX = scrollBarMinX + 6;
                if (mouseX >= scrollBarMinX && mouseX <= scrollBarMaxX) {

                    scrollMultiplier = -1.0F;
                    contentHeightOverBounds = getContentOverhang();

                    if (contentHeightOverBounds < 1) {
                        contentHeightOverBounds = 1;
                    }

                    int empty = (int) ((float) ((maxY - minY) * (maxY - minY)) / (float) getContentHeight());
                    if (empty < 32) {
                        empty = 32;
                    }
                    if (empty > maxY - minY - 8) {
                        empty = maxY - minY - 8;
                    }
                    scrollMultiplier /= (float) (maxY - minY - empty) / (float) contentHeightOverBounds;

                } else {
                    scrollMultiplier = 1.0F;
                }

                if (clickInBounds) {
                    initialClickY = mouseY;
                } else {
                    initialClickY = -2.0F;
                }

            } else {
                initialClickY = -2.0F;
            }

        } else if (initialClickY >= 0.0F) {
            // Scrolling
            amountScrolled -= (mouseY - initialClickY) * scrollMultiplier;
            initialClickY = mouseY;
        }
    }

    protected int getScrollBarX() {
        return maxX;
    }

    protected void drawContainerBackground(Tessellator tess) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        tess.startDrawingQuads();
        tess.setColorOpaque_I(0x202020);
        tess.addVertex(minX, maxY, 0.0D);
        tess.addVertex(maxX, maxY, 0.0D);
        tess.addVertex(maxX, minY, 0.0D);
        tess.addVertex(minX, minY, 0.0D);
        tess.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public interface IGuiScreen {

        int getGuiLeft();

        int getGuiTop();

        int getXSize();

        int getYSize();

        void addButton(GuiButton button);

        void removeButton(GuiButton button);

        int getOverlayOffsetX();

        void doActionPerformed(GuiButton but);
    }

    public interface ListSelectionListener<T> {

        void selectionChanged(GuiScrollableList<T> list, int selectedIndex);
    }
}
