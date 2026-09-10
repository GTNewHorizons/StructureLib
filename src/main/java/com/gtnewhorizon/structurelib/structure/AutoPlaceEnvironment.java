package com.gtnewhorizon.structurelib.structure;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.fluid.FluidSourceProviders;
import com.gtnewhorizon.structurelib.fluid.IFluidSource;

/**
 * Represent the environment in which autoplace of a single element took place.
 */
public class AutoPlaceEnvironment {

    private IItemSource source;
    private IFluidSource fluidSource;
    private final EntityPlayer actor;
    private final Consumer<IChatComponent> chatter;
    private final IStructureDefinition<?> definition;
    private final String piece;
    private final ExtendedFacing facing;
    final int[] offsetABC;
    private final int[] baseOffsetABC;
    private final int[] basePositionXYZ;
    private final Object contextObject;
    private final FluidRoundState fluidRoundState;

    public static AutoPlaceEnvironment fromLegacy(IItemSource source, EntityPlayer actor,
            Consumer<IChatComponent> chatter) {
        if (source instanceof WrappedIItemSource) {
            AutoPlaceEnvironment original = ((WrappedIItemSource) source).container;
            // feels like this is extremely likely to cause issues, but this does allow us to recover lost info.
            // TODO probably should remove this once all addons migrate over
            if (actor != original.actor || chatter != original.chatter) {
                AutoPlaceEnvironment newEnv = original;
                if (actor != original.actor) newEnv = newEnv.withActor(actor);
                if (chatter != original.chatter) newEnv = newEnv.withChatter(chatter);
                return newEnv;
            }
            return original;
        }
        return new AutoPlaceEnvironment(source, null, actor, chatter, null, null, null, null, null, null, null);
    }

    AutoPlaceEnvironment(EntityPlayer actor, Consumer<IChatComponent> chatter, IStructureDefinition<?> definition,
            String piece, ExtendedFacing facing, int[] baseOffsetABC, int[] basePositionXYZ, Object contextObject,
            IFluidSource fluidSource, FluidRoundState fluidRoundState) {
        this.source = null;
        this.fluidSource = fluidSource;
        this.actor = actor;
        this.chatter = chatter;
        this.definition = definition;
        this.piece = piece;
        this.facing = facing;
        this.offsetABC = new int[3];
        this.baseOffsetABC = baseOffsetABC;
        this.basePositionXYZ = basePositionXYZ;
        this.contextObject = contextObject;
        this.fluidRoundState = fluidRoundState;
    }

    AutoPlaceEnvironment(IItemSource source, IFluidSource fluidSource, EntityPlayer actor,
            Consumer<IChatComponent> chatter, IStructureDefinition<?> definition, String piece, ExtendedFacing facing,
            int[] offsetABC, int[] baseOffsetABC, int[] basePositionXYZ, Object contextObject) {
        this(
                source,
                fluidSource,
                actor,
                chatter,
                definition,
                piece,
                facing,
                offsetABC,
                baseOffsetABC,
                basePositionXYZ,
                contextObject,
                null);
    }

    private AutoPlaceEnvironment(IItemSource source, IFluidSource fluidSource, EntityPlayer actor,
            Consumer<IChatComponent> chatter, IStructureDefinition<?> definition, String piece, ExtendedFacing facing,
            int[] offsetABC, int[] baseOffsetABC, int[] basePositionXYZ, Object contextObject,
            FluidRoundState fluidRoundState) {
        this.source = definition != null && !(source instanceof WrappedIItemSource)
                ? new WrappedIItemSource(this, source)
                : source;
        this.fluidSource = fluidSource;
        this.actor = actor;
        this.chatter = chatter;
        this.definition = definition;
        this.piece = piece;
        this.facing = facing;
        this.offsetABC = offsetABC;
        this.baseOffsetABC = baseOffsetABC;
        this.basePositionXYZ = basePositionXYZ;
        this.contextObject = contextObject;
        this.fluidRoundState = fluidRoundState;
    }

    protected AutoPlaceEnvironment(AutoPlaceEnvironment parent) {
        this(
                parent.getSource(),
                parent.fluidSource,
                parent.getActor(),
                parent.getChatter(),
                parent.definition,
                parent.piece,
                parent.facing,
                parent.offsetABC,
                parent.baseOffsetABC,
                parent.basePositionXYZ,
                parent.contextObject,
                parent.fluidRoundState);
    }

    void setSource(IItemSource source) {
        this.source = definition != null && !(source instanceof WrappedIItemSource)
                ? new WrappedIItemSource(this, source)
                : source;
    }

    public APILevel getAPILevel() {
        return definition == null ? actor instanceof EntityPlayerMP ? APILevel.Legacy : APILevel.LegacyRelaxed
                : APILevel.V2;
    }

    /**
     * From where survival autoplace will drain resources.
     */
    public IItemSource getSource() {
        return source;
    }

    /**
     * The fluid source this environment has been given, or null when it has none.
     * <p>
     * A structure element that has to place fluid blocks should ask {@link #getEffectiveFluidSource()} instead, which
     * falls back to whatever the actor can offer when there is no explicit fluid source.
     */
    @Nullable
    public IFluidSource getFluidSource() {
        return fluidSource;
    }

    /**
     * From where survival autoplace will drain fluid.
     * <p>
     * This is the fluid source this environment has been given, or what the actor can offer when the actor is a server
     * side player: the {@linkplain com.gtnewhorizon.structurelib.fluid.FluidSourceProviders registered fluid source
     * providers}, e.g. the ME network behind a wireless terminal the player has on them, and then the fluid containers
     * the player is carrying. It is null when the actor isn't a server side player and no fluid source has been given,
     * in which case fluid blocks cannot be placed at all.
     */
    @Nullable
    public IFluidSource getEffectiveFluidSource() {
        if (fluidSource != null) return fluidSource;
        if (actor instanceof EntityPlayerMP) return FluidSourceProviders.getSourceFor((EntityPlayerMP) actor);
        return null;
    }

    /**
     * The initiator of actions. for very critical errors you can also just send the error messages here, bypassing any
     * filter that {@link #getChatter()} might have. You might want to use
     * {@link com.gtnewhorizon.structurelib.StructureLibAPI#addThrottledChat(Object, EntityPlayer, IChatComponent, short)}
     * to help reduce spam.
     */
    public EntityPlayer getActor() {
        return actor;
    }

    /**
     * send error messages here. Caller will choose an appropriate way to forward it to player if the other fallbacks
     * also fails.
     */
    public Consumer<IChatComponent> getChatter() {
        return chatter;
    }

    /**
     * Test if given location is contained within the current piece.
     *
     * @param offsetA offset in A direction <b>relative to current element</b>
     * @param offsetB offset in B direction <b>relative to current element</b>
     * @param offsetC offset in C direction <b>relative to current element</b>
     * @return true if contained, false otherwise.
     */
    public boolean isContainedInPiece(int offsetA, int offsetB, int offsetC) {
        if (definition == null) return false;
        return definition.isContainedInStructure(
                piece,
                offsetABC[0] + offsetA + baseOffsetABC[0],
                offsetABC[1] + offsetB + baseOffsetABC[1],
                offsetABC[2] + offsetC + baseOffsetABC[2]);
    }

    public ExtendedFacing getFacing() {
        return facing;
    }

    /**
     * Return a new instance with source modified to given value.
     * <p>
     * The returned instance shares the fluid source, and every bookkeeping about the current autoplace round, with this
     * one.
     *
     * @param source new source
     * @return new instance
     */
    public AutoPlaceEnvironment withSource(IItemSource source) {
        return new AutoPlaceEnvironment(
                source,
                fluidSource,
                actor,
                chatter,
                definition,
                piece,
                facing,
                offsetABC,
                baseOffsetABC,
                basePositionXYZ,
                contextObject,
                fluidRoundState);
    }

    /**
     * Return a new instance with actor modified to given value.
     * <p>
     * The returned instance shares the fluid source, and every bookkeeping about the current autoplace round, with this
     * one.
     *
     * @param actor new actor
     * @return new instance
     */
    public AutoPlaceEnvironment withActor(EntityPlayer actor) {
        return new AutoPlaceEnvironment(
                source,
                fluidSource,
                actor,
                chatter,
                definition,
                piece,
                facing,
                offsetABC,
                baseOffsetABC,
                basePositionXYZ,
                contextObject,
                fluidRoundState);
    }

    /**
     * Return a new instance with chatter modified to given value.
     * <p>
     * The returned instance shares the fluid source, and every bookkeeping about the current autoplace round, with this
     * one.
     *
     * @param chatter new chatter
     * @return new instance
     */
    public AutoPlaceEnvironment withChatter(Consumer<IChatComponent> chatter) {
        return new AutoPlaceEnvironment(
                source,
                fluidSource,
                actor,
                chatter,
                definition,
                piece,
                facing,
                offsetABC,
                baseOffsetABC,
                basePositionXYZ,
                contextObject,
                fluidRoundState);
    }

    /**
     * Return a new instance with an explicit fluid source.
     * <p>
     * This is how a multiblock that wants its fluid to come from somewhere else than the player inventory, e.g. from a
     * tank, hands its fluid source to autoplace.
     *
     * @param fluidSource new fluid source
     * @return new instance
     */
    public AutoPlaceEnvironment withFluidSource(@Nullable IFluidSource fluidSource) {
        return new AutoPlaceEnvironment(
                source,
                fluidSource,
                actor,
                chatter,
                definition,
                piece,
                facing,
                offsetABC,
                baseOffsetABC,
                basePositionXYZ,
                contextObject,
                fluidRoundState);
    }

    IStructureDefinition<?> getDefinition() {
        return definition;
    }

    String getPiece() {
        return piece;
    }

    int[] getBaseOffsetABC() {
        return baseOffsetABC;
    }

    @Nullable
    int[] getBasePositionXYZ() {
        return basePositionXYZ;
    }

    @Nullable
    Object getContextObject() {
        return contextObject;
    }

    /**
     * The answer to the fluid placement gate of the current autoplace round, or null when it has not been asked yet.
     */
    @Nullable
    Boolean getFluidGateResult() {
        return fluidRoundState == null ? null : fluidRoundState.getGateResult();
    }

    void setFluidGateResult(boolean ready) {
        if (fluidRoundState != null) fluidRoundState.setGateResult(ready);
    }

    /**
     * Flag that at least one fluid element wanted to place its fluid, but had to wait for the structure to be built
     * first. An autoplace round that did nothing but wait reports zero placed elements instead of reporting that the
     * structure is done.
     */
    void markFluidPlacementDeferred() {
        if (fluidRoundState != null) fluidRoundState.markDeferred();
    }

    boolean isFluidPlacementDeferred() {
        return fluidRoundState != null && fluidRoundState.isDeferred();
    }

    private static class WrappedIItemSource implements IItemSource {

        final AutoPlaceEnvironment container;
        final IItemSource delegate;

        public WrappedIItemSource(AutoPlaceEnvironment container, IItemSource delegate) {
            this.container = container;
            this.delegate = delegate;
        }

        @Override
        @Nonnull
        public Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean simulate, int count) {
            return delegate.take(predicate, simulate, count);
        }

        @Override
        public ItemStack takeOne(Predicate<ItemStack> predicate, boolean simulate) {
            return delegate.takeOne(predicate, simulate);
        }

        @Override
        public boolean takeAll(Predicate<ItemStack> predicate, boolean simulate, int count) {
            return delegate.takeAll(predicate, simulate, count);
        }

        @Override
        public boolean takeOne(ItemStack stack, boolean simulate) {
            return delegate.takeOne(stack, simulate);
        }

        @Override
        public boolean takeAll(ItemStack stack, boolean simulate) {
            return delegate.takeAll(stack, simulate);
        }
    }

    /**
     * Bookkeeping about the current autoplace round, shared by every instance {@link AutoPlaceEnvironment} hands out
     * for itself. Implemented by the autoplace walker. Third parties should neither implement nor call this.
     */
    public interface FluidRoundState {

        /**
         * The answer the fluid placement gate gave earlier in this round, or null when it has not been asked yet.
         */
        @Nullable
        Boolean getGateResult();

        /**
         * Remember the answer of the fluid placement gate for the rest of this round.
         */
        void setGateResult(boolean ready);

        /**
         * Remember that a fluid element had to wait for the structure to be built first.
         */
        void markDeferred();

        /**
         * Whether any fluid element had to wait for the structure to be built in this round.
         */
        boolean isDeferred();
    }

    /**
     * Defines the various API level an {@link AutoPlaceEnvironment} has implemented.
     * <p>
     * Enum constants are defined in chronological order.
     */
    public enum APILevel {
        /**
         * Implements {@link #getChatter()}, {@link #getActor()} and {@link #getSource()}. {@link #getActor()} is
         * guaranteed to be an {@link net.minecraft.entity.player.EntityPlayerMP}
         */
        Legacy,
        /**
         * Implements {@link #getChatter()}, {@link #getActor()} and {@link #getSource()}. {@link #getActor()} is
         * <b>NOT</b> guaranteed to be an {@link net.minecraft.entity.player.EntityPlayerMP}
         */
        LegacyRelaxed,
        /**
         * Implements everything so far we have defined.
         */
        V2,
    }
}
