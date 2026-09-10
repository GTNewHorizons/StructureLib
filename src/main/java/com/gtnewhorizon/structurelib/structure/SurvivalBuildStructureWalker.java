package com.gtnewhorizon.structurelib.structure;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IStructureElement.PlaceResult;

class SurvivalBuildStructureWalker<T> implements IStructureWalker<T>, AutoPlaceEnvironment.FluidRoundState {

    final T object;
    final ItemStack trigger;
    private final int elementBudget;
    private final ISurvivalBuildEnvironment params;
    private final boolean check;
    private int built = -1;

    private Boolean fluidGateResult;
    private boolean fluidDeferred;

    private final AutoPlaceEnvironment env;

    public SurvivalBuildStructureWalker(T object, ItemStack trigger, int elementBudget,
            ISurvivalBuildEnvironment params, IStructureDefinition<?> definition, String piece, ExtendedFacing facing,
            int[] baseOffsetABC, int[] basePositionXYZ, boolean check) {
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
                baseOffsetABC,
                basePositionXYZ,
                object,
                params.getFluidSource(),
                this);
    }

    @Override
    public boolean visit(IStructureElement<T> element, World world, int x, int y, int z, int a, int b, int c) {
        env.offsetABC[0] = a;
        env.offsetABC[1] = b;
        env.offsetABC[2] = c;
        env.setSource(params.getSource());
        PlaceResult placeResult = element.survivalPlaceBlock(object, world, x, y, z, trigger, env);
        if (placeResult != PlaceResult.SKIP && placeResult != PlaceResult.REJECT_CONTINUE && built == -1) built = 0;
        switch (placeResult) {
            case SKIP:
            case REJECT_CONTINUE:
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

    /**
     * Number of elements this round has placed, or -1 when the structure is done.
     * <p>
     * A round that only had to wait for the structure to be built before it could place its fluid did not build
     * anything, and the structure is not done either, so it reports no progress instead of reporting completion.
     */
    public int getBuilt() {
        return built == -1 && fluidDeferred ? 0 : built;
    }

    @Nullable
    @Override
    public Boolean getGateResult() {
        return fluidGateResult;
    }

    @Override
    public void setGateResult(boolean ready) {
        fluidGateResult = ready;
    }

    @Override
    public void markDeferred() {
        fluidDeferred = true;
    }

    @Override
    public boolean isDeferred() {
        return fluidDeferred;
    }
}
