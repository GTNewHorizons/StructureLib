package com.gtnewhorizon.structurelib.gui;

import static org.lwjgl.opengl.ARBImaging.*;
import static org.lwjgl.opengl.ARBImaging.GL_SEPARABLE_2D;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_ALPHA_TEST_FUNC;
import static org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_3D;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB;
import static org.lwjgl.opengl.GL15.GL_FOG_COORD_ARRAY;
import static org.lwjgl.opengl.GL20.*;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.ImmutableMap;
import com.gtnewhorizon.structurelib.ChannelDescription;
import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.gui.GuiScrollableList.IGuiScreen;

import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

@Optional.Interface(iface = "codechicken.nei.api.INEIGuiHandler", modid = "NotEnoughItems")
public class GuiScreenConfigureChannels extends GuiContainer implements IGuiScreen, INEIGuiHandler {

    private static final String I18N_PREFIX = "item.structurelib.constructableTrigger.gui.";

    private static final int ADD_BTN = 0;
    private static final int UNSET_BTN = 1;
    private static final int WIPE_BTN = 2;

    private static final int KEY_MAX_WIDTH = 50;
    private final ItemStack trigger;
    private final GuiChannelsList list;
    private final GuiAutoCompleteList autoCompleteList;
    private final Map<String, List<String>> tooltipSplitCache = new HashMap<>();
    private final TIntObjectMap<Map<String, String>> trimCache = new TIntObjectHashMap<>();
    private GuiTextField key, value;
    protected int guiTop, guiLeft;

    public GuiScreenConfigureChannels(Container container, ItemStack trigger) {
        super(container);

        this.trigger = trigger;
        list = new GuiChannelsList(152, 100, 12, 12, 14);
        list.addSelectionListener((list, selectedIndex) -> {
            Entry<String, Integer> e = list.getElementAt(selectedIndex);
            key.setText(e.getKey());
            value.setText(e.getValue().toString());
            updateButtons();
        });
        autoCompleteList = new GuiAutoCompleteList(151 + 12 - 45 - 2, 50, 45, 119 + 12, 14);
    }

    @Override
    public void initGui() {
        super.initGui();
        // so you can keep holding backspace to delete a long sequence of chars
        Keyboard.enableRepeatEvents(true);

        guiLeft = (this.width - this.getXSize()) / 2;
        guiTop = (this.height - this.getYSize()) / 2;

        key = new GuiTextField(fontRendererObj, guiLeft + 45, guiTop + 119, 151 + 12 - 45, 12) {

            @Override
            public void writeText(String text) {
                // force lower case
                super.writeText(text.toLowerCase(Locale.ROOT));
                updateButtons();
            }

            @Override
            public void mouseClicked(int p_146192_1_, int p_146192_2_, int p_146192_3_) {
                super.mouseClicked(p_146192_1_, p_146192_2_, p_146192_3_);
                boolean flag = p_146192_1_ >= this.xPosition && p_146192_1_ < this.xPosition + this.width
                        && p_146192_2_ >= this.yPosition
                        && p_146192_2_ < this.yPosition + this.height;
                if (flag && p_146192_3_ == 1) {
                    key.setText("");
                    value.setText("");
                }
            }

            @Override
            public void setText(String p_146180_1_) {
                super.setText(p_146180_1_);
                updateButtons();
            }
        };
        value = new GuiTextField(fontRendererObj, guiLeft + 45, guiTop + 139, 151 + 12 - 45, 12) {

            @Override
            public void writeText(String text) {
                // ignore write requests containing non digit characters.
                // we don't accept decimals or negative numbers, so we can safely use isDigit instead of something more
                // advanced
                // use codePoints() in case someone pasted an emoji by mistake
                if (text != null && text.codePoints().allMatch(Character::isDigit)) {
                    super.writeText(text);
                    updateButtons();
                }
            }

            @Override
            public void setFocused(boolean p_146195_1_) {
                if (!p_146195_1_ && isFocused() && !StringUtils.isBlank(getText())) {
                    int result;
                    try {
                        result = Math.max(Integer.parseInt(getText()), 1);
                    } catch (NumberFormatException e) {
                        result = 1;
                    }
                    setText(String.valueOf(result));
                }
                super.setFocused(p_146195_1_);
            }

            @Override
            public void mouseClicked(int p_146192_1_, int p_146192_2_, int p_146192_3_) {
                super.mouseClicked(p_146192_1_, p_146192_2_, p_146192_3_);
                boolean flag = p_146192_1_ >= this.xPosition && p_146192_1_ < this.xPosition + this.width
                        && p_146192_2_ >= this.yPosition
                        && p_146192_2_ < this.yPosition + this.height;
                if (flag && p_146192_3_ == 1) {
                    setText("");
                }
            }

            @Override
            public void setText(String p_146180_1_) {
                super.setText(p_146180_1_);
                updateButtons();
            }
        };

        list.onGuiInit(this);
        autoCompleteList.onGuiInit(this);

        addButton(
                new GuiButton(
                        ADD_BTN,
                        guiLeft + 12,
                        guiTop + 157,
                        47,
                        20,
                        I18n.format("item.structurelib.constructableTrigger.gui.add")));
        addButton(
                new GuiButton(
                        UNSET_BTN,
                        guiLeft + 65,
                        guiTop + 157,
                        47,
                        20,
                        I18n.format("item.structurelib.constructableTrigger.gui.unset")));
        addButton(
                new GuiButton(
                        WIPE_BTN,
                        guiLeft + 118,
                        guiTop + 157,
                        47,
                        20,
                        I18n.format("item.structurelib.constructableTrigger.gui.wipe")));

        updateButtons();
        tooltipSplitCache.clear();
    }

    @Override
    public int getGuiLeft() {
        return guiLeft;
    }

    @Override
    public int getGuiTop() {
        return guiTop;
    }

    @Override
    public int getXSize() {
        return 176;
    }

    @Override
    public int getYSize() {
        return 188;
    }

    @Override
    public void addButton(GuiButton button) {
        getButtonList().add(button);
    }

    @Override
    public void removeButton(GuiButton button) {
        getButtonList().remove(button);
    }

    @Override
    public int getOverlayOffsetX() {
        return 0;
    }

    @Override
    public void doActionPerformed(GuiButton but) {
        actionPerformed(but);
    }

    @SuppressWarnings("unchecked")
    private List<GuiButton> getButtonList() {
        return buttonList;
    }

    private boolean isMouseOverValue() {
        int mx = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int my = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        return mx >= value.xPosition && mx < value.xPosition + value.width
                && my >= value.yPosition
                && my < value.yPosition + value.height;

    }

    @Override
    public void handleMouseInput() {
        int delta = Mouse.getEventDWheel();
        if (delta != 0) {
            if (isMouseOverValue()) {
                if (delta > 0) {
                    value.setText(String.valueOf(getValue() + 1));
                } else {
                    value.setText(String.valueOf(Math.max(getValue() - 1, 1)));
                }
            } else {
                list.handleDWheel(delta);
            }
        }
        super.handleMouseInput();
    }

    @Override
    protected void mouseClicked(int mX, int mY, int button) {
        super.mouseClicked(mX, mY, button);
        key.mouseClicked(mX, mY, button);
        value.mouseClicked(mX, mY, button);
    }

    @Override
    protected void keyTyped(char aChar, int aKey) {
        GuiScrollableList<?> l = key.isFocused() ? autoCompleteList : list;
        switch (aKey) {
            case Keyboard.KEY_TAB:
                if (key.isFocused()) {
                    key.setFocused(false);
                    value.setFocused(true);
                } else {
                    key.setFocused(true);
                    value.setFocused(false);
                }
                return;
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                if (key.isFocused() && autoCompleteList.getSelectedElement() != null) {
                    key.setText(autoCompleteList.getSelectedElement());
                    key.setFocused(false);
                    value.setFocused(true);
                    return;
                }
                GuiButton add = getButtonList().get(ADD_BTN);
                if (add.enabled) doActionPerformed(add);
                return;
            case Keyboard.KEY_UP:
                if (l.selectedIndex > 0) l.setSelection(l.selectedIndex - 1);
                return;
            case Keyboard.KEY_DOWN:
                if (l.selectedIndex < l.getNumElements() - 1) l.setSelection(l.selectedIndex + 1);
                return;
            case Keyboard.KEY_ESCAPE:
                if (key.isFocused()) {
                    if (autoCompleteList.getSelectedElement() != null) {
                        autoCompleteList.reset();
                    } else {
                        key.setFocused(false);
                    }
                    return;
                }
                break;
        }
        if (key.textboxKeyTyped(aChar, aKey)) {
            updateButtons();
            return;
        }
        if (value.textboxKeyTyped(aChar, aKey)) return;
        super.keyTyped(aChar, aKey);
    }

    private void updateButtons() {
        // this will be called from setText of key and value. NEVER UPDATE THE VALUE OF THESE HERE OR GET A
        // STACKOVERFLOW!
        String keyText = key.getText();
        boolean existing = !StringUtils.isEmpty(keyText) && ChannelDataAccessor.hasSubChannel(trigger, keyText);
        getButtonList().get(ADD_BTN).displayString = existing ? I18n.format(I18N_PREFIX + "set")
                : I18n.format(I18N_PREFIX + "add");
        getButtonList().get(ADD_BTN).enabled = !StringUtils.isBlank(value.getText())
                && Integer.parseInt(value.getText()) > 0;
        getButtonList().get(UNSET_BTN).enabled = existing && !StringUtils.isBlank(value.getText());

        autoCompleteList.reset();
    }

    private int getValue() {
        try {
            return Integer.parseInt(value.getText());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    protected void actionPerformed(GuiButton btn) {
        if (btn == null) return;
        switch (btn.id) {
            case ADD_BTN:
                int value = getValue();
                if (value <= 0) return;
                ChannelDataAccessor.setChannelData(trigger, key.getText(), value);
                break;
            case UNSET_BTN:
                ChannelDataAccessor.unsetChannelData(trigger, key.getText());
                break;
            case WIPE_BTN:
                ChannelDataAccessor.wipeChannelData(trigger);
                key.setText("");
                this.value.setText("");
                break;
        }

        updateButtons();

        super.actionPerformed(btn);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        StructureLib.instance().proxy().uploadChannels(trigger);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        key.updateCursorCounter();
        value.updateCursorCounter();
    }

    @Override
    public void drawGuiContainerBackgroundLayer(float partialTick, int mX, int mY) {
        mc.renderEngine.bindTexture(new ResourceLocation("structurelib", "textures/gui/channels.png"));
        int topLeftX = (this.width - this.getXSize()) / 2;
        int topLeftY = (this.height - this.getYSize()) / 2;
        drawTexturedModalRect(topLeftX, topLeftY, 0, 0, getXSize(), getYSize());
        list.drawScreen(mX, mY, partialTick);
        fontRendererObj.drawString(
                I18n.format("item.structurelib.constructableTrigger.gui.key"),
                guiLeft + 12,
                guiTop + 122,
                0);
        key.drawTextBox();
        fontRendererObj.drawString(
                I18n.format("item.structurelib.constructableTrigger.gui.value"),
                guiLeft + 12,
                guiTop + 142,
                0);
        value.drawTextBox();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        if (key.isFocused() && autoCompleteList.getNumElements() > 0) {
            autoCompleteList.drawScreen(mouseX, mouseY, 0);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        String highlight = null;
        if (key.isFocused() && autoCompleteList.getNumElements() > 0) {
            highlight = autoCompleteList.getSelectedElement();
        }
        if (highlight == null && ChannelDescription.has(key.getText())) {
            highlight = key.getText();
        }
        if (highlight != null) {
            drawChannelDescriptionTooltip(highlight);
        }
    }

    private void drawChannelDescriptionTooltip(String candidate) {
        List<String> tooltip = tooltipSplitCache.computeIfAbsent(candidate, this::getChannelDescriptionTooltip);
        drawHoveringText(tooltip, guiLeft + 151 + 12, guiTop + 122, fontRendererObj);
    }

    public static void debugPrintGLEnable() {
        Map<Integer, String> functions = ImmutableMap.<Integer, String>builder().put(GL11.GL_NEVER, "GL_NEVER")
                .put(GL11.GL_LESS, "GL_LESS").put(GL11.GL_EQUAL, "GL_EQUAL").put(GL11.GL_LEQUAL, "GL_LEQUAL")
                .put(GL11.GL_GREATER, "GL_GREATER").put(GL11.GL_NOTEQUAL, "GL_NOTEQUAL")
                .put(GL11.GL_GEQUAL, "GL_GEQUAL").put(GL11.GL_ALWAYS, "GL_ALWAYS").put(GL11.GL_ZERO, "GL_ZERO")
                .put(GL11.GL_ONE, "GL_ONE").put(GL11.GL_SRC_COLOR, "GL_SRC_COLOR")
                .put(GL11.GL_ONE_MINUS_SRC_COLOR, "GL_ONE_MINUS_SRC_COLOR").put(GL11.GL_DST_COLOR, "GL_DST_COLOR")
                .put(GL11.GL_ONE_MINUS_DST_COLOR, "GL_ONE_MINUS_DST_COLOR").put(GL11.GL_SRC_ALPHA, "GL_SRC_ALPHA")
                .put(GL11.GL_ONE_MINUS_SRC_ALPHA, "GL_ONE_MINUS_SRC_ALPHA").put(GL11.GL_DST_ALPHA, "GL_DST_ALPHA")
                .put(GL11.GL_ONE_MINUS_DST_ALPHA, "GL_ONE_MINUS_DST_ALPHA")
                .put(GL11.GL_CONSTANT_COLOR, "GL_CONSTANT_COLOR")
                .put(GL11.GL_ONE_MINUS_CONSTANT_COLOR, "GL_ONE_MINUS_CONSTANT_COLOR")
                .put(GL11.GL_CONSTANT_ALPHA, "GL_CONSTANT_ALPHA")
                .put(GL11.GL_ONE_MINUS_CONSTANT_ALPHA, "GL_ONE_MINUS_CONSTANT_ALPHA")
                .put(GL11.GL_SRC_ALPHA_SATURATE, "GL_SRC_ALPHA_SATURATE").build();
        System.out.println("GL_ALPHA_TEST: " + GL11.glIsEnabled(GL_ALPHA_TEST));
        System.out.println("GL_AUTO_NORMAL: " + GL11.glIsEnabled(GL_AUTO_NORMAL));
        System.out.println("GL_BLEND: " + GL11.glIsEnabled(GL_BLEND));
        System.out.println("GL_COLOR_ARRAY: " + GL11.glIsEnabled(GL_COLOR_ARRAY));
        System.out.println("GL_COLOR_LOGIC_OP: " + GL11.glIsEnabled(GL_COLOR_LOGIC_OP));
        System.out.println("GL_COLOR_MATERIAL: " + GL11.glIsEnabled(GL_COLOR_MATERIAL));
        System.out.println("GL_COLOR_SUM: " + GL11.glIsEnabled(GL_COLOR_SUM));
        System.out.println("GL_COLOR_TABLE: " + GL11.glIsEnabled(GL_COLOR_TABLE));
        System.out.println("GL_CONVOLUTION_1D: " + GL11.glIsEnabled(GL_CONVOLUTION_1D));
        System.out.println("GL_CONVOLUTION_2D: " + GL11.glIsEnabled(GL_CONVOLUTION_2D));
        System.out.println("GL_CULL_FACE: " + GL11.glIsEnabled(GL_CULL_FACE));
        System.out.println("GL_DEPTH_TEST: " + GL11.glIsEnabled(GL_DEPTH_TEST));
        System.out.println("GL_DITHER: " + GL11.glIsEnabled(GL_DITHER));
        System.out.println("GL_EDGE_FLAG_ARRAY: " + GL11.glIsEnabled(GL_EDGE_FLAG_ARRAY));
        System.out.println("GL_FOG: " + GL11.glIsEnabled(GL_FOG));
        System.out.println("GL_FOG_COORD_ARRAY: " + GL11.glIsEnabled(GL_FOG_COORD_ARRAY));
        System.out.println("GL_HISTOGRAM: " + GL11.glIsEnabled(GL_HISTOGRAM));
        System.out.println("GL_INDEX_ARRAY: " + GL11.glIsEnabled(GL_INDEX_ARRAY));
        System.out.println("GL_INDEX_LOGIC_OP: " + GL11.glIsEnabled(GL_INDEX_LOGIC_OP));
        System.out.println("GL_LIGHTING: " + GL11.glIsEnabled(GL_LIGHTING));
        System.out.println("GL_LINE_SMOOTH: " + GL11.glIsEnabled(GL_LINE_SMOOTH));
        System.out.println("GL_LINE_STIPPLE: " + GL11.glIsEnabled(GL_LINE_STIPPLE));
        System.out.println("GL_MAP1_COLOR_4: " + GL11.glIsEnabled(GL_MAP1_COLOR_4));
        System.out.println("GL_MAP1_INDEX: " + GL11.glIsEnabled(GL_MAP1_INDEX));
        System.out.println("GL_MAP1_NORMAL: " + GL11.glIsEnabled(GL_MAP1_NORMAL));
        System.out.println("GL_MAP1_TEXTURE_COORD_1: " + GL11.glIsEnabled(GL_MAP1_TEXTURE_COORD_1));
        System.out.println("GL_MAP1_TEXTURE_COORD_2: " + GL11.glIsEnabled(GL_MAP1_TEXTURE_COORD_2));
        System.out.println("GL_MAP1_TEXTURE_COORD_3: " + GL11.glIsEnabled(GL_MAP1_TEXTURE_COORD_3));
        System.out.println("GL_MAP1_TEXTURE_COORD_4: " + GL11.glIsEnabled(GL_MAP1_TEXTURE_COORD_4));
        System.out.println("GL_MAP2_COLOR_4: " + GL11.glIsEnabled(GL_MAP2_COLOR_4));
        System.out.println("GL_MAP2_INDEX: " + GL11.glIsEnabled(GL_MAP2_INDEX));
        System.out.println("GL_MAP2_NORMAL: " + GL11.glIsEnabled(GL_MAP2_NORMAL));
        System.out.println("GL_MAP2_TEXTURE_COORD_1: " + GL11.glIsEnabled(GL_MAP2_TEXTURE_COORD_1));
        System.out.println("GL_MAP2_TEXTURE_COORD_2: " + GL11.glIsEnabled(GL_MAP2_TEXTURE_COORD_2));
        System.out.println("GL_MAP2_TEXTURE_COORD_3: " + GL11.glIsEnabled(GL_MAP2_TEXTURE_COORD_3));
        System.out.println("GL_MAP2_TEXTURE_COORD_4: " + GL11.glIsEnabled(GL_MAP2_TEXTURE_COORD_4));
        System.out.println("GL_MAP2_VERTEX_3: " + GL11.glIsEnabled(GL_MAP2_VERTEX_3));
        System.out.println("GL_MAP2_VERTEX_4: " + GL11.glIsEnabled(GL_MAP2_VERTEX_4));
        System.out.println("GL_MINMAX: " + GL11.glIsEnabled(GL_MINMAX));
        System.out.println("GL_MULTISAMPLE: " + GL11.glIsEnabled(GL_MULTISAMPLE));
        System.out.println("GL_NORMAL_ARRAY: " + GL11.glIsEnabled(GL_NORMAL_ARRAY));
        System.out.println("GL_NORMALIZE: " + GL11.glIsEnabled(GL_NORMALIZE));
        System.out.println("GL_POINT_SMOOTH: " + GL11.glIsEnabled(GL_POINT_SMOOTH));
        System.out.println("GL_POINT_SPRITE: " + GL11.glIsEnabled(GL_POINT_SPRITE));
        System.out.println("GL_POLYGON_SMOOTH: " + GL11.glIsEnabled(GL_POLYGON_SMOOTH));
        System.out.println("GL_POLYGON_OFFSET_FILL: " + GL11.glIsEnabled(GL_POLYGON_OFFSET_FILL));
        System.out.println("GL_POLYGON_OFFSET_LINE: " + GL11.glIsEnabled(GL_POLYGON_OFFSET_LINE));
        System.out.println("GL_POLYGON_OFFSET_POINT: " + GL11.glIsEnabled(GL_POLYGON_OFFSET_POINT));
        System.out.println("GL_POLYGON_STIPPLE: " + GL11.glIsEnabled(GL_POLYGON_STIPPLE));
        System.out.println("GL_POST_COLOR_MATRIX_COLOR_TABLE: " + GL11.glIsEnabled(GL_POST_COLOR_MATRIX_COLOR_TABLE));
        System.out.println("GL_POST_CONVOLUTION_COLOR_TABLE: " + GL11.glIsEnabled(GL_POST_CONVOLUTION_COLOR_TABLE));
        System.out.println("GL_RESCALE_NORMAL: " + GL11.glIsEnabled(GL_RESCALE_NORMAL));
        System.out.println("GL_SAMPLE_ALPHA_TO_COVERAGE: " + GL11.glIsEnabled(GL_SAMPLE_ALPHA_TO_COVERAGE));
        System.out.println("GL_SAMPLE_ALPHA_TO_ONE: " + GL11.glIsEnabled(GL_SAMPLE_ALPHA_TO_ONE));
        System.out.println("GL_SAMPLE_COVERAGE: " + GL11.glIsEnabled(GL_SAMPLE_COVERAGE));
        System.out.println("GL_SCISSOR_TEST: " + GL11.glIsEnabled(GL_SCISSOR_TEST));
        System.out.println("GL_SECONDARY_COLOR_ARRAY: " + GL11.glIsEnabled(GL_SECONDARY_COLOR_ARRAY));
        System.out.println("GL_SEPARABLE_2D: " + GL11.glIsEnabled(GL_SEPARABLE_2D));
        System.out.println("GL_STENCIL_TEST: " + GL11.glIsEnabled(GL_STENCIL_TEST));
        System.out.println("GL_TEXTURE_1D: " + GL11.glIsEnabled(GL_TEXTURE_1D));
        System.out.println("GL_TEXTURE_2D: " + GL11.glIsEnabled(GL_TEXTURE_2D));
        System.out.println("GL_TEXTURE_3D: " + GL11.glIsEnabled(GL_TEXTURE_3D));
        System.out.println("GL_TEXTURE_COORD_ARRAY: " + GL11.glIsEnabled(GL_TEXTURE_COORD_ARRAY));
        System.out.println("GL_TEXTURE_CUBE_MAP: " + GL11.glIsEnabled(GL_TEXTURE_CUBE_MAP));
        System.out.println("GL_TEXTURE_GEN_Q: " + GL11.glIsEnabled(GL_TEXTURE_GEN_Q));
        System.out.println("GL_TEXTURE_GEN_R: " + GL11.glIsEnabled(GL_TEXTURE_GEN_R));
        System.out.println("GL_TEXTURE_GEN_S: " + GL11.glIsEnabled(GL_TEXTURE_GEN_S));
        System.out.println("GL_TEXTURE_GEN_T: " + GL11.glIsEnabled(GL_TEXTURE_GEN_T));
        System.out.println("GL_VERTEX_ARRAY: " + GL11.glIsEnabled(GL_VERTEX_ARRAY));
        System.out.println("GL_VERTEX_PROGRAM_POINT_SIZE: " + GL11.glIsEnabled(GL_VERTEX_PROGRAM_POINT_SIZE));
        System.out.println("GL_VERTEX_PROGRAM_TWO_SIDE: " + GL11.glIsEnabled(GL_VERTEX_PROGRAM_TWO_SIDE));
        System.out.println("Depth Function: " + functions.get(GL11.glGetInteger(GL_DEPTH_FUNC)));
        System.out.printf(
                "Blend Function: DST: Alpha %s RGB %s, SRC: Alpha %s RGB %s\n",
                functions.get(GL11.glGetInteger(GL_BLEND_DST_ALPHA)),
                functions.get(GL11.glGetInteger(GL_BLEND_DST_RGB)),
                functions.get(GL11.glGetInteger(GL_BLEND_SRC_ALPHA)),
                functions.get(GL11.glGetInteger(GL_BLEND_SRC_RGB)));
        System.out.println(
                "Alpha Test Function: " + functions.get(GL11.glGetInteger(GL_ALPHA_TEST_FUNC))
                        + ", Reference: "
                        + GL11.glGetDouble(GL11.GL_ALPHA_TEST_REF));
    }

    private List<String> getChannelDescriptionTooltip(String candidate) {
        List<String> tooltip = new ArrayList<>();
        int maxLine = width - (guiLeft + 151 + 12) - 12;
        for (Entry<String, String> e : ChannelDescription.get(candidate).getDescriptions().entrySet()) {
            tooltip.addAll(
                    fontRendererObj.listFormattedStringToWidth(
                            StatCollector.translateToLocalFormatted(
                                    "item.structurelib.constructableTrigger.gui.channels.from",
                                    Loader.instance().getIndexedModList().get(e.getKey()).getName()),
                            maxLine));
            tooltip.addAll(
                    fontRendererObj.listFormattedStringToWidth(StatCollector.translateToLocal(e.getValue()), maxLine));
        }
        return tooltip;
    }

    public String trim(String str, int keyMaxWidth) {
        Map<String, String> cache = trimCache.get(keyMaxWidth);
        if (cache == null) {
            trimCache.put(keyMaxWidth, cache = new HashMap<>());
        }
        return cache.computeIfAbsent(str, e -> {
            String s = fontRendererObj.trimStringToWidth(e, keyMaxWidth);
            if (s.length() != e.length()) {
                StringBuilder buffer = new StringBuilder(s).deleteCharAt(s.length() - 1).append("...");
                while ((s = fontRendererObj.trimStringToWidth(buffer.toString(), keyMaxWidth)).length()
                        != buffer.length())
                    // drop last original char
                    buffer.deleteCharAt(s.length() - 4);
                do {
                    buffer.append('.');
                } while (fontRendererObj.getStringWidth(buffer.toString()) <= keyMaxWidth);
                s = buffer.deleteCharAt(s.length() - 1).insert(s.length() - 3, EnumChatFormatting.GRAY).toString();
            }
            return s;
        });
    }

    @Override
    @Optional.Method(modid = "NotEnoughItems")
    public VisiblityData modifyVisiblity(GuiContainer gui, VisiblityData currentVisibility) {
        return null;
    }

    @Override
    public Iterable<Integer> getItemSpawnSlots(GuiContainer gui, ItemStack item) {
        return Collections.emptyList();
    }

    @Override
    public List<TaggedInventoryArea> getInventoryAreas(GuiContainer gui) {
        return Collections.emptyList();
    }

    @Override
    public boolean handleDragNDrop(GuiContainer gui, int mousex, int mousey, ItemStack draggedStack, int button) {
        if (button != 0 || mousex <= guiLeft
                || mousey <= guiTop
                || mousex >= guiLeft + getXSize()
                || mousey >= guiTop + getYSize()) {
            return false;
        }
        Collection<Entry<String, Integer>> channels = ChannelDescription.iterate(draggedStack);
        switch (channels.size()) {
            case 0:
                return false;
            case 1:
            default: // TODO implement a GUI to select which pair to use, instead of blindly using the first
                Entry<String, Integer> e = channels.iterator().next();
                ChannelDataAccessor.setChannelData(trigger, e.getKey(), e.getValue());
                key.setText(e.getKey());
                value.setText(e.getValue().toString());
                draggedStack.stackSize = 0;
                return true;
        }
    }

    @Override
    public boolean hideItemPanelSlot(GuiContainer gui, int x, int y, int w, int h) {
        return false;
    }

    private class GuiAutoCompleteList extends GuiScrollableList<String> {

        private List<Entry<String, String>> cache; // <key, display string>

        public GuiAutoCompleteList(int width, int height, int originX, int originY, int slotHeight) {
            super(width, height, originX, originY, slotHeight);
            margin = 2;
        }

        private List<Entry<String, String>> getOptions() {
            if (cache == null) {
                cache = search();
            }
            return cache;
        }

        private List<Entry<String, String>> search() {
            List<Entry<String, String>> cache = new ArrayList<>();
            Set<String> added = new HashSet<>();
            String term = key.getText();
            String[] subjects = ChannelDescription.getAll().keySet().toArray(new String[0]);
            Arrays.sort(subjects);
            if (StringUtils.isBlank(term)) {
                // nothing. just gives out everything
                for (String subject : subjects) {
                    cache.add(new AbstractMap.SimpleImmutableEntry<>(subject, EnumChatFormatting.GRAY + subject));
                }
                return cache;
            }
            // prefix match
            for (String s : subjects) {
                if (s.startsWith(term)) {
                    cache.add(
                            new AbstractMap.SimpleImmutableEntry<>(
                                    s,
                                    term + EnumChatFormatting.GRAY + s.substring(term.length())));
                    added.add(s);
                }
            }
            // subsequence match
            for (String subject : subjects) {
                if (!added.contains(subject) && subject.length() > term.length()) {
                    int i = 0, j = 0;
                    StringBuilder sb = new StringBuilder();
                    boolean mode = true;
                    sb.append(EnumChatFormatting.WHITE);
                    while (i < term.length() && j < subject.length()) {
                        if (term.codePointAt(i) == subject.codePointAt(j)) {
                            if (!mode) {
                                sb.append(EnumChatFormatting.WHITE);
                                mode = true;
                            }
                            sb.appendCodePoint(term.codePointAt(i));
                            i++;
                            j++;
                        } else {
                            if (mode) {
                                sb.append(EnumChatFormatting.GRAY);
                                mode = false;
                            }
                            sb.appendCodePoint(subject.codePointAt(j));
                            j++;
                        }
                    }
                    if (i == term.length()) {
                        if (j < subject.length()) {
                            sb.append(EnumChatFormatting.GRAY);
                            sb.append(subject, j, subject.length());
                        }
                        cache.add(new AbstractMap.SimpleImmutableEntry<>(subject, sb.toString()));
                        added.add(subject);
                    }
                }
            }
            return cache;
        }

        public void reset() {
            cache = search();
            selectedIndex = cache.isEmpty() ? -1 : 0;
            resetScroll();
        }

        @Override
        public String getElementAt(int index) {
            return index >= 0 && index < cache.size() ? getOptions().get(index).getKey() : null;
        }

        @Override
        public int getNumElements() {
            return getOptions().size();
        }

        @Override
        protected void drawElement(int elementIndex, int x, int y, int height, Tessellator tessellator,
                boolean isHovering) {
            int color = elementIndex == selectedIndex ? 0xffffff : 0xcccccc;
            fontRendererObj.drawString(getOptions().get(elementIndex).getValue(), x + margin, y + margin / 2, color);
        }
    }

    private class GuiChannelsList extends GuiScrollableList<Entry<String, Integer>> {

        private List<Entry<String, Integer>> cache;

        public GuiChannelsList(int width, int height, int originX, int originY, int slotHeight) {
            super(width, height, originX, originY, slotHeight);
        }

        @Override
        public Entry<String, Integer> getElementAt(int index) {
            return ChannelDataAccessor.iterateChannelData(trigger).sorted().skip(index).findFirst().orElse(null);
        }

        @Override
        public int getNumElements() {
            return ChannelDataAccessor.countChannelData(trigger);
        }

        @Override
        protected boolean elementClicked(int elementIndex, boolean doubleClick, int mXRelative, int mYRelative) {
            if (mXRelative >= margin + 1 && mXRelative <= margin + 5
                    && mYRelative >= margin / 2 + 1
                    && mYRelative <= margin / 2 + 5) {
                Entry<String, Integer> e = getElementAt(elementIndex);
                if (e != null) {
                    ChannelDataAccessor.unsetChannelData(trigger, e.getKey());
                    updateButtons();
                    return false;
                }
            }
            return super.elementClicked(elementIndex, doubleClick, mXRelative, mYRelative);
        }

        @Override
        protected void prepareDrawElements() {
            cache = ChannelDataAccessor.iterateChannelData(trigger).sorted().collect(Collectors.toList());
        }

        @Override
        protected void drawElement(int elementIndex, int x, int y, int height, Tessellator tessellator,
                boolean isHovering) {
            if (elementIndex < 0 || elementIndex >= cache.size()) {
                return;
            }
            Entry<String, Integer> e = cache.get(elementIndex);
            if (e == null) return;
            if (elementIndex > 0) {
                // args x1, x2, y1, color
                drawHorizontalLine(minX + 1, maxX - 1, y - 2, 0xffaaaaaa);
            }
            int keyMaxWidth = KEY_MAX_WIDTH * 2;
            if (isHovering) {
                mc.renderEngine.bindTexture(new ResourceLocation("structurelib", "textures/gui/channels.png"));
                GL11.glColor4f(1, 1, 1, 1);
                drawTexturedModalRect(x + margin + 1, y + margin / 2 + 1, 251, 251, 5, 5);
            }
            fontRendererObj.drawString(trim(e.getKey(), keyMaxWidth - 9), x + margin + 8, y + margin / 2, 0xffffff);
            int valueX = x + margin + keyMaxWidth + margin;
            fontRendererObj.drawString(trim(e.getValue().toString(), maxX - valueX), valueX, y + margin / 2, 0xffffff);
        }

        @Override
        public void drawScreen(int mX, int mY, float partialTick) {
            super.drawScreen(mX, mY, partialTick);
            // args x1, y1, y2, color
            drawVerticalLine(minX + margin + margin / 2 + KEY_MAX_WIDTH * 2, minY + 1, maxY - 1, 0xffaaaaaa);
        }
    }
}
