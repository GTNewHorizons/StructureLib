package com.gtnewhorizon.structurelib.client.world;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import com.mojang.authlib.GameProfile;

public class StructureFakePlayer extends EntityPlayer {

    public StructureFakePlayer(World world, GameProfile profile) {
        super(world, profile);
        this.capabilities.isCreativeMode = true;
    }

    @Override
    public void addChatMessage(IChatComponent message) {

    }

    @Override
    public boolean canCommandSenderUseCommand(int permissionLevel, String command) {
        return false;
    }

    @Override
    public ChunkCoordinates getPlayerCoordinates() {
        return new ChunkCoordinates(0, 0, 0);
    }

    @Override
    public void openGui(Object mod, int modGuiId, World world, int x, int y, int z) {}

    @Override
    public boolean isEntityInvulnerable() {
        return true;
    }

    @Override
    public boolean canAttackPlayer(EntityPlayer player) {
        return false;
    }

    @Override
    public void onDeath(DamageSource source) {}

    @Override
    public void onUpdate() {}

    @Override
    public void travelToDimension(int dim) {}
}
