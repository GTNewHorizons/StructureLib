package com.gtnewhorizon.structurelib.structure;

import com.gtnewhorizon.structurelib.structure.IStructureElement.PlaceResult;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.UUID;

class SurvivalBuildStructureWalker<T> implements IStructureWalker<T> {
    private final T object;
    private final ItemStack trigger;
    private final IItemSource source;
    private final UUID actorProfile;
    private final int elementBudget;
    private final boolean check;
    private int built;

    public SurvivalBuildStructureWalker(T object, ItemStack trigger, IItemSource source, UUID actorProfile, int elementBudget, boolean check) {
        this.object = object;
        this.trigger = trigger;
        this.source = source;
        this.actorProfile = actorProfile;
        this.elementBudget = elementBudget;
        this.check = check;
        built = 0;
    }

    @Override
    public boolean visit(IStructureElement<T> element, World world, int x, int y, int z) {
        PlaceResult placeResult = element.survivalPlaceBlock(object, world, x, y, z, trigger, source, actorProfile);
        switch (placeResult) {
            case SKIP:
                return true;
            case ACCEPT:
                if (check)
                    element.check(object, world, x, y, z);
                return ++built < elementBudget;
            case REJECT:
                return false;
            case ACCEPT_STOP:
                if (check)
                    element.check(object, world, x, y, z);
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