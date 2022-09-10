package com.gtnewhorizon.structurelib.structure;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IStructureElement.PlaceResult;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

class SurvivalBuildStructureWalker<T> implements IStructureWalker<T> {
    final T object;
    final ItemStack trigger;
    private final int elementBudget;
    private final ISurvivalBuildEnvironment params;
    private final boolean check;
    private int built = -1;

    private final AutoPlaceEnvironment env;

    public SurvivalBuildStructureWalker(
            T object,
            ItemStack trigger,
            int elementBudget,
            ISurvivalBuildEnvironment params,
            IStructureDefinition<?> definition,
            String piece,
            ExtendedFacing facing,
            int[] baseOffsetABC,
            boolean check) {
        this.object = object;
        this.trigger = trigger;
        this.elementBudget = elementBudget;
        this.params = params;
        this.check = check;

        env = new AutoPlaceEnvironment(
                params.getActor(),
                params.getActor()::addChatComponentMessage,
                definition,
                piece,
                facing,
                baseOffsetABC);
    }

    @Override
    public boolean visit(IStructureElement<T> element, World world, int x, int y, int z, int a, int b, int c) {
        env.offsetABC[0] = a;
        env.offsetABC[1] = b;
        env.offsetABC[2] = c;
        env.setSource(params.getSource());
        PlaceResult placeResult = element.survivalPlaceBlock(object, world, x, y, z, trigger, env);
        if (placeResult != PlaceResult.SKIP && built == -1) built = 0;
        switch (placeResult) {
            case SKIP:
                return true;
            case ACCEPT:
                if (check) element.check(object, world, x, y, z);
                return ++built < elementBudget;
            case REJECT:
                StructureLibAPI.markHintParticleError(params.getActor(), world, x, y, z);
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
