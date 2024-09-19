package com.gtnewhorizon.structurelib.client.nei.integration.gregtech;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.client.nei.GUI_MultiblockHandler;

public class GT_GUI_MultiblockHandler extends GUI_MultiblockHandler<IConstructable> {

    public GT_GUI_MultiblockHandler() {
        super();
    }

    // spotless:off
// TODO: Move this into a non GUI multi block handler class
//    @Override
//    protected void placeMultiblock() {
//        if (GT_Runnable_MachineBlockUpdate.isCurrentThreadEnabled()) {
//            GT_Runnable_MachineBlockUpdate.setCurrentThreadEnabled(false);
//        }
//
//        IConstructable constructable = null;
//        ItemStack copy = stackForm.copy();
//        copy.getItem().onItemUse(
//                copy,
//                renderer.world.getFakeMultiblockBuilder(),
//                renderer.world,
//                PLACE_POSITION.x,
//                PLACE_POSITION.y,
//                PLACE_POSITION.z,
//                0,
//                PLACE_POSITION.x,
//                PLACE_POSITION.y,
//                PLACE_POSITION.z);
//
//        TileEntity tTileEntity = renderer.world.getTileEntity(PLACE_POSITION.x, PLACE_POSITION.y, PLACE_POSITION.z);
//        ((ITurnable) tTileEntity).setFrontFacing(ForgeDirection.SOUTH);
//        IMetaTileEntity mte = ((IGregTechTileEntity) tTileEntity).getMetaTileEntity();
//
//        if (!StructureLibAPI.isInstrumentEnabled()) {
//            StructureLibAPI.enableInstrument(StructureLibAPI.MOD_ID);
//        }
//        structureElements.clear();
//
// This seems relevant because the .construct() call ignores hatches, etc.  Needs an optimized CreativeItemSource
//        if (mte instanceof ISurvivalConstructable survivalConstructable) {
//            int result, iterations = 0;
//            do {
//                result = survivalConstructable.survivalConstruct(
//                        getBuildTriggerStack(),
//                        Integer.MAX_VALUE,
//                        ISurvivalBuildEnvironment.create(CreativeItemSource.instance, renderer.world.getFakeMultiblockBuilder()));
//                iterations++;
//            } while (result > 0 && iterations < MAX_PLACE_ROUNDS);
//        } else if (tTileEntity instanceof IConstructableProvider iConstructableProvider) {
//            constructable = iConstructableProvider.getConstructable();
//        } else if (tTileEntity instanceof IConstructable iConstructable) {
//            constructable = iConstructable;
//        }
//        if (constructable != null) {
//            constructable.construct(getBuildTriggerStack(), false);
//        }
//
//        if (StructureLibAPI.isInstrumentEnabled()) {
//            StructureLibAPI.disableInstrument();
//        }
//
//        if (!GT_Runnable_MachineBlockUpdate.isCurrentThreadEnabled()) {
//            GT_Runnable_MachineBlockUpdate.setCurrentThreadEnabled(true);
//        }
//    }
// spotless:on
}
