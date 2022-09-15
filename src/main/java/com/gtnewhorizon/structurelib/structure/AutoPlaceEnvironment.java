package com.gtnewhorizon.structurelib.structure;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;

/**
 * Represent the environment in which autoplace of a single element took place.
 */
public class AutoPlaceEnvironment {
    private IItemSource source;
    private final EntityPlayer actor;
    private final Consumer<IChatComponent> chatter;
    private final IStructureDefinition<?> definition;
    private final String piece;
    private final ExtendedFacing facing;
    final int[] offsetABC;
    private final int[] baseOffsetABC;

    public static AutoPlaceEnvironment fromLegacy(
            IItemSource source, EntityPlayer actor, Consumer<IChatComponent> chatter) {
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
        return new AutoPlaceEnvironment(source, actor, chatter, null, null, null, null, null);
    }

    AutoPlaceEnvironment(
            EntityPlayer actor,
            Consumer<IChatComponent> chatter,
            IStructureDefinition<?> definition,
            String piece,
            ExtendedFacing facing,
            int[] baseOffsetABC) {
        this.source = null;
        this.actor = actor;
        this.chatter = chatter;
        this.definition = definition;
        this.piece = piece;
        this.facing = facing;
        this.offsetABC = new int[3];
        this.baseOffsetABC = baseOffsetABC;
    }

    AutoPlaceEnvironment(
            IItemSource source,
            EntityPlayer actor,
            Consumer<IChatComponent> chatter,
            IStructureDefinition<?> definition,
            String piece,
            ExtendedFacing facing,
            int[] offsetABC,
            int[] baseOffsetABC) {
        this.source = definition != null && !(source instanceof WrappedIItemSource)
                ? new WrappedIItemSource(this, source)
                : source;
        this.actor = actor;
        this.chatter = chatter;
        this.definition = definition;
        this.piece = piece;
        this.facing = facing;
        this.offsetABC = offsetABC;
        this.baseOffsetABC = baseOffsetABC;
    }

    protected AutoPlaceEnvironment(AutoPlaceEnvironment parent) {
        this(
                parent.getSource(),
                parent.getActor(),
                parent.getChatter(),
                parent.definition,
                parent.piece,
                parent.facing,
                parent.offsetABC,
                parent.baseOffsetABC);
    }

    void setSource(IItemSource source) {
        this.source = definition != null && !(source instanceof WrappedIItemSource)
                ? new WrappedIItemSource(this, source)
                : source;
    }

    public APILevel getAPILevel() {
        return definition == null
                ? actor instanceof EntityPlayerMP ? APILevel.Legacy : APILevel.LegacyRelaxed
                : APILevel.V2;
    }

    /**
     * From where survival autoplace will drain resources.
     */
    public IItemSource getSource() {
        return source;
    }

    /**
     * The initiator of actions. for very critical errors you can also just send the error messages here, bypassing
     * any filter that {@link #getChatter()} might have. You might want to use
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
     *
     * @param source new source
     * @return new instance
     */
    public AutoPlaceEnvironment withSource(IItemSource source) {
        return new AutoPlaceEnvironment(source, actor, chatter, definition, piece, facing, offsetABC, baseOffsetABC);
    }

    /**
     * Return a new instance with actor modified to given value.
     *
     * @param actor new actor
     * @return new instance
     */
    public AutoPlaceEnvironment withActor(EntityPlayer actor) {
        return new AutoPlaceEnvironment(source, actor, chatter, definition, piece, facing, offsetABC, baseOffsetABC);
    }

    /**
     * Return a new instance with chatter modified to given value.
     *
     * @param chatter new chatter
     * @return new instance
     */
    public AutoPlaceEnvironment withChatter(Consumer<IChatComponent> chatter) {
        return new AutoPlaceEnvironment(source, actor, chatter, definition, piece, facing, offsetABC, baseOffsetABC);
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
     * Defines the various API level an {@link AutoPlaceEnvironment} has implemented.
     * <p>
     * Enum constants are defined in chronological order.
     */
    public enum APILevel {
        /**
         * Implements {@link #getChatter()}, {@link #getActor()} and {@link #getSource()}.
         * {@link #getActor()} is guaranteed to be an {@link net.minecraft.entity.player.EntityPlayerMP}
         */
        Legacy,
        /**
         * Implements {@link #getChatter()}, {@link #getActor()} and {@link #getSource()}.
         * {@link #getActor()} is <b>NOT</b> guaranteed to be an {@link net.minecraft.entity.player.EntityPlayerMP}
         */
        LegacyRelaxed,
        /**
         * Implements everything so far we have defined.
         */
        V2,
    }
}
