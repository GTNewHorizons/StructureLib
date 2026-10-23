package com.gtnewhorizon.structurelib.alignment.constructable;

import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.ConfigurationHandler;
import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.dismantle.DismantleManager;
import com.gtnewhorizon.structurelib.item.ItemConstructableTriggerV2;
import com.gtnewhorizon.structurelib.item.ItemDebugConstructableTrigger;
import com.gtnewhorizon.structurelib.item.ProjectorTier;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;

public class ConstructableUtility {

    private static final WeakHashMap<EntityPlayerMP, Long> lastUse = new WeakHashMap<>();
    private static long clientSideLastUse = 0;

    private ConstructableUtility() {}

    public static boolean handle(ItemStack aStack, EntityPlayer aPlayer, World aWorld, int aX, int aY, int aZ,
            int aSide) {
        StructureLibAPI.startHinting(aWorld);
        boolean ret = handle0(ProjectorTier.forStructure(aStack), aPlayer, aWorld, aX, aY, aZ, aSide);
        StructureLibAPI.endHinting(aWorld);
        return ret;
    }

    private static boolean handle0(ItemStack aStack, EntityPlayer aPlayer, World aWorld, int aX, int aY, int aZ,
            int aSide) {
        TileEntity tTileEntity = aWorld.getTileEntity(aX, aY, aZ);
        if (tTileEntity == null || aPlayer instanceof FakePlayer) {
            return aPlayer instanceof EntityPlayerMP;
        }
        if (aPlayer instanceof EntityPlayerMP) {
            // not sneaking - client side will generate hologram. nothing to do on server side
            if (!aPlayer.isSneaking()) return true;

            long timePast = System.currentTimeMillis() - getLastUseMilis(aPlayer);
            if (timePast < ConfigurationHandler.INSTANCE.getAutoPlaceInterval()) {
                aPlayer.addChatComponentMessage(
                        new ChatComponentTranslation(
                                "item.structurelib.constructableTrigger.too_fast",
                                ConfigurationHandler.INSTANCE.getAutoPlaceInterval() - timePast));
                return true;
            }
        } else if (!StructureLib.isCurrentPlayer(aPlayer)) {
            return false;
        }

        IConstructable constructable = getConstructable(tTileEntity, aSide);
        if (constructable == null) return false;
        return interactWithConstructable(aStack, aPlayer, constructable);
    }

    public static IConstructable getConstructable(TileEntity tTileEntity, int side) {
        IConstructable constructable = null;
        if (tTileEntity instanceof IConstructableProvider)
            constructable = ((IConstructableProvider) tTileEntity).getConstructable();
        else if (tTileEntity instanceof IConstructable) {
            constructable = (IConstructable) tTileEntity;
        } else if (IMultiblockInfoContainer.contains(tTileEntity.getClass())) {
            ExtendedFacing facing = tTileEntity instanceof IAlignment ? ((IAlignment) tTileEntity).getExtendedFacing()
                    : ExtendedFacing.of(ForgeDirection.getOrientation(side));
            constructable = IMultiblockInfoContainer.<TileEntity>get(tTileEntity.getClass())
                    .toConstructable(tTileEntity, facing);
        }

        return constructable;
    }

    /** Server-side remote construction. Returns true only when the entire structure is complete. */
    public static boolean handleRemote(ItemStack stack, EntityPlayerMP player, World world, int x, int y, int z,
            int side) {
        stack = ProjectorTier.forStructure(stack);
        if (world.isRemote || player.worldObj != world || !world.blockExists(x, y, z)) return false;
        long timePast = System.currentTimeMillis() - getLastUseMilis(player);
        if (timePast < ConfigurationHandler.INSTANCE.getAutoPlaceInterval()) {
            player.addChatMessage(
                    new ChatComponentTranslation(
                            "item.structurelib.constructableTrigger.too_fast",
                            ConfigurationHandler.INSTANCE.getAutoPlaceInterval() - timePast));
            return false;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        IConstructable constructable = tile == null ? null : getConstructable(tile, side);
        if (constructable == null) return false;
        if (player.capabilities.isCreativeMode || stack.getItem() instanceof ItemDebugConstructableTrigger) {
            constructable.construct(stack, false);
            setLastUseMilis(player);
            return true;
        }
        if (!(constructable instanceof ISurvivalConstructable)) {
            player.addChatMessage(new ChatComponentTranslation("structurelib.autoplace.error.not_enabled"));
            return false;
        }
        int built = ((ISurvivalConstructable) constructable).survivalConstruct(
                stack,
                ConfigurationHandler.INSTANCE.getAutoPlaceBudget(),
                ISurvivalBuildEnvironment.create(IItemSource.fromPlayer(player), player));
        if (built > 0) player.addChatMessage(new ChatComponentTranslation("structurelib.autoplace.built_stat", built));
        else if (built == -1) player.addChatMessage(new ChatComponentTranslation("structurelib.autoplace.complete"));
        setLastUseMilis(player);
        return built == -1;
    }

    private static boolean interactWithConstructable(ItemStack aStack, EntityPlayer aPlayer,
            IConstructable constructable) {
        if (aPlayer instanceof EntityPlayerMP playerMP) {
            // server side and sneaking (already checked above)
            // do construct
            if (aPlayer.capabilities.isCreativeMode || aStack.getItem() instanceof ItemDebugConstructableTrigger) {
                constructable.construct(aStack, false);
                if (aStack.getItem() instanceof ItemConstructableTriggerV2)
                    DismantleManager.INSTANCE.onCompletedBuild(playerMP);
            } else if (constructable instanceof ISurvivalConstructable) {
                int built = ((ISurvivalConstructable) constructable).survivalConstruct(
                        aStack,
                        ConfigurationHandler.INSTANCE.getAutoPlaceBudget(),
                        ISurvivalBuildEnvironment.create(IItemSource.fromPlayer(playerMP), playerMP));
                if (built > 0) {
                    playerMP.addChatMessage(new ChatComponentTranslation("structurelib.autoplace.built_stat", built));
                } else if (built == -1) {
                    playerMP.addChatMessage(new ChatComponentTranslation("structurelib.autoplace.complete"));
                    if (aStack.getItem() instanceof ItemConstructableTriggerV2)
                        DismantleManager.INSTANCE.onCompletedBuild(playerMP);
                }
                setLastUseMilis(aPlayer);
            } else {
                playerMP.addChatMessage(new ChatComponentTranslation("structurelib.autoplace.error.not_enabled"));
            }
            return true;
        }
        // client side
        // particles and text
        constructable.construct(aStack, true);
        if (System.currentTimeMillis() - getLastUseMilis(aPlayer) >= 300)
            StructureLib.addClientSideChatMessages(constructable.getStructureDescription(aStack));
        setLastUseMilis(aPlayer);
        return false;
    }

    private static void setLastUseMilis(EntityPlayer aPlayer) {
        if (!(aPlayer instanceof EntityPlayerMP))
            // assume client side
            clientSideLastUse = System.currentTimeMillis();
        else lastUse.put((EntityPlayerMP) aPlayer, System.currentTimeMillis());
    }

    private static long getLastUseMilis(EntityPlayer aPlayer) {
        if (!(aPlayer instanceof EntityPlayerMP))
            // assume client side
            return clientSideLastUse;
        return lastUse.getOrDefault(aPlayer, 0L);
    }
}
