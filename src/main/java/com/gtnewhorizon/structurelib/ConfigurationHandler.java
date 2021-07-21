package com.gtnewhorizon.structurelib;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;
import java.util.Map;

public enum ConfigurationHandler {
    INSTANCE;

    private Configuration config;
    private int maxCoexistingHologram;
    private boolean removeCollidingHologram;

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
        maxCoexistingHologram = config.getInt("maxCoexisting", "client.hologram", 1, 1, 100, "An attempt will be made to prune old holograms when a new hologram is about to be projected");
        removeCollidingHologram = config.getBoolean("removeColliding", "client.hologram", true, "An attempt will be made to remove an existing hologram if it collides with a new hologram.");

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

    Configuration getConfig() {
        return config;
    }
}
