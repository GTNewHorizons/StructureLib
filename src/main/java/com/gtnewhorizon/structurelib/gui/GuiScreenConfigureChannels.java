package com.gtnewhorizon.structurelib.gui;

import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.gui.GuiScrollableList.IGuiScreen;

public class GuiScreenConfigureChannels extends GuiScreen implements IGuiScreen {

    private static final int KEY_MAX_WIDTH = 50;
    private final ItemStack trigger;
    private final GuiChannelsList list;
    private GuiTextField key, value;
    protected int guiTop, guiLeft;

    public GuiScreenConfigureChannels(ItemStack trigger) {
        this.trigger = trigger;
        list = new GuiChannelsList(152, 100, 12, 12, 14);
        list.addSelectionListener((list, selectedIndex) -> {
            Entry<String, Integer> e = list.getElementAt(selectedIndex);
            key.setText(e.getKey());
            value.setText(e.getValue().toString());
            updateButtons();
        });
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

        addButton(
                new GuiButton(
                        0,
                        guiLeft + 10,
                        guiTop + 157,
                        35,
                        20,
                        I18n.format("item.structurelib.constructableTrigger.gui.add")));
        addButton(
                new GuiButton(
                        1,
                        guiLeft + 50,
                        guiTop + 157,
                        35,
                        20,
                        I18n.format("item.structurelib.constructableTrigger.gui.unset")));
        addButton(
                new GuiButton(
                        2,
                        guiLeft + 90,
                        guiTop + 157,
                        35,
                        20,
                        I18n.format("item.structurelib.constructableTrigger.gui.wipe")));
        addButton(
                new GuiButton(
                        3,
                        guiLeft + 130,
                        guiTop + 157,
                        35,
                        20,
                        I18n.format("item.structurelib.constructableTrigger.gui.no_hatch")));

        updateButtons();
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

    @Override
    public void handleMouseInput() {
        int delta = Mouse.getEventDWheel();
        if (delta != 0) list.handleDWheel(delta);
        super.handleMouseInput();
    }

    @Override
    protected void mouseClicked(int mX, int mY, int button) {
        key.mouseClicked(mX, mY, button);
        value.mouseClicked(mX, mY, button);
        super.mouseClicked(mX, mY, button);
    }

    @Override
    protected void keyTyped(char aChar, int aKey) {
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
                GuiButton add = getButtonList().get(0);
                if (add.enabled) doActionPerformed(add);
                return;
            case Keyboard.KEY_UP:
                if (list.selectedIndex > 0) list.setSelection(list.selectedIndex - 1);
                return;
            case Keyboard.KEY_DOWN:
                if (list.selectedIndex < list.getNumElements() - 1) list.setSelection(list.selectedIndex + 1);
                return;
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
        getButtonList().get(0).displayString = existing ? I18n.format("item.structurelib.constructableTrigger.gui.set")
                : I18n.format("item.structurelib.constructableTrigger.gui.add");
        getButtonList().get(0).enabled = !StringUtils.isBlank(value.getText());
        getButtonList().get(1).enabled = existing && !StringUtils.isBlank(value.getText());
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
            case 0:
                int value = getValue();
                if (value <= 0) return;
                ChannelDataAccessor.setChannelData(trigger, key.getText(), value);
                break;
            case 1:
                ChannelDataAccessor.unsetChannelData(trigger, key.getText());
                break;
            case 2:
                ChannelDataAccessor.wipeChannelData(trigger);
                break;
            case 3:
                ChannelDataAccessor.hasHatchPlacing(trigger);
                break;
        }
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
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mX, int mY, float partialTick) {
        mc.renderEngine.bindTexture(new ResourceLocation("structurelib", "textures/gui/channels.png"));
        int topLeftX = (this.width - this.getXSize()) / 2;
        int topLeftY = (this.height - this.getYSize()) / 2;
        drawTexturedModalRect(topLeftX, topLeftY, 0, 0, getXSize(), getYSize());
        super.drawScreen(mX, mY, partialTick);
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

        public String trim(String e, int keyMaxWidth) {
            // TODO optimize this to not create a billion string, or maybe cache this
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
        }
    }
}
