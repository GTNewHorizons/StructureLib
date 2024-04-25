package com.gtnewhorizon.structurelib;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.GuiConfigEntries;
import cpw.mods.fml.client.config.IConfigElement;

@SuppressWarnings({ "unchecked" })
public final class RegistryOrderEntry extends GuiConfigEntries.CategoryEntry {

    public RegistryOrderEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList,
            IConfigElement<?> configElement) {
        super(owningScreen, owningEntryList, configElement);
    }

    @Override
    protected GuiScreen buildChildScreen() {
        List<IConfigElement<?>> childElements = this.configElement.getChildElements();
        IConfigElement<?> disabled = null, order = null;
        for (IConfigElement<?> e : childElements) {
            if (e.getName().equals("disabled")) disabled = e;
            else if (e.getName().equals("ordering")) order = e;
        }
        if (disabled == null || order == null) {
            throw new IllegalArgumentException("Did not find expected config entires");
        }
        return new GuiEditRegistryConfig(
                this.owningScreen,
                disabled,
                order,
                configElement.getName(),
                this.owningScreen.modID,
                null,
                owningScreen.allRequireWorldRestart || this.configElement.requiresWorldRestart(),
                owningScreen.allRequireMcRestart || this.configElement.requiresMcRestart(),
                this.owningScreen.title,
                ((this.owningScreen.titleLine2 == null ? "" : this.owningScreen.titleLine2) + " > " + this.name));
    }

    private GuiEditRegistryConfig getChildScreen() {
        return (GuiEditRegistryConfig) childScreen;
    }

    @Override
    public boolean saveConfigElement() {
        GuiEditRegistryConfig c = getChildScreen();
        if (c != null && c.isInitialized()) return c.saveConfigElements();
        return false;
    }

    @Override
    public boolean isChanged() {
        GuiEditRegistryConfig c = getChildScreen();
        return c != null && c.isInitialized() && c.isChanged();
    }

    @Override
    public boolean isDefault() {
        GuiEditRegistryConfig c = getChildScreen();
        return c != null && c.isInitialized() && c.isDefault();
    }

    @Override
    public void setToDefault() {
        GuiEditRegistryConfig c = getChildScreen();
        if (c != null && c.isInitialized()) c.setToDefault();
    }

    @Override
    public void undoChanges() {
        GuiEditRegistryConfig c = getChildScreen();
        if (c != null && c.isInitialized()) c.undoChanges();
    }
}
