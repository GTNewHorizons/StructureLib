package com.gtnewhorizon.structurelib;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.gtnewhorizon.structurelib.structure.FluidAutoplace;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.FMLLaunchHandler;

public enum ConfigurationHandler {

    INSTANCE;

    private Configuration config;
    private int maxCoexistingHologram;
    private boolean removeCollidingHologram;
    private int hintLifespan;
    private int hintTransparency;
    private int autoPlaceBudget;
    private int autoPlaceInterval;
    private boolean fluidAutoplace = true;
    private FluidAutoplace.GateMode fluidGateMode = FluidAutoplace.GateMode.STRICT;
    private Map<String, Pair<List<String>, List<String>>> registryOrders;

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
        fluidAutoplace = config.getBoolean(
                "fluidAutoplace",
                "common.autoplace",
                true,
                "Whether survival autoplace may place fluid blocks, e.g. the water a multiblock needs inside itself, by draining fluid from the player's fluid containers.\n"
                        + "A multiblock can also provide its own fluid source, see IFluidSource.\n"
                        + "This only disables fluid blocks. Multiblocks keep working, they just cannot have those positions filled by autoplace.");
        fluidGateMode = readFluidGateMode();

        loadRegistryOrder();

        saveConfig();
    }

    private FluidAutoplace.GateMode readFluidGateMode() {
        String value = config.getString(
                "fluidGateMode",
                "common.autoplace",
                FluidAutoplace.GateMode.STRICT.name(),
                "How careful autoplace is about placing a fluid into a structure that is not finished yet, as a fluid would flow out of it.\n"
                        + "NONE: place the fluid right away.\n"
                        + "LENIENT: only place the fluid when it cannot flow out of the structure.\n"
                        + "STRICT: only place the fluid once every non fluid element of the structure is satisfied. Note that this makes a fluid at worst one auto place round late.");
        for (FluidAutoplace.GateMode mode : FluidAutoplace.GateMode.values()) {
            if (mode.name().equalsIgnoreCase(value)) return mode;
        }
        StructureLib.LOGGER.warn("Unknown fluidGateMode '{}', falling back to STRICT", value);
        return FluidAutoplace.GateMode.STRICT;
    }

    void loadRegistryOrder() {
        loadRegistryOrderImpl();
        setLanguageKeys();
        saveConfig();
    }

    private void loadRegistryOrderImpl() {
        registryOrders = new HashMap<>();
        for (Map.Entry<String, WeakReference<SortedRegistry<?>>> e : SortedRegistry.ALL_REGISTRIES.entrySet()) {
            SortedRegistry<?> r = e.getValue().get();
            if (r == null) continue;
            String category = "registries." + e.getKey();
            if (FMLLaunchHandler.side().isClient()) {
                // only meaningful on client, and would crash on server
                config.setCategoryConfigEntryClass(category, RegistryOrderEntry.class);
            }
            Property pOrder = config.get(
                    category,
                    "ordering",
                    Iterables.toArray(r.getCurrentOrdering(), String.class),
                    "stuff not in this list will be automatically available after all entries listed here in their natural order, unless explicitly disabled in disabled config below.");
            Property pDisable = config.get(category, "disabled", new String[0], "stuff in this list will be disabled");
            List<String> all = Lists.newArrayList(r.getCurrentOrdering());
            List<String> curVal = new ArrayList<>(Arrays.asList(pOrder.getStringList()));
            List<String> disabled = new ArrayList<>(Arrays.asList(pDisable.getStringList()));
            curVal.removeAll(disabled);
            all.removeAll(disabled);
            curVal.removeIf(s -> !all.remove(s));
            curVal.addAll(all);
            pOrder.set(curVal.toArray(new String[0]));
            registryOrders.put(e.getKey(), Pair.of(curVal, disabled));
        }
        saveConfig();
    }

    private void saveConfig() {
        if (config.hasChanged()) {
            config.save();
            config.load();
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

    /**
     * Whether survival autoplace may place fluid blocks by draining fluid from a fluid source.
     */
    public boolean isFluidAutoplaceEnabled() {
        return fluidAutoplace;
    }

    /**
     * How careful autoplace is about placing a fluid into a structure that is not finished yet.
     */
    public FluidAutoplace.GateMode getFluidGateMode() {
        return fluidGateMode;
    }

    public Pair<List<String>, List<String>> getRegistryOrder(String name) {
        return registryOrders.get(name);
    }

    Configuration getConfig() {
        return config;
    }

}
