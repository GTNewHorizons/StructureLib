package com.gtnewhorizon.structurelib.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.structurelib.ChannelDescription;
import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;

import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import cpw.mods.fml.common.Optional;

/** General channel browser. All edits are staged until Apply is pressed. */
@Optional.Interface(iface = "codechicken.nei.api.INEIGuiHandler", modid = "NotEnoughItems")
public class GuiScreenConfigureChannels extends GuiContainer implements INEIGuiHandler {

    private static final String PREFIX = "item.structurelib.constructableTrigger.gui.browser.";
    private static final int APPLY = 0, CANCEL = 1, ADVANCED = 2, RESET_ALL = 3, RESET = 4, MINUS = 5, PLUS = 6,
            ADD = 7;
    private static final int ROW = 26;
    private final ItemStack trigger;
    private final ChannelSettings draft;
    private final Map<String, List<Entry<ItemStack, Integer>>> itemCache = new HashMap<>();
    private GuiTextField search, itemSearch, number, custom;
    private List<String> channels = new ArrayList<>();
    private List<Entry<ItemStack, Integer>> variants = new ArrayList<>();
    private List<Entry<String, Integer>> dropped = new ArrayList<>();
    private String selected;
    private int leftWidth, channelScroll, itemScroll, focusedOption;
    private boolean advanced;
    private boolean numberEdited;
    private long resetDeadline;
    private List<String> hoverText;

    public GuiScreenConfigureChannels(Container container, ItemStack trigger) {
        super(container);
        this.trigger = trigger;
        Map<String, Integer> initial = new HashMap<>();
        ChannelDataAccessor.iterateChannelData(trigger).forEach(e -> initial.put(e.getKey(), e.getValue()));
        draft = new ChannelSettings(initial);
    }

    private String tr(String key, Object... args) {
        return I18n.format(PREFIX + key, args);
    }

    @Override
    public void initGui() {
        String oldSearch = search == null ? "" : search.getText();
        String oldItems = itemSearch == null ? "" : itemSearch.getText();
        String oldCustom = custom == null ? "" : custom.getText();
        String oldNumber = number == null ? null : number.getText();
        boolean wasNumberEdited = numberEdited;
        // Leave room for NEI beside the browser whenever the screen is wide enough.
        xSize = Math.min(width - 12, Math.min(440, Math.max(308, width - 144)));
        ySize = Math.min(300, height - 12);
        leftWidth = Math.max(112, xSize * 2 / 5);
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        search = field(12, 34, leftWidth - 20, oldSearch);
        itemSearch = field(leftWidth + 12, 66, xSize - leftWidth - 24, oldItems);
        number = field(leftWidth + 36, ySize - 76, xSize - leftWidth - 72, "");
        number.setMaxStringLength(10);
        custom = field(12, ySize - 76, leftWidth - 20, oldCustom);
        custom.setMaxStringLength(128);
        buttonList.clear();
        button(APPLY, xSize - 80, ySize - 26, 68, tr("apply"));
        button(CANCEL, xSize - 154, ySize - 26, 68, tr("cancel"));
        button(ADVANCED, Math.max(12, 58 - guiLeft), ySize - 26, 84, tr("advanced"));
        button(RESET_ALL, 12, ySize - 50, leftWidth - 20, tr("reset_all"));
        button(RESET, leftWidth + 12, ySize - 50, xSize - leftWidth - 24, tr("default"));
        button(MINUS, leftWidth + 12, ySize - 80, 20, "-");
        button(PLUS, xSize - 32, ySize - 80, 20, "+");
        button(ADD, 12, ySize - 100, leftWidth - 20, tr("custom"));
        refreshChannels();
        if (selected == null && !channels.isEmpty()) selected = channels.get(0);
        syncNumber();
        if (oldNumber != null) {
            number.setText(oldNumber);
            numberEdited = wasNumberEdited;
        }
        refreshVariants();
        updateControls();
    }

    private GuiTextField field(int x, int y, int w, String text) {
        GuiTextField field = new GuiTextField(fontRendererObj, guiLeft + x, guiTop + y, w, 14);
        field.setMaxStringLength(128);
        field.setText(text);
        return field;
    }

    private void button(int id, int x, int y, int w, String text) {
        buttonList.add(new GuiButton(id, guiLeft + x, guiTop + y, w, 20, text));
    }

    private GuiButton button(int id) {
        return (GuiButton) buttonList.get(id);
    }

    private String label(String channel) {
        String translation = PREFIX + "channel." + channel;
        if (StatCollector.canTranslate(translation)) return StatCollector.translateToLocal(translation);
        String readable = channel.replace('_', ' ');
        return readable.isEmpty() ? channel : readable.substring(0, 1).toUpperCase(Locale.ROOT) + readable.substring(1);
    }

    private List<Entry<ItemStack, Integer>> items(String channel) {
        if (channel == null || !ChannelDescription.has(channel)) return Collections.emptyList();
        return itemCache.computeIfAbsent(
                channel,
                key -> ChannelDescription.get(key).getItems().entrySet().stream()
                        .filter(
                                e -> e.getKey() != null && e.getKey().getItem() != null
                                        && e.getValue() > 0
                                        && e.getValue() <= ChannelSettings.MAX_VALUE)
                        .sorted(
                                Comparator.comparingInt((Entry<ItemStack, Integer> e) -> e.getValue())
                                        .thenComparing(e -> e.getKey().getDisplayName())
                                        .thenComparing(e -> String.valueOf(e.getKey().getItemDamage())))
                        .collect(Collectors.toList()));
    }

    private String valueLabel(String channel) {
        Integer value = draft.get(channel);
        if (value == null) return tr("default");
        if (value <= 0 || value > ChannelSettings.MAX_VALUE) return tr("invalid_value", value);
        List<Entry<ItemStack, Integer>> matches = items(channel).stream().filter(e -> e.getValue().equals(value))
                .collect(Collectors.toList());
        if (matches.isEmpty()) return value.toString();
        return matches.get(0).getKey().getDisplayName()
                + (matches.size() > 1 ? " (+" + (matches.size() - 1) + ")" : "");
    }

    private void refreshChannels() {
        String term = search.getText().trim().toLowerCase(Locale.ROOT);
        channels = draft.channels(ChannelDescription.getAll().keySet()).stream().filter(
                key -> key.toLowerCase(Locale.ROOT).contains(term) || label(key).toLowerCase(Locale.ROOT).contains(term)
                        || valueLabel(key).toLowerCase(Locale.ROOT).contains(term))
                .collect(Collectors.toList());
        channelScroll = clamp(channelScroll, channels.size(), channelRows());
    }

    private void refreshVariants() {
        String term = itemSearch.getText().trim().toLowerCase(Locale.ROOT);
        variants = items(selected).stream()
                .filter(e -> e.getKey().getDisplayName().toLowerCase(Locale.ROOT).contains(term))
                .collect(Collectors.toList());
        itemScroll = clamp(itemScroll, optionCount(), itemRows());
        focusedOption = Math.max(0, Math.min(focusedOption, optionCount() - 1));
    }

    private int channelRows() {
        return Math.max(1, ((advanced ? ySize - 106 : ySize - 56) - 56) / ROW);
    }

    private int itemRows() {
        return Math.max(1, (ySize - (numericVisible() ? 100 : 84) - 88) / ROW);
    }

    private int optionCount() {
        return dropped.isEmpty() ? variants.size() : dropped.size();
    }

    private int clamp(int offset, int count, int rows) {
        return Math.max(0, Math.min(offset, Math.max(0, count - rows)));
    }

    private boolean numericVisible() {
        return selected != null && (advanced || items(selected).isEmpty());
    }

    private void syncNumber() {
        Integer value = draft.get(selected);
        number.setText(value == null ? "" : value.toString());
        numberEdited = false;
    }

    private void select(String channel) {
        selected = channel;
        dropped.clear();
        itemSearch.setText("");
        itemScroll = 0;
        focusedOption = 0;
        syncNumber();
        refreshVariants();
        updateControls();
    }

    private void updateControls() {
        boolean numeric = numericVisible();
        number.setVisible(numeric);
        custom.setVisible(advanced);
        itemSearch.setVisible(selected != null && !items(selected).isEmpty() && dropped.isEmpty());
        for (GuiTextField field : Arrays.asList(itemSearch, number, custom)) {
            if (!field.getVisible()) field.setFocused(false);
        }
        button(ADD).visible = advanced;
        button(ADD).enabled = !custom.getText().trim().isEmpty();
        button(MINUS).visible = button(PLUS).visible = numeric;
        Integer parsed = ChannelSettings.parse(number.getText());
        boolean valid = !numeric || !numberEdited && number.getText().isEmpty() || parsed != null;
        button(APPLY).enabled = valid && draft.isValid();
        button(MINUS).enabled = parsed != null && parsed > 1;
        button(PLUS).enabled = number.getText().isEmpty() || parsed != null && parsed < ChannelSettings.MAX_VALUE;
        button(RESET).enabled = selected != null && draft.get(selected) != null;
        button(RESET_ALL).enabled = !draft.snapshot().isEmpty();
        button(ADVANCED).displayString = tr(advanced ? "simple" : "advanced");
        button(RESET_ALL).displayString = tr(
                resetDeadline > System.currentTimeMillis() ? "confirm_reset" : "reset_all");
    }

    @Override
    protected void actionPerformed(GuiButton btn) {
        if (btn == null || !btn.enabled) return;
        switch (btn.id) {
            case APPLY:
                ChannelDataAccessor.wipeChannelData(trigger);
                draft.snapshot().forEach((key, value) -> ChannelDataAccessor.setChannelData(trigger, key, value));
                if (trigger.hasTagCompound() && trigger.getTagCompound().hasNoTags()) trigger.setTagCompound(null);
                StructureLib.instance().proxy().uploadChannels(trigger);
                mc.thePlayer.closeScreen();
                return;
            case CANCEL:
                mc.thePlayer.closeScreen();
                return;
            case ADVANCED:
                advanced = !advanced;
                custom.setFocused(false);
                number.setFocused(false);
                syncNumber();
                break;
            case RESET_ALL:
                if (resetDeadline > System.currentTimeMillis()) {
                    draft.resetAll();
                    resetDeadline = 0;
                    syncNumber();
                } else resetDeadline = System.currentTimeMillis() + 3000;
                break;
            case RESET:
                draft.reset(selected);
                syncNumber();
                break;
            case MINUS:
            case PLUS:
                Integer current = ChannelSettings.parse(number.getText());
                int next = current == null ? 1 : current + (btn.id == PLUS ? 1 : -1);
                draft.set(selected, Integer.toString(next));
                syncNumber();
                break;
            case ADD:
                String name = custom.getText().trim().toLowerCase(Locale.ROOT);
                if (draft.get(name) == null) draft.set(name, "1");
                select(name);
                search.setText("");
                custom.setText("");
                break;
            default:
                break;
        }
        refreshChannels();
        updateControls();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        for (GuiTextField field : Arrays.asList(search, itemSearch, number, custom)) field.updateCursorCounter();
        updateControls();
    }

    private boolean inside(int x, int y, int left, int top, int w, int h) {
        return x >= left && x < left + w && y >= top && y < top + h;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiTextField field : Arrays.asList(search, itemSearch, number, custom)) {
            if (field.getVisible()) field.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (mouseButton != 0) return;
        int x = mouseX - guiLeft, y = mouseY - guiTop;
        if (inside(x, y, 12, 56, leftWidth - 20, channelRows() * ROW)) {
            int index = channelScroll + (y - 56) / ROW;
            if (index < channels.size()) select(channels.get(index));
        } else if (inside(x, y, leftWidth + 12, 88, xSize - leftWidth - 24, itemRows() * ROW)) {
            int index = itemScroll + (y - 88) / ROW;
            chooseOption(index);
        }
    }

    private void chooseOption(int index) {
        if (index < 0 || index >= optionCount()) return;
        focusedOption = index;
        if (!dropped.isEmpty()) {
            Entry<String, Integer> mapping = dropped.get(index);
            draft.set(mapping.getKey(), mapping.getValue().toString());
            select(mapping.getKey());
        } else {
            draft.set(selected, variants.get(index).getValue().toString());
            syncNumber();
        }
        refreshChannels();
        updateControls();
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta == 0) return;
        int x = Mouse.getEventX() * width / mc.displayWidth - guiLeft;
        int y = height - Mouse.getEventY() * height / mc.displayHeight - 1 - guiTop;
        int step = delta > 0 ? -1 : 1;
        if (inside(x, y, 12, 56, leftWidth - 20, channelRows() * ROW)) {
            channelScroll = clamp(channelScroll + step, channels.size(), channelRows());
        } else if (inside(x, y, leftWidth + 12, 88, xSize - leftWidth - 24, itemRows() * ROW)) {
            itemScroll = clamp(itemScroll + step, optionCount(), itemRows());
        }
    }

    @Override
    protected void keyTyped(char character, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE
                || keyCode == mc.gameSettings.keyBindInventory.getKeyCode() && !search.isFocused()
                        && !itemSearch.isFocused()
                        && !number.isFocused()
                        && !custom.isFocused()) {
            mc.thePlayer.closeScreen();
            return;
        }
        if (keyCode == Keyboard.KEY_TAB) {
            List<GuiTextField> fields = Arrays.asList(search, itemSearch, number, custom).stream()
                    .filter(GuiTextField::getVisible).collect(Collectors.toList());
            int focused = -1;
            for (int i = 0; i < fields.size(); i++) if (fields.get(i).isFocused()) focused = i;
            for (GuiTextField field : fields) field.setFocused(false);
            fields.get(Math.floorMod(focused + (isShiftKeyDown() ? -1 : 1), fields.size())).setFocused(true);
            return;
        }
        if (search.textboxKeyTyped(character, keyCode)) {
            channelScroll = 0;
            refreshChannels();
        } else if (itemSearch.getVisible() && itemSearch.textboxKeyTyped(character, keyCode)) {
            itemScroll = 0;
            focusedOption = 0;
            refreshVariants();
        } else if (editNumber(character, keyCode)) {
            refreshChannels();
        } else if (custom.getVisible() && custom.textboxKeyTyped(character, keyCode)) {
            // A custom channel is only added with the explicit button or Enter.
        } else if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            if (custom.isFocused() && advanced) actionPerformed(button(ADD));
            else if (itemSearch.isFocused() || !dropped.isEmpty()) chooseOption(focusedOption);
            else if (search.isFocused() && !channels.isEmpty()) {
                select(channels.contains(selected) ? selected : channels.get(channelScroll));
            } else actionPerformed(button(APPLY));
        } else if (keyCode == Keyboard.KEY_UP || keyCode == Keyboard.KEY_DOWN) {
            int direction = keyCode == Keyboard.KEY_UP ? -1 : 1;
            if (itemSearch.isFocused() || !dropped.isEmpty()) {
                focusedOption = Math.max(0, Math.min(optionCount() - 1, focusedOption + direction));
                if (focusedOption < itemScroll) itemScroll = focusedOption;
                if (focusedOption >= itemScroll + itemRows()) itemScroll = focusedOption - itemRows() + 1;
            } else if (!channels.isEmpty()) {
                int index = Math.max(0, Math.min(channels.size() - 1, channels.indexOf(selected) + direction));
                select(channels.get(index));
                if (index < channelScroll) channelScroll = index;
                if (index >= channelScroll + channelRows()) channelScroll = index - channelRows() + 1;
            }
        }
        updateControls();
    }

    private boolean editNumber(char character, int keyCode) {
        if (!number.getVisible()) return false;
        String before = number.getText();
        if (!number.textboxKeyTyped(character, keyCode)) return false;
        if (!before.equals(number.getText())) {
            numberEdited = true;
            draft.set(selected, number.getText());
        }
        return true;
    }

    private void text(String text, int x, int y, int w, int color) {
        String shown = fontRendererObj.trimStringToWidth(text, w);
        if (!shown.equals(text)) shown = fontRendererObj.trimStringToWidth(text, Math.max(0, w - 9)) + "...";
        fontRendererObj.drawString(shown, guiLeft + x, guiTop + y, color);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTick, int mouseX, int mouseY) {
        hoverText = null;
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xffb6b6b6);
        drawRect(guiLeft + 2, guiTop + 2, guiLeft + xSize - 2, guiTop + ySize - 2, 0xff292d32);
        drawRect(guiLeft + leftWidth, guiTop + 30, guiLeft + leftWidth + 1, guiTop + ySize - 32, 0xff626971);
        text(tr("title"), 12, 10, xSize - 24, 0xffffff);
        text(tr("settings"), 12, 23, leftWidth - 20, 0xaeb9c5);
        search.drawTextBox();
        int mx = mouseX - guiLeft, my = mouseY - guiTop;
        for (int row = 0; row < channelRows() && channelScroll + row < channels.size(); row++) {
            String channel = channels.get(channelScroll + row);
            int y = 56 + row * ROW;
            if (channel.equals(selected))
                drawRect(guiLeft + 10, guiTop + y - 2, guiLeft + leftWidth - 6, guiTop + y + ROW - 2, 0xff42576b);
            text(label(channel), 14, y, leftWidth - 26, 0xffffff);
            text(valueLabel(channel), 14, y + 12, leftWidth - 26, draft.get(channel) == null ? 0x9aa7b2 : 0xa9dcaa);
            if (inside(mx, my, 12, y, leftWidth - 20, ROW)) {
                hoverText = Arrays.asList(label(channel), channel, valueLabel(channel));
            }
        }
        if (channels.isEmpty()) text(tr("empty"), 12, 58, leftWidth - 20, 0xaeb9c5);
        scrollMark(channelScroll, channels.size(), channelRows(), leftWidth - 5, 56);
        int right = leftWidth + 12, rightWidth = xSize - leftWidth - 24;
        text(selected == null ? tr("select") : label(selected), right, 34, rightWidth, 0xffffff);
        String summary = selected == null ? "" : tr(items(selected).isEmpty() ? "numeric" : "variants");
        if (selected != null && ChannelDescription.has(selected)) {
            summary = ChannelDescription.get(selected).getDescriptions().values().stream().sorted()
                    .map(StatCollector::translateToLocal).findFirst().orElse(summary);
        }
        text(dropped.isEmpty() ? summary : tr("choose_channel"), right, 50, rightWidth, 0xaeb9c5);
        if (inside(mx, my, right, 32, rightWidth, 30) && selected != null) {
            hoverText = new ArrayList<>();
            hoverText.add(label(selected));
            if (ChannelDescription.has(selected)) ChannelDescription.get(selected).getDescriptions().values().forEach(
                    description -> hoverText.addAll(
                            fontRendererObj.listFormattedStringToWidth(
                                    StatCollector.translateToLocal(description),
                                    Math.min(260, width - 32))));
            hoverText.addAll(fontRendererObj.listFormattedStringToWidth(tr("default_help"), Math.min(260, width - 32)));
        }
        itemSearch.drawTextBox();
        if (itemSearch.getVisible() && itemSearch.getText().isEmpty() && !itemSearch.isFocused()) {
            text(tr("registered"), right + 4, 69, rightWidth - 8, 0x808890);
        }
        for (int row = 0; row < itemRows() && itemScroll + row < optionCount(); row++) {
            int index = itemScroll + row, y = 88 + row * ROW;
            if (index == focusedOption && (itemSearch.isFocused() || !dropped.isEmpty())) {
                drawRect(guiLeft + right - 3, guiTop + y - 2, guiLeft + right - 1, guiTop + y + ROW - 2, 0xffd4e6f5);
            }
            if (!dropped.isEmpty()) {
                Entry<String, Integer> mapping = dropped.get(index);
                text(label(mapping.getKey()), right, y, rightWidth, 0xffffff);
                text(mapping.getValue().toString(), right, y + 12, rightWidth, 0xa9dcaa);
            } else {
                Entry<ItemStack, Integer> variant = variants.get(index);
                if (variant.getValue().equals(draft.get(selected))) drawRect(
                        guiLeft + right - 2,
                        guiTop + y - 2,
                        guiLeft + xSize - 10,
                        guiTop + y + ROW - 2,
                        0xff42576b);
                RenderHelper.enableGUIStandardItemLighting();
                itemRender.renderItemAndEffectIntoGUI(
                        fontRendererObj,
                        mc.getTextureManager(),
                        variant.getKey(),
                        guiLeft + right,
                        guiTop + y + 2);
                RenderHelper.disableStandardItemLighting();
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                text(variant.getKey().getDisplayName(), right + 22, y + 4, rightWidth - 26, 0xffffff);
                if (inside(mx, my, right, y, rightWidth, ROW)) {
                    hoverText = new ArrayList<>(
                            variant.getKey().getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips));
                    hoverText.add(tr("raw", variant.getValue()));
                }
            }
        }
        scrollMark(itemScroll, optionCount(), itemRows(), xSize - 7, 88);
        if (optionCount() == 0 && selected != null) {
            text(tr(items(selected).isEmpty() ? "numeric" : "empty"), right, 90, rightWidth, 0xaeb9c5);
        }
        if (numericVisible()) {
            number.drawTextBox();
            if (numberEdited && ChannelSettings.parse(number.getText()) == null) {
                text(tr("invalid"), right, ySize - 92, rightWidth, 0xff9999);
            }
        }
        custom.drawTextBox();
        if (inside(mx, my, right, ySize - 50, rightWidth, 20)) {
            hoverText = fontRendererObj.listFormattedStringToWidth(tr("default_help"), Math.min(260, width - 32));
        }
        if (inside(mx, my, 12, 8, xSize - 24, 13)) {
            hoverText = fontRendererObj.listFormattedStringToWidth(tr("help"), Math.min(260, width - 32));
        }
        if (!draft.isValid() && inside(mx, my, xSize - 80, ySize - 26, 68, 20)) {
            hoverText = fontRendererObj.listFormattedStringToWidth(tr("invalid_settings"), Math.min(260, width - 32));
        }
    }

    private void scrollMark(int offset, int total, int rows, int x, int y) {
        if (total <= rows) return;
        int height = rows * ROW;
        int thumb = Math.max(8, height * rows / total);
        int top = y + (height - thumb) * offset / (total - rows);
        drawRect(guiLeft + x, guiTop + y, guiLeft + x + 2, guiTop + y + height, 0xff17191c);
        drawRect(guiLeft + x, guiTop + top, guiLeft + x + 2, guiTop + top + thumb, 0xff9aafc2);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (hoverText != null) drawHoveringText(hoverText, mouseX, mouseY, fontRendererObj);
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
        if (button != 0 || !inside(mousex, mousey, guiLeft, guiTop, xSize, ySize)) return false;
        List<Entry<String, Integer>> mappings = ChannelDescription.iterate(draggedStack).stream()
                .filter(e -> e.getValue() > 0 && e.getValue() <= ChannelSettings.MAX_VALUE)
                .sorted(Entry.comparingByKey()).collect(Collectors.toList());
        if (mappings.isEmpty()) return false;
        if (mappings.size() == 1) {
            Entry<String, Integer> mapping = mappings.get(0);
            draft.set(mapping.getKey(), mapping.getValue().toString());
            select(mapping.getKey());
        } else {
            dropped = mappings;
            itemScroll = 0;
            focusedOption = 0;
        }
        refreshChannels();
        updateControls();
        draggedStack.stackSize = 0;
        return true;
    }

    @Override
    public boolean hideItemPanelSlot(GuiContainer gui, int x, int y, int w, int h) {
        return false;
    }
}
