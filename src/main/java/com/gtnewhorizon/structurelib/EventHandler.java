package com.gtnewhorizon.structurelib;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.client.nei.GUI_MultiblockHandler;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.util.PositionedIStructureElement;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class EventHandler {

    @SubscribeEvent
    public void OnStructureEvent(StructureEvent.StructureElementVisitedEvent event) {
        GUI_MultiblockHandler.structureElements.add(
                new PositionedIStructureElement(
                        event.getX(),
                        event.getY(),
                        event.getZ(),
                        (IStructureElement<IConstructable>) event.getElement()));
    }
}
