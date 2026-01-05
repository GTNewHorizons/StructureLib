package com.gtnewhorizon.structurelib.gui;

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
import java.util.stream.StreamSupport;

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

import com.google.common.base.Splitter;
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
    private static final int INFO_BOX_SIZE = 11;
    private final ItemStack trigger;
    private final GuiChannelsList list;
    private final GuiAutoCompleteList autoCompleteList;
    private final Map<String, List<String>> tooltipSplitCache = new HashMap<>();
    private final TIntObjectMap<Map<String, String>> trimCache = new TIntObjectHashMap<>();
    private GuiTextField key, value;
    protected int guiTop, guiLeft;
    private List<String> info;

    public GuiScreenConfigureChannels(Container container, ItemStack trigger) {
        super(container);

        this.trigger = trigger;
        list = new GuiChannelsList(152, 91, 12, 21, 14);
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

        addButton(new GuiButton(ADD_BTN, guiLeft + 12, guiTop + 157, 47, 20, I18n.format(I18N_PREFIX + "add")));
        addButton(new GuiButton(UNSET_BTN, guiLeft + 65, guiTop + 157, 47, 20, I18n.format(I18N_PREFIX + "unset")));
        addButton(new GuiButton(WIPE_BTN, guiLeft + 118, guiTop + 157, 47, 20, I18n.format(I18N_PREFIX + "wipe")));

        updateButtons();
        tooltipSplitCache.clear();
        info = StreamSupport
                .stream(
                        Splitter.on("\\n").split(StatCollector.translateToLocal(I18N_PREFIX + "info")).spliterator(),
                        false)
                .flatMap(line -> fontRendererObj.listFormattedStringToWidth(line, width * 1 / 3).stream())
                .collect(Collectors.toList());
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

    public List<GuiButton> getButtonList() {
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
                    if (autoCompleteList.getSelectedElement() != null) {
                        key.setText(autoCompleteList.getSelectedElement());
                        return;
                    }

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
                    value.setText("1");
                    value.setSelectionPos(0);
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
        if (key.textboxKeyTyped(aChar, aKey) || value.textboxKeyTyped(aChar, aKey)) {
            updateButtons();
            return;
        }
        super.keyTyped(aChar, aKey);
    }

    private void updateButtons() {
        // this will be called from setText of key and value. NEVER UPDATE THE VALUE OF THESE HERE OR GET A
        // STACKOVERFLOW!
        String keyText = key.getText();
        boolean existing = !StringUtils.isEmpty(keyText) && ChannelDataAccessor.hasSubChannel(trigger, keyText);
        getButtonList().get(ADD_BTN).displayString = existing ? I18n.format(I18N_PREFIX + "set")
                : I18n.format(I18N_PREFIX + "add");
        getButtonList().get(ADD_BTN).enabled = !StringUtils.isBlank(key.getText())
                && !StringUtils.isBlank(value.getText())
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
        fontRendererObj.drawString(I18n.format(I18N_PREFIX + "title"), guiLeft + 12, guiTop + 9, 0);
        fontRendererObj.drawString(I18n.format(I18N_PREFIX + "key"), guiLeft + 12, guiTop + 122, 0);
        key.drawTextBox();
        fontRendererObj.drawString(I18n.format(I18N_PREFIX + "value"), guiLeft + 12, guiTop + 142, 0);
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
        if (mouseX > xSize + guiLeft - INFO_BOX_SIZE - 12 && mouseX < xSize + guiLeft - 12
                && mouseY > guiTop + 7
                && mouseY < guiTop + INFO_BOX_SIZE + 7) {
            drawHoveringText(info, mouseX, mouseY, fontRendererObj);
        }
    }

    private void drawChannelDescriptionTooltip(String candidate) {
        List<String> tooltip = tooltipSplitCache.computeIfAbsent(candidate, this::getChannelDescriptionTooltip);
        drawHoveringText(tooltip, guiLeft + 151 + 12, guiTop + 122, fontRendererObj);
    }

    private List<String> getChannelDescriptionTooltip(String candidate) {
        List<String> tooltip = new ArrayList<>();
        int maxLine = width - (guiLeft + 151 + 12) - 12;
        for (Entry<String, String> e : ChannelDescription.get(candidate).getDescriptions().entrySet()) {
            tooltip.addAll(
                    fontRendererObj.listFormattedStringToWidth(
                            StatCollector.translateToLocalFormatted(
                                    I18N_PREFIX + "channels.from",
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
