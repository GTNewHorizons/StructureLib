package com.gtnewhorizon.structurelib.client.nei.integration.structurelib;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IMultiblockInfoContainer;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.client.nei.GUI_MultiblockHandler;

public class StructureCompatGuiHandler extends GUI_MultiblockHandler<IConstructable> {

    public StructureCompatGuiHandler() {
        super();
    }

    // spotless:off
// TODO: Move this into a non GUI multi block handler class
//    @Override
//    protected void placeMultiblock() {
//        Block stackBlock = ((ItemBlock) stackForm.getItem()).field_150939_a;
//        renderer.world.setBlock(
//                PLACE_POSITION.x,
//                PLACE_POSITION.y,
//                PLACE_POSITION.z,
//                stackBlock,
//                stackForm.getItemDamage(),
//                3);
//
//        TileEntity tTileEntity = renderer.world.getTileEntity(PLACE_POSITION.x, PLACE_POSITION.y, PLACE_POSITION.z);
//        IMultiblockInfoContainer<TileEntity> t = IMultiblockInfoContainer.get(tTileEntity.getClass());
//        ISurvivalConstructable multi = t.toConstructable(tTileEntity, ExtendedFacing.DEFAULT);
//
//        if (!StructureLibAPI.isInstrumentEnabled()) {
//            StructureLibAPI.enableInstrument(StructureLibAPI.MOD_ID);
//        }
//        structureElements.clear();
//
//        int result, iterations = 0;
//        boolean tryContruct = false;
//        do {
//            result = multi.survivalConstruct(
//                    getBuildTriggerStack(),
//                    Integer.MAX_VALUE,
//                    ISurvivalBuildEnvironment.create(CreativeItemSource.instance, fakeMultiblockBuilder));
//            iterations++;
//            if (result == -2) {
//                tryContruct = true;
//                break;
//            }
//        } while (result > 0 && iterations < StructureWorld.MAX_PLACE_ROUNDS);
//
//        if (tryContruct) {
//            multi.construct(getBuildTriggerStack(), false);
//        }
//
//        // A single tick is needed for some non GT multiblocks to complete
//        renderer.world.updateEntitiesForNEI();
//
//        if (StructureLibAPI.isInstrumentEnabled()) {
//            StructureLibAPI.disableInstrument();
//        }
//
//    }
// spotless:on
    public void loadTileMulti(TileEntity multiblock, ItemStack stackForm) {
        IMultiblockInfoContainer<TileEntity> m = IMultiblockInfoContainer.get(multiblock.getClass());
        IConstructable constructable = m.toConstructable(multiblock, ExtendedFacing.DEFAULT);
        super.loadMultiblock(constructable, stackForm);
    }
}
