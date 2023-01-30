package com.gtnewhorizon.structurelib;

import java.io.File;
import java.util.Map;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public enum ConfigurationHandler {

    INSTANCE;

    private Configuration config;
    private int maxCoexistingHologram;
    private boolean removeCollidingHologram;
    private int hintLifespan;
    private int hintTransparency;
    private int autoPlaceBudget;
    private int autoPlaceInterval;

    ConfigurationHandler() {
        FMLCommonHandler.instance().bus().register(this);
    }

    void init(File f) {
        config = new Configuration(f, ConfigurationVersion.latest().getVersionMarker());
        ConfigurationVersion.migrateToLatest(config);
        loadConfig();
        setLanguageKeys();
    }

    private void setLanguageKeys() {
        for (String categoryName : config.getCategoryNames()) {
            ConfigCategory category = config.getCategory(categoryName);
            category.setLanguageKey("structurelib.config." + categoryName);
            for (Map.Entry<String, Property> entry : category.entrySet()) {
                entry.getValue().setLanguageKey(String.format("%s.%s", category.getLanguagekey(), entry.getKey()));
            }
        }
    }

    private void loadConfig() {
        maxCoexistingHologram = config.getInt(
                "maxCoexisting",
                "client.hologram",
                1,
                1,
                100,
                "An attempt will be made to prune old holograms when a new hologram is about to be projected");
        removeCollidingHologram = config.getBoolean(
                "removeColliding",
                "client.hologram",
                true,
                "An attempt will be made to remove an existing hologram if it collides with a new hologram.");
        hintLifespan = config
                .getInt("hintLifespan", "client.hologram", 400, 1, 20000, "Ticks before a hologram disappears.");
        hintTransparency = config.getInt(
                "hintTransparency",
                "client.hologram",
                192,
                1,
                255,
                "Alpha value of hologram particles. Higher the value, the more \"ghostly\" the hologram will appear to be.");
        autoPlaceBudget = config.getInt(
                "autoPlaceBudget",
                "common.hologram",
                25,
                1,
                200,
                "Max number of elements can be placed in one round of auto place.\n"
                        + "As expected, server side settings will overrides client settings.\n"
                        + "Certain larger multi might increase these values beyond this configured value.");
        autoPlaceInterval = config.getInt(
                "autoPlaceInterval",
                "common.hologram",
                300,
                0,
                20000,
                "Unit: millisecond. Minimal interval between two auto place round.\n"
                        + "As expected, server side settings will overrides client settings.\n"
                        + "Note this relates to the wall clock, not in game ticks.\n"
                        + "Value smaller than default is likely to be perceived as no minimal interval whatsoever.");

        if (config.hasChanged()) {
            config.save();
        }
    }

    @SubscribeEvent
    public void onConfigChange(ConfigChangedEvent.PostConfigChangedEvent e) {
        if (e.modID.equals(StructureLibAPI.MOD_ID)) {
            loadConfig();
        }
    }

    public int getMaxCoexistingHologram() {
        return maxCoexistingHologram;
    }

    public boolean isRemoveCollidingHologram() {
        return removeCollidingHologram;
    }

    public int getHintLifespan() {
        return hintLifespan;
    }

    public int getHintTransparency() {
        return hintTransparency;
    }

    public int getAutoPlaceBudget() {
        return autoPlaceBudget;
    }

    public int getAutoPlaceInterval() {
        return autoPlaceInterval;
    }

    Configuration getConfig() {
        return config;
    }
}
