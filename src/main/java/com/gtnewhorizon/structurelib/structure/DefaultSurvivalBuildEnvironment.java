package com.gtnewhorizon.structurelib.structure;

import net.minecraft.entity.player.EntityPlayer;

class DefaultSurvivalBuildEnvironment implements ISurvivalBuildEnvironment {

    private final IItemSource source;
    private final EntityPlayer actor;

    public DefaultSurvivalBuildEnvironment(IItemSource source, EntityPlayer actor) {
        this.source = source;
        this.actor = actor;
    }

    @Override
    public IItemSource getSource() {
        return source;
    }

    @Override
    public EntityPlayer getActor() {
        return actor;
    }
}
