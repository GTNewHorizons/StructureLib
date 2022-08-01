package com.gtnewhorizon.structurelib.structure;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.ConstructableUtility;
import com.gtnewhorizon.structurelib.structure.IStructureElement.PlaceResult;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

class SurvivalBuildStructureWalker<T> implements IStructureWalker<T> {
    private final T object;
    private final ItemStack trigger;
    private final IItemSource source;
    private final EntityPlayerMP actor;
    private final int elementBudget;
    private final boolean check;
    private int built = -1;

    public SurvivalBuildStructureWalker(
            T object, ItemStack trigger, IItemSource source, EntityPlayerMP actor, int elementBudget, boolean check) {
        this.object = object;
        this.trigger = trigger;
        this.source = source;
        this.actor = actor;
        this.elementBudget = elementBudget;
        this.check = check;
    }

    @Override
    public boolean visit(IStructureElement<T> element, World world, int x, int y, int z) {
        PlaceResult placeResult = element.survivalPlaceBlock(
                object, world, x, y, z, trigger, source, actor, actor::addChatComponentMessage);
        if (placeResult != PlaceResult.SKIP && built == -1) built = 0;
        switch (placeResult) {
            case SKIP:
                return true;
            case ACCEPT:
                if (check) element.check(object, world, x, y, z);
                return ++built < elementBudget;
            case REJECT:
                if (ConstructableUtility.redBrokenOne)
                    StructureLibAPI.updateHintParticleTint(actor, world, x, y, z, new short[] {255, 128, 128, 0});
                return false;
            case ACCEPT_STOP:
                if (check) element.check(object, world, x, y, z);
                built += 1;
                // intentional fallthrough
            case STOP:
                return false;
            default:
                throw new NullPointerException();
        }
    }

    public int getBuilt() {
        return built;
    }
}
