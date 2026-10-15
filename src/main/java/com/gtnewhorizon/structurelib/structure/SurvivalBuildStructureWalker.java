package com.gtnewhorizon.structurelib.structure;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.github.bsideup.jabel.Desugar;
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
    private final List<DeferredFluid<T>> deferredFluids = new ArrayList<>();

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
        if (placeResult == PlaceResult.REJECT_CONTINUE && element.isFluidElement(object)) {
            // The fluid has to wait. Remember where it is, as this round may still build what it is waiting for, and
            // then it can be placed before the round is over instead of one round later.
            deferredFluids.add(new DeferredFluid<>(element, x, y, z, a, b, c));
        }
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
     * Place the fluids that had to wait during this round, now that the round has built everything it was going to.
     * <p>
     * A fluid position is often visited before the blocks that are meant to hold it, so a round that finishes the
     * structure would otherwise leave the fluid to the next round even though there is nothing left to wait for. The
     * gate is asked again, as the answer it gave was about the world as it was before this round built anything.
     * <p>
     * Like the walk itself, this places at most as many elements as the round still has budget left, and whatever it
     * cannot reach is left to the next round.
     *
     * @param world the world this round was run in
     */
    void finishRound(World world) {
        if (deferredFluids.isEmpty()) return;
        // A round that built nothing has not changed the answer of the gate either.
        if (built <= 0) return;

        int budget = elementBudget - built;
        env.clearFluidGateResult();
        for (DeferredFluid<T> deferred : deferredFluids) {
            if (budget <= 0) break;
            IStructureElement<T> element = deferred.element();
            // The element has to see the same offsets and item source it saw during the walk, as elements may read
            // channel data or take items through them.
            env.offsetABC[0] = deferred.a();
            env.offsetABC[1] = deferred.b();
            env.offsetABC[2] = deferred.c();
            env.setSource(params.getSource());
            PlaceResult placeResult = element
                    .survivalPlaceBlock(object, world, deferred.x(), deferred.y(), deferred.z(), trigger, env);
            if (placeResult != PlaceResult.ACCEPT) continue;
            if (check) element.check(object, world, deferred.x(), deferred.y(), deferred.z());
            built += 1;
            budget -= 1;
        }
        deferredFluids.clear();
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
    public void clearGateResult() {
        fluidGateResult = null;
    }

    @Override
    public void markDeferred() {
        fluidDeferred = true;
    }

    @Override
    public boolean isDeferred() {
        return fluidDeferred;
    }

    /**
     * A fluid position a round had to leave alone, kept so that it can be tried once more when the round is over.
     */
    @Desugar
    private record DeferredFluid<T> (IStructureElement<T> element, int x, int y, int z, int a, int b, int c) {}
}
