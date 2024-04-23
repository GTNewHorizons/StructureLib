/*
 * Forge Mod Loader Copyright (c) 2012-2014 cpw. All rights reserved. This program and the accompanying materials are
 * made available under the terms of the GNU Lesser Public License v2.1 which accompanies this distribution, and is
 * available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html Contributors (this class): bspkrs - implementation
 */

package com.gtnewhorizon.structurelib;

import static cpw.mods.fml.client.config.GuiUtils.RESET_CHAR;
import static cpw.mods.fml.client.config.GuiUtils.UNDO_CHAR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.config.GuiButtonExt;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.GuiUnicodeGlyphButton;
import cpw.mods.fml.client.config.GuiUtils;
import cpw.mods.fml.client.config.HoverChecker;
import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import cpw.mods.fml.client.event.ConfigChangedEvent.PostConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.Event.Result;

/**
 * This class is the base GuiScreen for all config GUI screens. It can be extended by mods to provide the top-level
 * config screen that will be called when the Config button is clicked from the Main Menu Mods list.
 *
 * @author bspkrs
 */
public class GuiEditRegistryConfig extends GuiScreen {

    public static final String OMITTED = "..";
    public static final int LIST_WINDOW_PADDING = 20;
    public static final int MID_SECTION_WIDTH = 10;
    /**
     * A reference to the screen object that created this. Used for navigating between screens.
     */
    public final GuiScreen parentScreen;
    private final IConfigElement<String> disabledElement;
    private final String[] beforeDisabledValue;
    private final IConfigElement<String> orderElement;
    private final String[] beforeOrderValue;
    final Collection<EntriesList> lists;
    private final String registryName;
    EntriesList disabledEntries;
    EntriesList orderEntries;
    public String title = "Config GUI";
    public String titleLine2;
    private GuiButtonExt btnDefault;
    private GuiButtonExt btnUndoAll;
    public final String modID;
    /**
     * When set to a non-null value the OnConfigChanged and PostConfigChanged events will be posted when the Done button
     * is pressed if any configElements were changed (includes child screens). If not defined, the events will be posted
     * if the parent gui is null or if the parent gui is not an instance of GuiConfig.
     */
    public final String configID;
    public final boolean isWorldRunning;
    public final boolean allRequireWorldRestart;
    public final boolean allRequireMcRestart;
    public boolean needsRefresh = true;
    private HoverChecker undoHoverChecker;
    private HoverChecker resetHoverChecker;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public GuiEditRegistryConfig(GuiScreen parentScreen, IConfigElement disabled, IConfigElement order,
            String registryName, String modID, String configID, boolean allRequireWorldRestart,
            boolean allRequireMcRestart, String title, String titleLine2) {
        this.mc = parentScreen.mc;
        this.lists = Collections.newSetFromMap(new WeakHashMap<>());
        this.disabledElement = disabled;
        this.beforeDisabledValue = (String[]) this.disabledElement.getList();
        this.orderElement = order;
        this.beforeOrderValue = (String[]) this.orderElement.getList();
        this.parentScreen = parentScreen;
        this.allRequireWorldRestart = allRequireWorldRestart;
        this.allRequireMcRestart = allRequireMcRestart;
        this.registryName = registryName;
        this.modID = modID;
        this.configID = configID;
        this.isWorldRunning = mc.theWorld != null;
        if (title != null) this.title = title;
        this.titleLine2 = titleLine2;
        if (this.titleLine2 != null && this.titleLine2.startsWith(" > "))
            this.titleLine2 = this.titleLine2.replaceFirst(" > ", "");
    }

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        if (this.disabledEntries == null || this.needsRefresh) {
            this.disabledEntries = new EntriesList(
                    (width - MID_SECTION_WIDTH) / 2 - LIST_WINDOW_PADDING,
                    LIST_WINDOW_PADDING,
                    I18n.format("structurelib.configgui.disabled"));
            this.disabledEntries.initEntries(Arrays.asList(disabledElement.getList()));
        } else {
            this.disabledEntries.reinit((width - MID_SECTION_WIDTH) / 2 - LIST_WINDOW_PADDING, LIST_WINDOW_PADDING);
        }
        if (this.orderEntries == null || this.needsRefresh) {
            this.orderEntries = new EntriesList(
                    (width - MID_SECTION_WIDTH) / 2 - LIST_WINDOW_PADDING,
                    (width + MID_SECTION_WIDTH) / 2,
                I18n.format("structurelib.configgui.enabled"));
            this.orderEntries.initEntries(Arrays.asList(orderElement.getList()));
        } else {
            this.orderEntries
                    .reinit((width - MID_SECTION_WIDTH) / 2 - LIST_WINDOW_PADDING, (width + MID_SECTION_WIDTH) / 2);
        }
        this.needsRefresh = false;

        int undoGlyphWidth = mc.fontRenderer.getStringWidth(UNDO_CHAR) * 2;
        int resetGlyphWidth = mc.fontRenderer.getStringWidth(RESET_CHAR) * 2;
        int doneWidth = Math.max(mc.fontRenderer.getStringWidth(I18n.format("gui.done")) + 20, 100);
        int undoWidth = mc.fontRenderer.getStringWidth(" " + I18n.format("fml.configgui.tooltip.undoChanges"))
                + undoGlyphWidth
                + 20;
        int resetWidth = mc.fontRenderer.getStringWidth(" " + I18n.format("fml.configgui.tooltip.resetToDefault"))
                + resetGlyphWidth
                + 20;
        int buttonWidthHalf = (doneWidth + 5 + undoWidth + 5 + resetWidth + 5) / 2;
        this.buttonList.add(
                new GuiButtonExt(
                        2000,
                        this.width / 2 - buttonWidthHalf,
                        this.height - 29,
                        doneWidth,
                        20,
                        I18n.format("gui.done")));
        this.buttonList.add(
                this.btnDefault = new GuiUnicodeGlyphButton(
                        2001,
                        this.width / 2 - buttonWidthHalf + doneWidth + 5 + undoWidth + 5,
                        this.height - 29,
                        resetWidth,
                        20,
                        " " + I18n.format("fml.configgui.tooltip.resetToDefault"),
                        RESET_CHAR,
                        2.0F));
        this.buttonList.add(
                btnUndoAll = new GuiUnicodeGlyphButton(
                        2002,
                        this.width / 2 - buttonWidthHalf + doneWidth + 5,
                        this.height - 29,
                        undoWidth,
                        20,
                        " " + I18n.format("fml.configgui.tooltip.undoChanges"),
                        UNDO_CHAR,
                        2.0F));

        this.undoHoverChecker = new HoverChecker(this.btnUndoAll, 800);
        this.resetHoverChecker = new HoverChecker(this.btnDefault, 800);
    }

    /**
     * Called when the screen is unloaded. Used to disable keyboard repeat events
     */
    @Override
    public void onGuiClosed() {
        if (this.configID != null && this.parentScreen instanceof GuiConfig) {
            GuiConfig parentGuiConfig = (GuiConfig) this.parentScreen;
            parentGuiConfig.needsRefresh = true;
            parentGuiConfig.initGui();
        }

        if (!(this.parentScreen instanceof GuiConfig)) Keyboard.enableRepeatEvents(false);
    }

    public boolean saveConfigElements() {
        if (isChanged()) {
            disabledElement.set(disabledEntries.entries.stream().map(e -> e.name).toArray(String[]::new));
            orderElement.set(orderEntries.entries.stream().map(e -> e.name).toArray(String[]::new));
        }
        return false;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 2000) {
            try {
                if ((configID != null || this.parentScreen == null || !(this.parentScreen instanceof GuiConfig))
                        && isChanged()) {
                    saveConfigElements();
                    if (Loader.isModLoaded(modID)) {
                        ConfigChangedEvent event = new OnConfigChangedEvent(modID, configID, isWorldRunning, false);
                        FMLCommonHandler.instance().bus().post(event);
                        if (!event.getResult().equals(Result.DENY)) FMLCommonHandler.instance().bus()
                                .post(new PostConfigChangedEvent(modID, configID, isWorldRunning, false));

                        if (this.parentScreen instanceof GuiConfig) ((GuiConfig) this.parentScreen).needsRefresh = true;
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }

            this.mc.displayGuiScreen(this.parentScreen);
        } else if (button.id == 2001) {
            setToDefault();
        } else if (button.id == 2002) {
            undoChanges();
        }
    }

    public boolean isInitialized() {
        return disabledEntries != null && orderEntries != null;
    }

    public void undoChanges() {
        disabledEntries.initEntries(Arrays.asList(beforeDisabledValue));
        orderEntries.initEntries(Arrays.asList(beforeOrderValue));
    }

    public void setToDefault() {
        disabledEntries.initEntries(Arrays.asList(disabledElement.getList()));
        orderEntries.initEntries(Arrays.asList(orderElement.getList()));
    }

    public boolean isDefault() {
        return !orderEntries.areContentEqual(orderElement.getDefaults())
                || !disabledEntries.areContentEqual(disabledElement.getDefaults());
    }

    public boolean isChanged() {
        return !orderEntries.areContentEqual(beforeOrderValue) || !disabledEntries.areContentEqual(beforeDisabledValue);
    }

    /**
     * Called when the mouse is clicked.
     */
    @Override
    protected void mouseClicked(int x, int y, int mouseEvent) {
        if (mouseEvent != 0 || !this.disabledEntries.func_148179_a(x, y, mouseEvent)
                && !this.orderEntries.func_148179_a(x, y, mouseEvent)) {
            super.mouseClicked(x, y, mouseEvent);
        }
    }

    /**
     * Called when the mouse is moved or a mouse button is released. Signature: (mouseX, mouseY, which) which==-1 is
     * mouseMove, which==0 or which==1 is mouseUp
     */
    @Override
    protected void mouseMovedOrUp(int x, int y, int mouseEvent) {
        if (mouseEvent != 0 || !this.disabledEntries.func_148181_b(x, y, mouseEvent)
                && !this.orderEntries.func_148181_b(x, y, mouseEvent)) {
            super.mouseMovedOrUp(x, y, mouseEvent);
        }
    }

    /**
     * Fired when a key is typed. This is the equivalent of KeyListener.keyTyped(KeyEvent e).
     */
    @Override
    protected void keyTyped(char eventChar, int eventKey) {
        if (eventKey == Keyboard.KEY_ESCAPE) this.mc.displayGuiScreen(parentScreen);
    }

    String trimStringToWidth(String str, int width) {
        int strwidth = mc.fontRenderer.getStringWidth(str);
        int elipsisWidth = mc.fontRenderer.getStringWidth(OMITTED);
        if (strwidth > width - elipsisWidth && elipsisWidth > strwidth) {
            return mc.fontRenderer.trimStringToWidth(str, width - elipsisWidth).trim() + OMITTED;
        }
        return str;
    }

    /**
     * Draws the screen and all the components in it.
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        for (EntriesList list : lists) {
            list.handleDrag(mouseX, mouseY, partialTicks);
        }
        this.drawDefaultBackground();
        for (EntriesList list : lists) {
            list.drawScreen(mouseX, mouseY, partialTicks);
        }
        this.drawCenteredString(this.fontRendererObj, this.title, this.width / 2, 8, 16777215);
        String title2 = this.titleLine2;

        if (title2 != null) {
            title2 = trimStringToWidth(title2, width - 6);
            this.drawCenteredString(this.fontRendererObj, title2, this.width / 2, 18, 16777215);
        }

        this.btnUndoAll.enabled = isChanged();
        this.btnDefault.enabled = isDefault();
        super.drawScreen(mouseX, mouseY, partialTicks);
        for (EntriesList list : lists) {
            list.drawScreenPost(mouseX, mouseY, partialTicks);
        }
        if (this.undoHoverChecker.checkHover(mouseX, mouseY)) this.drawToolTip(
                this.mc.fontRenderer.listFormattedStringToWidth(I18n.format("fml.configgui.tooltip.undoAll"), 300),
                mouseX,
                mouseY);
        if (this.resetHoverChecker.checkHover(mouseX, mouseY)) this.drawToolTip(
                this.mc.fontRenderer.listFormattedStringToWidth(I18n.format("fml.configgui.tooltip.resetAll"), 300),
                mouseX,
                mouseY);
    }

    @SuppressWarnings("rawtypes")
    public void drawToolTip(List stringList, int x, int y) {
        this.func_146283_a(stringList, x, y);
    }

    private class EntriesList extends GuiListExtended {

        final String heading;
        final List<ListEntry> entries = new ArrayList<>();
        int controlX;

        public EntriesList(int width, int left, String heading) {
            super(
                    mc,
                    width,
                    GuiEditRegistryConfig.this.height,
                    titleLine2 != null ? 33 : 23,
                    GuiEditRegistryConfig.this.height - 32,
                    20);
            this.heading = heading;
            if (heading != null) {
                setHasListHeader(true, mc.fontRenderer.FONT_HEIGHT + 4);
            }
            setSlotXBoundsFromLeft(left);
            this.controlX = this.getScrollBarX() - 20;
            lists.add(this);
        }

        @Override
        public void drawScreen(int p_148128_1_, int p_148128_2_, float p_148128_3_) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            ScaledResolution scaledresolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
            GL11.glScissor((left - 4) * scaledresolution.getScaleFactor(), (int) ((scaledresolution.getScaledHeight_double() - (bottom + 4)) * scaledresolution.getScaleFactor()), (width + 8) * scaledresolution.getScaleFactor(), (height + 8) * scaledresolution.getScaleFactor());
            super.drawScreen(p_148128_1_, p_148128_2_, p_148128_3_);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }

        @Override
        public void overlayBackground(int p_148136_1_, int p_148136_2_, int p_148136_3_, int p_148136_4_) {
        }

        @Override
        protected void drawContainerBackground(Tessellator tessellator) {
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 0, 1);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glShadeModel(GL11.GL_SMOOTH);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            tessellator.startDrawingQuads();
            tessellator.setColorRGBA_I(0, 64);
            tessellator.addVertex(left, bottom, 0);
            tessellator.addVertex(right, bottom, 0);
            tessellator.addVertex(right, top, 0);
            tessellator.addVertex(left, top, 0);
            tessellator.draw();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glShadeModel(GL11.GL_FLAT);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_BLEND);
        }

        @Override
        protected void drawListHeader(int p_148129_1_, int p_148129_2_, Tessellator p_148129_3_) {
            if (heading == null) {
                return;
            }
            FontRenderer fr = mc.fontRenderer;
            fr.drawString(
                this.heading,
                    p_148129_1_ + this.width / 2 - fr.getStringWidth(this.heading) / 2,
                    Math.min(this.top + 3, p_148129_2_),
                    0xffffff);
        }

        void reinit(int width, int left) {
            this.width = width;
            this.bottom = GuiEditRegistryConfig.this.height - 32;
            this.setSlotXBoundsFromLeft(left);
            this.controlX = this.getScrollBarX() - 20;
        }

        void initEntries(Iterable<?> entries) {
            this.controlX = this.getScrollBarX() - 20;
            this.entries.clear();
            for (Object entry : entries) {
                addEntry(new ListEntry(entry.toString()));
            }
        }

        void addEntry(ListEntry entry) {
            entry.entriesList = this;
            this.entries.add(entry);
            this.controlX = Math.min(this.controlX, this.getScrollBarX() - entry.getControlSetWidth() - 2);
        }

        @Override
        public IGuiListEntry getListEntry(int i) {
            return entries.get(i);
        }

        @Override
        protected int getSize() {
            return entries.size();
        }

        int getLabelX() {
            return left + 2;
        }

        int getControlX() {
            return controlX;
        }

        @Override
        public int getListWidth() {
            return width;
        }

        @Override
        protected int getScrollBarX() {
            return left + getListWidth() - 6;
        }

        public void handleDrag(int mouseX, int mouseY, float partialTicks) {
            for (int i = 0, entriesSize = entries.size(); i < entriesSize; i++) {
                ListEntry entry = entries.get(i);
                if (entry.handleDrag(i, mouseX, mouseY, partialTicks)) {
                    return;
                }
            }
        }

        public void drawScreenPost(int mouseX, int mouseY, float partialTicks) {
            for (ListEntry entry : entries) {
                entry.drawToolTip(mouseX, mouseY);
            }
        }

        public boolean areContentEqual(Object[] vals) {
            if (vals.length != entries.size()) return false;
            for (int i = 0; i < entries.size(); i++) {
                ListEntry entry = entries.get(i);
                if (!entry.name.equals(vals[i])) return false;
            }
            return true;
        }
    }

    class ListEntry implements GuiListExtended.IGuiListEntry {

        private EntriesList entriesList;
        private final HoverChecker hoverCheckerText = new HoverChecker(0, 0, 0, 0, 0);
        final String name;
        final String localizedName;
        private final String tooltip;
        public boolean modified = false;

        private final GuiButtonExt btnDrag = new GuiButtonDrag(1, 0, 0, 20, 20);
        private final HoverChecker hoverCheckerBtnDrag = new HoverChecker(btnDrag, 800);
        private boolean isDragging = false;
        private long lastScroll = 0;

        ListEntry(String name) {
            this.name = name;
            String nameKey = "structurelib.sortedregistry." + registryName + "." + name;
            this.localizedName = trans(nameKey, name);

            String tooltipKey = nameKey + ".tooltip";
            String tooltip = trans(tooltipKey, null);
            StringBuilder tooltipBuilder = new StringBuilder();
            tooltipBuilder.append(EnumChatFormatting.GREEN);
            tooltipBuilder.append(this.localizedName);
            if (tooltip != null) {
                tooltipBuilder.append('\n').append(EnumChatFormatting.YELLOW).append(tooltip);
            }
            tooltipBuilder.append('\n');
            tooltipBuilder.append(EnumChatFormatting.DARK_GRAY);
            tooltipBuilder.append(this.name);
            this.tooltip = tooltipBuilder.toString();
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, Tessellator tessellator,
                int mouseX, int mouseY, boolean isSelected) {
            drawOutlineBox(x, y, listWidth, slotHeight, tessellator);
            drawLabel(y, slotHeight);
            drawControlSet(y, slotHeight, mouseX, mouseY);
        }

        private void drawOutlineBox(int x, int y, int listWidth, int slotHeight, Tessellator tessellator) {
            if (isDragging) {
                // left x, right x, top y, bottom y
                int lx = x, rx = entriesList.getScrollBarX() - 4, ty = y, by = y + slotHeight;
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                tessellator.startDrawingQuads();
                tessellator.setColorOpaque_I(0x808080);
                tessellator.addVertexWithUV(lx - 4, by + 4, 0.0D, 0.0D, 1.0D);
                tessellator.addVertexWithUV(rx + 4, by + 4, 0.0D, 1.0D, 1.0D);
                tessellator.addVertexWithUV(rx + 4, ty - 4, 0.0D, 1.0D, 0.0D);
                tessellator.addVertexWithUV(lx - 4, ty - 4, 0.0D, 0.0D, 0.0D);
                tessellator.setColorOpaque_I(0x202020);
                tessellator.addVertexWithUV(lx - 2, by + 2, 0.0D, 0.0D, 1.0D);
                tessellator.addVertexWithUV(rx + 2, by + 2, 0.0D, 1.0D, 1.0D);
                tessellator.addVertexWithUV(rx + 2, ty - 2, 0.0D, 1.0D, 0.0D);
                tessellator.addVertexWithUV(lx - 2, ty - 2, 0.0D, 0.0D, 0.0D);
                tessellator.draw();
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }
        }

        private void drawControlSet(int y, int slotHeight, int mouseX, int mouseY) {
            btnDrag.xPosition = entriesList.getControlX();
            btnDrag.yPosition = y + (slotHeight - btnDrag.height) / 2;
            btnDrag.drawButton(mc, mouseX, mouseY);
        }

        private void drawLabel(int y, int slotHeight) {
            String label = modified ? EnumChatFormatting.ITALIC + localizedName : localizedName;
            int allowedWidth = entriesList.getControlX() - entriesList.getLabelX() - 4;
            // probably not very wise to recompute this every render tick, but it doesn't matter for now
            String toDraw = trimStringToWidth(label, allowedWidth);
            hoverCheckerText.updateBounds(
                    y + slotHeight / 2 - mc.fontRenderer.FONT_HEIGHT / 2,
                    y + slotHeight / 2 + mc.fontRenderer.FONT_HEIGHT / 2,
                    entriesList.getLabelX(),
                    entriesList.getLabelX() + mc.fontRenderer.getStringWidth(toDraw));

            mc.fontRenderer.drawString(
                    toDraw,
                    entriesList.getLabelX(),
                    y + slotHeight / 2 - mc.fontRenderer.FONT_HEIGHT / 2,
                    getLabelColor());
        }

        void drawToolTip(int mouseX, int mouseY) {
            // ensure checkHover() is always called, or the hover threshold might fail under certain corner cases
            if (hoverCheckerText.checkHover(mouseX, mouseY)) {
                GuiEditRegistryConfig.this.drawToolTip(
                        mc.fontRenderer.listFormattedStringToWidth(tooltip, entriesList.getListWidth()),
                        mouseX,
                        mouseY);
            }
            if (hoverCheckerBtnDrag.checkHover(mouseX, mouseY) && !isDragging) {
                GuiEditRegistryConfig.this.drawToolTip(
                        mc.fontRenderer.listFormattedStringToWidth(
                                I18n.format("structurelib.configgui.drag.tooltip"),
                                entriesList.getListWidth()),
                        mouseX,
                        mouseY);
            }
        }

        protected int getLabelColor() {
            return isDragging ? 0x888888 : 0xffffff;
        }

        int getControlSetWidth() {
            return this.btnDrag.getButtonWidth();
        }

        public boolean handleDrag(int index, int mouseX, int mouseY, float partialTicks) {
            if (!isDragging) {
                return false;
            }
            for (EntriesList list : lists) {
                if (list == entriesList) continue;
                if (mouseX > list.left && mouseX < list.right && mouseY > list.top && mouseY < list.bottom) {
                    int l;
                    if (list.entries.isEmpty()) l = 0;
                    else l = list.func_148124_c(mouseX, mouseY);
                    if (l < 0) {
                        if (mouseY < (list.bottom + list.top) / 2) {
                            l = 0;
                        } else {
                            l = list.entries.size();
                        }
                    }
                    entriesList.func_148143_b(true);
                    entriesList.entries.remove(index);
                    list.func_148143_b(false);
                    list.entries.add(l, this);
                    this.entriesList = list;
                    return true;
                }
            }
            long now = System.currentTimeMillis();
            if (now - lastScroll > 20) {
                if (mouseY < entriesList.top) {
                    entriesList.scrollBy(-3);
                    lastScroll = now;
                } else if (mouseY > entriesList.bottom) {
                    entriesList.scrollBy(3);
                    lastScroll = now;
                }
            }
            int l = entriesList.func_148124_c(
                    MathHelper.clamp_int(mouseX, entriesList.left, entriesList.right),
                    MathHelper.clamp_int(mouseY, entriesList.top, entriesList.bottom));
            if (l < 0 || l == index) return true;
            entriesList.entries.remove(index);
            entriesList.entries.add(l, this);
            return true;
        }

        @Override
        public void mouseReleased(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            isDragging = false;
        }

        @Override
        public boolean mousePressed(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            if (btnDrag.mousePressed(mc, x, y)) {
                isDragging = true;
                return true;
            }
            return false;
        }
    }

    private static String trans(String transKey, String fallback) {
        String translated = I18n.format(transKey);
        return translated.equals(transKey) ? fallback : translated;
    }

    private static class GuiButtonDrag extends GuiButtonExt {

        public GuiButtonDrag(int id, int xPos, int yPos, int width, int height) {
            super(id, xPos, yPos, width, height, null);
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (this.visible) {
                this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
                        && mouseX < this.xPosition + this.width
                        && mouseY < this.yPosition + this.height;
                int k = this.getHoverState(this.field_146123_n);
                GuiUtils.drawContinuousTexturedBox(
                        buttonTextures,
                        this.xPosition,
                        this.yPosition,
                        0,
                        46 + k * 20,
                        this.width,
                        this.height,
                        200,
                        20,
                        2,
                        3,
                        2,
                        2,
                        this.zLevel);
                mc.renderEngine.bindTexture(new ResourceLocation("structurelib", "textures/gui/fourwayarrow2.png"));
                int x = this.xPosition + (this.width - 12) / 2;
                int y = this.yPosition + (this.height - 12) / 2;
                Tessellator tessellator = Tessellator.instance;
                tessellator.startDrawingQuads();
                tessellator.addVertexWithUV(x, y + 12, zLevel, 0, 1);
                tessellator.addVertexWithUV(x + 12, y + 12, zLevel, 1, 1);
                tessellator.addVertexWithUV(x + 12, y, zLevel, 1, 0);
                tessellator.addVertexWithUV(x, y, zLevel, 0, 0);
                tessellator.draw();
            }
        }
    }
}
