package com.gtnewhorizon.structurelib.dismantle;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.SecurityPermissions;
import appeng.api.features.ILocatable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import cpw.mods.fml.common.Loader;

/**
 * Optional AE2 integration. Mirrors StructureCompat's terminal -> locatable -> grid path without modifying
 * StructureCompat or any AE2 source.
 */
final class Ae2DismantleStorage {

    private final ItemStack terminal;
    private final IGrid grid;
    private final IWirelessTermHandler handler;
    private final IMEMonitor<IAEItemStack> inventory;
    private final EntityPlayerMP player;

    private Ae2DismantleStorage(ItemStack terminal, IGrid grid, IWirelessTermHandler handler,
            IMEMonitor<IAEItemStack> inventory, EntityPlayerMP player) {
        this.terminal = terminal;
        this.grid = grid;
        this.handler = handler;
        this.inventory = inventory;
        this.player = player;
    }

    static Ae2DismantleStorage find(EntityPlayerMP player) {
        if (!Loader.isModLoaded("appliedenergistics2")) return null;
        for (ItemStack stack : player.inventory.mainInventory) {
            Ae2DismantleStorage found = fromTerminal(stack, player);
            if (found != null) return found;
        }
        IInventory baubles = getBaubles(player);
        if (baubles != null) {
            for (int i = 0; i < baubles.getSizeInventory(); i++) {
                Ae2DismantleStorage found = fromTerminal(baubles.getStackInSlot(i), player);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static IInventory getBaubles(EntityPlayerMP player) {
        if (!Loader.isModLoaded("Baubles")) return null;
        try {
            Class<?> api = Class.forName("baubles.api.BaublesApi");
            Method getter = api.getMethod("getBaubles", net.minecraft.entity.player.EntityPlayer.class);
            return (IInventory) getter.invoke(null, player);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            return null;
        }
    }

    private static Ae2DismantleStorage fromTerminal(ItemStack stack, EntityPlayerMP player) {
        if (stack == null || stack.stackSize <= 0) return null;
        try {
            IWirelessTermHandler wh = AEApi.instance().registries().wireless().getWirelessTerminalHandler(stack);
            if (wh == null) return null;
            long id = Long.parseLong(wh.getEncryptionKey(stack));
            ILocatable locatable = AEApi.instance().registries().locatable().getLocatableBy(id);
            if (!(locatable instanceof IGridHost)) return null;
            IGridNode node = ((IGridHost) locatable).getGridNode(ForgeDirection.UNKNOWN);
            if (node == null || node.getGrid() == null) return null;
            IGrid grid = node.getGrid();
            if (!terminalAvailable(stack, wh, player)) return null;
            ISecurityGrid security = grid.getCache(ISecurityGrid.class);
            if (security != null && !security.hasPermission(player, SecurityPermissions.INJECT)) return null;
            IStorageGrid storage = grid.getCache(IStorageGrid.class);
            if (storage == null || storage.getItemInventory() == null) return null;
            return new Ae2DismantleStorage(stack, grid, wh, storage.getItemInventory(), player);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static boolean terminalAvailable(ItemStack stack, IWirelessTermHandler wh, EntityPlayerMP player) {
        boolean range = wh.hasInfinityRange(stack) || hasAddonBooster(stack);
        if (!range) range = AEApi.instance().registries().wireless().checkRange(stack, player);
        return range && (wh.hasInfinityPower(stack) || wh.hasPower(player, 1.0, stack));
    }

    private static boolean hasAddonBooster(ItemStack stack) {
        try {
            Method method = stack.getItem().getClass().getMethod("checkForBooster", ItemStack.class);
            if (Boolean.TRUE.equals(method.invoke(stack.getItem(), stack))) return true;
        } catch (ReflectiveOperationException ignored) {
            // AE2's built-in wireless term doesn't have this method.
        }
        try {
            Class<?> util = Class.forName("com.glodblock.github.util.Util");
            return Boolean.TRUE.equals(util.getMethod("hasInfinityBoosterCard", ItemStack.class).invoke(null, stack));
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    boolean isStillConnected(EntityPlayerMP currentPlayer) {
        Ae2DismantleStorage current = find(currentPlayer);
        return current != null && current.grid == grid;
    }

    private PlayerSource source() {
        return new PlayerSource(player, null);
    }

    boolean canAccept(List<ItemStack> drops) {
        List<IAEItemStack> combined = new ArrayList<>();
        for (ItemStack stack : drops) {
            IAEItemStack aeStack = AEApi.instance().storage().createItemStack(stack);
            if (aeStack == null) return false;
            boolean merged = false;
            for (IAEItemStack existing : combined) {
                if (existing.isSameType(aeStack)) {
                    existing.add(aeStack);
                    merged = true;
                    break;
                }
            }
            if (!merged) combined.add(aeStack);
        }
        for (IAEItemStack aeStack : combined) {
            if (inventory.injectItems(aeStack, Actionable.SIMULATE, source()) != null) return false;
        }
        return true;
    }

    List<ItemStack> insert(List<ItemStack> drops) {
        List<ItemStack> leftovers = new ArrayList<>();
        IEnergySource power = (amount, mode, multiplier) -> {
            if (handler.hasInfinityPower(terminal)) return amount;
            double adjusted = multiplier.multiply(amount);
            if (mode == Actionable.SIMULATE) return handler.hasPower(player, adjusted, terminal) ? amount : 0;
            return handler.usePower(player, adjusted, terminal) ? amount : 0;
        };
        for (ItemStack stack : drops) {
            IAEItemStack input = AEApi.instance().storage().createItemStack(stack);
            IAEItemStack rest = AEApi.instance().storage().poweredInsert(power, inventory, input, source());
            if (rest != null && rest.getStackSize() > 0) {
                long count = rest.getStackSize();
                while (count > 0) {
                    ItemStack part = rest.getItemStack();
                    part.stackSize = (int) Math.min(count, part.getMaxStackSize());
                    leftovers.add(part);
                    count -= part.stackSize;
                }
            }
        }
        return leftovers;
    }
}
