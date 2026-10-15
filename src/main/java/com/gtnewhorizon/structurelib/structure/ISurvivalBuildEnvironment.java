package com.gtnewhorizon.structurelib.structure;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.fluid.FluidSourceProviders;
import com.gtnewhorizon.structurelib.fluid.IFluidSource;

/*
 * Architecture notes: this should never get any callback from survival build. All structure significant code belongs
 * within the IStructureElement. This will make the user easier to maintain the IStructureDefinition. Structure Element
 * knows the most about what this structure element should do, not the caller.
 */
public interface ISurvivalBuildEnvironment {

    /**
     * Get the source of the items. This will be invoked exactly once per structure element build.
     * <p>
     * If the call chain ever got into the old API, this will be called at least once per
     * {@link IStructureDefinition#survivalBuild(Object, ItemStack, String, World, ExtendedFacing, int, int, int, int, int, int, int, ISurvivalBuildEnvironment, boolean)}
     * call.
     */
    IItemSource getSource();

    /**
     * Get the source of the fluid, which autoplace drains when a structure element has to place a fluid block.
     * <p>
     * Returning null is fine, and is what every existing implementation does. Autoplace then falls back to draining the
     * fluid containers held by {@link #getActor()} when it is a server side player, and refuses to place fluid blocks
     * when there is no way to pay for them at all.
     * <p>
     * A multiblock that wants its fluid to come from somewhere else than the player inventory, e.g. from its own tank,
     * returns its own source here. A multiblock that receives an environment from its caller can wrap it with
     * {@link #withFluidSource(ISurvivalBuildEnvironment, IFluidSource)}. See {@link IFluidSource} for the sources
     * StructureLib can build out of the box.
     */
    default IFluidSource getFluidSource() {
        return null;
    }

    /**
     * Get the fluid source autoplace will actually drain from, which is the source this environment has been given, or
     * a source built from what the actor can offer when there is none and the actor is a server side player: first
     * every provider registered with {@link FluidSourceProviders}, then the fluid containers the actor is carrying.
     * <p>
     * This is what a multiblock that wants to put a source of its own in front of the player's own ones combines it
     * with, e.g. to prefer an ME network it is attached to and keep the player's buckets as a fallback:
     *
     * <pre>
     * env = ISurvivalBuildEnvironment.withFluidSource(
     *         env,
     *         IFluidSource.composite(IFluidSource.fromHandler(myTank), env.getEffectiveFluidSource()));
     * </pre>
     *
     * @return the source, or null when there is none, i.e. when the actor isn't a server side player and no source has
     *         been given
     */
    default IFluidSource getEffectiveFluidSource() {
        IFluidSource source = getFluidSource();
        if (source != null) return source;
        if (getActor() instanceof EntityPlayerMP) return FluidSourceProviders.getSourceFor((EntityPlayerMP) getActor());
        return null;
    }

    /**
     * Get the origin of action. This will be invoked at least once per
     * {@link IStructureDefinition#survivalBuild(Object, ItemStack, String, World, ExtendedFacing, int, int, int, int, int, int, int, ISurvivalBuildEnvironment, boolean)}
     * call.
     */
    EntityPlayer getActor();

    /**
     * Replace the fluid source of an environment, keeping everything else the same.
     * <p>
     * This is what a multiblock uses when it wants its fluid to come from its own tank instead of from the player, as
     * the environment it receives comes from its caller:
     *
     * <pre>
     * env = ISurvivalBuildEnvironment.withFluidSource(env, IFluidSource.fromHandler(myTank));
     * </pre>
     *
     * @param env         the environment to take the item source and the actor from
     * @param fluidSource the fluid source to use, or null to fall back to the player inventory
     */
    static ISurvivalBuildEnvironment withFluidSource(ISurvivalBuildEnvironment env, IFluidSource fluidSource) {
        if (env == null) throw new IllegalArgumentException();
        return new ISurvivalBuildEnvironment() {

            @Override
            public IItemSource getSource() {
                return env.getSource();
            }

            @Override
            public IFluidSource getFluidSource() {
                return fluidSource;
            }

            @Override
            public EntityPlayer getActor() {
                return env.getActor();
            }
        };
    }

    /**
     * Create a default implementation with three parameters all being constants.
     *
     * @param source from where to drain resource
     * @param actor  source of action
     */
    static ISurvivalBuildEnvironment create(IItemSource source, EntityPlayer actor) {
        return new DefaultSurvivalBuildEnvironment(source, null, actor);
    }

    /**
     * Create a default implementation with three parameters all being constants.
     *
     * @param source      from where to drain items
     * @param fluidSource from where to drain fluid, or null to drain the fluid containers held by the actor
     * @param actor       source of action
     */
    static ISurvivalBuildEnvironment create(IItemSource source, IFluidSource fluidSource, EntityPlayer actor) {
        return new DefaultSurvivalBuildEnvironment(source, fluidSource, actor);
    }
}
