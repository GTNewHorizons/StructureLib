package com.gtnewhorizon.structurelib.client.nei;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.Tags;
import com.gtnewhorizon.structurelib.client.nei.integration.gregtech.GT_NEI_MultiblockHandler;
import com.gtnewhorizon.structurelib.client.nei.integration.structurelib.StructureCompatNEIHandler;

import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.TemplateRecipeHandler;

@SuppressWarnings("unused")
public class NEI_Config implements IConfigureNEI {

    public static boolean isAdded = true;

    @Override
    public void loadConfig() {
        isAdded = false;
        TemplateRecipeHandler handler = new StructureCompatNEIHandler();
        GuiCraftingRecipe.craftinghandlers.add(handler);
        GuiUsageRecipe.usagehandlers.add(handler);

        if (StructureLib.isGTLoaded) {
            handler = new GT_NEI_MultiblockHandler();
            GuiCraftingRecipe.craftinghandlers.add(handler);
            GuiUsageRecipe.usagehandlers.add(handler);
        }
        isAdded = true;
    }

    @Override
    public String getName() {
        return StructureLibAPI.MOD_ID + " NEI Plugin";
    }

    @Override
    public String getVersion() {
        return Tags.VERSION;
    }
}
