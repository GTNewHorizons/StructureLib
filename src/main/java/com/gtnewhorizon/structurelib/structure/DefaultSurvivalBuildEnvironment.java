package com.gtnewhorizon.structurelib.structure;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;

import com.gtnewhorizon.structurelib.fluid.IFluidSource;

class DefaultSurvivalBuildEnvironment implements ISurvivalBuildEnvironment {

    private final IItemSource source;
    private final IFluidSource fluidSource;
    private final EntityPlayer actor;

    public DefaultSurvivalBuildEnvironment(IItemSource source, EntityPlayer actor) {
        this(source, null, actor);
    }

    public DefaultSurvivalBuildEnvironment(IItemSource source, @Nullable IFluidSource fluidSource, EntityPlayer actor) {
        this.source = source;
        this.fluidSource = fluidSource;
        this.actor = actor;
    }

    @Override
    public IItemSource getSource() {
        return source;
    }

    @Override
    public IFluidSource getFluidSource() {
        return fluidSource;
    }

    @Override
    public EntityPlayer getActor() {
        return actor;
    }
}
