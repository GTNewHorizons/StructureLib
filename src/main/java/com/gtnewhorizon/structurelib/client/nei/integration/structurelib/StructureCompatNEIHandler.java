package com.gtnewhorizon.structurelib.client.nei.integration.structurelib;

import static com.gtnewhorizon.structurelib.client.nei.IMCForNEI.STRUCTURE_LIB_HANDLER;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import com.gtnewhorizon.structurelib.alignment.constructable.IMultiblockInfoContainer;
import com.gtnewhorizon.structurelib.client.nei.MultiblockHandler;
import com.gtnewhorizon.structurelib.client.world.StructureWorld;

import codechicken.nei.recipe.TemplateRecipeHandler;

public class StructureCompatNEIHandler extends MultiblockHandler {

    private static final StructureCompatGuiHandler baseHandler = new StructureCompatGuiHandler();
    private static final StructureWorld DummyWorld = new StructureWorld();

    public StructureCompatNEIHandler() {
        super(baseHandler);
    }

    @Override
    public TemplateRecipeHandler newInstance() {
        return new StructureCompatNEIHandler();
    }

    @Override
    public String getOverlayIdentifier() {
        return STRUCTURE_LIB_HANDLER;
    }

    @Override
    protected void tryLoadingMultiblock(ItemStack candidate) {
        if (candidate.getItem() instanceof ItemBlock ib) {
            Block block = ib.field_150939_a;
            if (block.hasTileEntity(candidate.getItemDamage())) {
                TileEntity te = block.createTileEntity(DummyWorld, ib.getMetadata(candidate.getItemDamage()));
                if (te != null && IMultiblockInfoContainer.contains(te.getClass())) {
                    baseHandler.setOnIngredientChanged(ingredients -> {
                        this.ingredients = ingredients;
                        resetPositionedIngredients();
                    });
                    baseHandler.setOnCandidateChanged(this::setResults);
                    baseHandler.loadTileMulti(te, candidate);
                }
            }
        }
    }
}
