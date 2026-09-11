package com.gtnewhorizon.structurelib.fluid;

import java.util.Iterator;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import com.gtnewhorizon.structurelib.SortedRegistry;
import com.gtnewhorizon.structurelib.util.InventoryIterable;
import com.gtnewhorizon.structurelib.util.InventoryUtility;
import com.gtnewhorizon.structurelib.util.InventoryUtility.InventoryProvider;
import com.gtnewhorizon.structurelib.util.InventoryUtility.ItemStackExtractor;

/**
 * The registry of every way StructureLib knows how to pull fluid out of an item, plus the inventory walk that uses
 * these extractors.
 * <p>
 * This is the fluid counterpart of the item extraction facilities that survival autoplace already relies on, including
 * the fact that the registry is a {@link SortedRegistry}: entries can be reordered or disabled by the player in
 * StructureLib's config, and the ordering can be synced to the server.
 */
public class FluidContainerExtractors {

    private static final SortedRegistry<FluidContainerExtractor> EXTRACTORS = new SortedRegistry<>(
            "fluidcontainerextractors");

    static {
        register("3000-forge-fluid-container-item", new ForgeFluidContainerItemExtractor());
        register("5000-forge-fluid-container-registry", new ForgeFluidContainerRegistryExtractor());
    }

    private FluidContainerExtractors() {}

    /**
     * Dummy method to force the class to initialize, and with it the builtin extractors.
     */
    public static void init() {}

    /**
     * Register an extractor. Lower keys are tried first, so a mod that wants its extractor to take priority over
     * StructureLib builtin ones should use a key sorting before {@code "3000-forge-fluid-container-item"}.
     *
     * @param key unique key. Matches the key shown in the config gui.
     * @param val the extractor
     */
    public static void register(String key, FluidContainerExtractor val) {
        EXTRACTORS.register(key, val);
    }

    /**
     * Extract fluid from every inventory associated with the given player.
     * <p>
     * This walks exactly the same inventories as item autoplace does, see
     * {@link com.gtnewhorizon.structurelib.util.InventoryUtility}: the main inventory first, then whatever else is
     * registered, which currently means the ender chest when a mod asked for it and every inventory a mod registered
     * for a backpack. Inside every item that holds an inventory the walk goes one level deep, so a bucket a player left
     * in a backpack is found, and a mod that already lets autoplace take items out of its backpacks does not have to do
     * anything for the fluid containers inside them.
     * <p>
     * Armor slots are excluded, as there is no fluid container that goes into one.
     *
     * @param player   where to take fluid from
     * @param resource what fluid to take, and how much of it at most
     * @param simulate whether to actually take the fluid
     * @return what has actually been taken. Never null, and never holding more than requested.
     */
    public static FluidStack takeFromPlayer(EntityPlayerMP player, FluidStack resource, boolean simulate) {
        if (player == null) throw new IllegalArgumentException();
        if (resource == null || resource.getFluid() == null) throw new IllegalArgumentException();
        int remaining = resource.amount;
        for (InventoryProvider<?> provider : InventoryUtility.getInventoryProviders(player)) {
            if (remaining <= 0) break;
            remaining -= takeFromProvider(player, provider, new FluidStack(resource, remaining), simulate);
        }
        return new FluidStack(resource, resource.amount - remaining);
    }

    /**
     * Extract fluid from one registered inventory of a player, and let its provider know it changed.
     * <p>
     * The wildcard of {@link InventoryProvider} is captured by this method's type parameter, which is what allows the
     * inventory to be handed back to {@code markDirty}.
     */
    private static <R extends Iterable<ItemStack>> int takeFromProvider(EntityPlayerMP player,
            InventoryProvider<R> provider, FluidStack resource, boolean simulate) {
        R inventory = provider.getInventory(player);
        if (inventory == null) return 0;
        // Only an inventory that is backed by an IInventory can have a slot written back, which a container such as a
        // bucket needs when it is emptied into the world. A provider that hands out its stacks some other way is
        // skipped rather than drained in place, as draining without replacing would leave the player a filled
        // container that is already spent.
        if (!(inventory instanceof InventoryIterable<?>iterable)) return 0;
        int taken = takeFromInventory(iterable.getInventory(), resource, simulate, player, iterable.getMaxSlot(), true);
        if (taken > 0 && !simulate && hasConnection(player)) provider.markDirty(inventory);
        return taken;
    }

    /**
     * Whether the inventory of given player has a client to be kept in sync with. A fake player, e.g. the one a
     * structure preview tool uses, has no connection, and an inventory it changed never has to be sent anywhere.
     */
    private static boolean hasConnection(EntityPlayerMP player) {
        return player.playerNetServerHandler != null;
    }

    /**
     * Extract fluid from given inventory.
     * <p>
     * Containers held inside an item of this inventory, e.g. in a backpack, are not looked into here. Take from a
     * player through {@link #takeFromPlayer(EntityPlayerMP, FluidStack, boolean)} to look into those as well.
     *
     * @param inv      where to take fluid from
     * @param resource what fluid to take, and how much of it at most
     * @param simulate whether to actually take the fluid
     * @return what has actually been taken. Never null, and never holding more than requested.
     */
    public static FluidStack takeFromInventory(IInventory inv, FluidStack resource, boolean simulate) {
        if (inv == null) throw new IllegalArgumentException();
        if (resource == null || resource.getFluid() == null) throw new IllegalArgumentException();
        return new FluidStack(resource, takeFromInventory(inv, resource, simulate, null, -1, false));
    }

    /**
     * Walk the slots of one inventory and take as much of the requested fluid as they hold.
     *
     * @param maxSlot   slot to stop at, exclusive. -1 for every slot
     * @param recursive whether to look inside the items of this inventory, e.g. into a backpack
     * @return how much has been taken, never more than requested
     */
    private static int takeFromInventory(IInventory inv, FluidStack resource, boolean simulate,
            @Nullable EntityPlayerMP player, int maxSlot, boolean recursive) {
        int slots = maxSlot < 0 ? inv.getSizeInventory() : Math.min(maxSlot, inv.getSizeInventory());
        int remaining = resource.amount;
        for (int slot = 0; slot < slots && remaining > 0; slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack == null || stack.getItem() == null || stack.stackSize <= 0) continue;
            int taken = takeFromStack(stack, inv, slot, resource, remaining, simulate, player);
            if (taken > 0) {
                remaining -= taken;
                continue;
            }
            if (recursive && player != null) {
                remaining -= takeFromNested(player, stack, new FluidStack(resource, remaining), simulate);
            }
        }
        return resource.amount - remaining;
    }

    /**
     * Take fluid out of a single slot, using the first extractor that recognises what is in it.
     *
     * @return how much has been taken out of this slot, never more than requested
     */
    private static int takeFromStack(ItemStack stack, IInventory owner, int slot, FluidStack resource, int maxAmount,
            boolean simulate, @Nullable EntityPlayerMP player) {
        for (FluidContainerExtractor extractor : EXTRACTORS.getPlayerOrdering(player)) {
            if (!extractor.isValidSource(stack)) continue;
            FluidStack request = new FluidStack(resource, maxAmount);
            boolean converts = extractor.convertsContainer(stack, request);
            ItemStack replacement = converts ? extractor.getReplacement(stack) : null;
            // A container that turns into another item can only be replaced when it is the only item in its slot.
            if (replacement != null && stack.stackSize != 1) continue;
            int drained = extractor.drain(stack, request, simulate);
            if (drained <= 0) continue;
            if (!simulate && converts) {
                // A null replacement means the whole slot is spent, e.g. the drops of a fluid that was paid in full.
                owner.setInventorySlotContents(slot, replacement);
                owner.markDirty();
            }
            return Math.min(drained, maxAmount);
        }
        return 0;
    }

    /**
     * Look inside the given stack for an inventory, e.g. a backpack, and take fluid out of the containers it holds.
     * <p>
     * Which stacks hold an inventory is decided by {@link InventoryUtility}, which is the same registry item autoplace
     * uses, so a mod that already lets autoplace take items out of its backpacks gets the fluid containers inside them
     * for free and never has to register anything for fluid. Only one level is looked into, exactly like the item walk
     * does.
     *
     * @return how much has been taken, never more than requested
     */
    private static int takeFromNested(EntityPlayerMP player, ItemStack stack, FluidStack resource, boolean simulate) {
        int taken = 0;
        for (Iterator<? extends ItemStackExtractor> iter = InventoryUtility.getStackExtractors(player); iter
                .hasNext();) {
            ItemStackExtractor extractor = iter.next();
            if (!extractor.isValidSource(stack, player)) continue;
            IInventory nested = extractor.getInventory(stack, player);
            if (nested == null) continue;
            int fromNested = takeFromInventory(
                    nested,
                    new FluidStack(resource, resource.amount - taken),
                    simulate,
                    player,
                    -1,
                    false);
            if (fromNested <= 0) continue;
            if (!simulate) nested.markDirty();
            taken += fromNested;
            break;
        }
        return taken;
    }

    /**
     * Builtin extractor for containers implementing Forge's {@code IFluidContainerItem}, e.g. cells. Such containers
     * keep their own fluid inside their NBT, so they are drained in place, and only turn into another item when their
     * item declares a container item.
     */
    public static class ForgeFluidContainerItemExtractor implements FluidContainerExtractor {

        @Override
        public boolean isValidSource(ItemStack stack) {
            return stack != null && stack.getItem() instanceof IFluidContainerItem;
        }

        @Override
        public int drain(ItemStack container, FluidStack resource, boolean simulate) {
            IFluidContainerItem item = (IFluidContainerItem) container.getItem();
            FluidStack contained = item.getFluid(container);
            if (contained == null || !contained.isFluidEqual(resource)) return 0;
            FluidStack drained = item.drain(container, Math.min(contained.amount, resource.amount), !simulate);
            return drained == null ? 0 : Math.min(drained.amount, resource.amount);
        }

        @Override
        public boolean convertsContainer(ItemStack container, FluidStack resource) {
            Item item = container.getItem();
            if (item == null || !item.hasContainerItem(container)) return false;
            FluidStack contained = ((IFluidContainerItem) item).getFluid(container);
            // The container only turns into another item when this drain empties it for good.
            return contained != null && contained.amount <= resource.amount;
        }

        @Nullable
        @Override
        public ItemStack getReplacement(ItemStack container) {
            Item item = container.getItem();
            return item == null ? null : item.getContainerItem(container);
        }
    }

    /**
     * Builtin extractor for containers registered with Forge's {@code FluidContainerRegistry}, e.g. buckets. Such
     * containers are consumed as a whole, and are replaced by their container item.
     */
    public static class ForgeFluidContainerRegistryExtractor implements FluidContainerExtractor {

        @Override
        public boolean isValidSource(ItemStack stack) {
            return stack != null && stack.getItem() != null && FluidContainerRegistry.isFilledContainer(stack);
        }

        @Override
        public int drain(ItemStack container, FluidStack resource, boolean simulate) {
            if (container.stackSize != 1) return 0;
            FluidStack contained = FluidContainerRegistry.getFluidForFilledItem(container);
            if (contained == null || !contained.isFluidEqual(resource)) return 0;
            // A bucket cannot be half emptied. Report the whole bucket, even when only part of it is needed.
            return contained.amount;
        }

        @Override
        public boolean convertsContainer(ItemStack container, FluidStack resource) {
            return container.stackSize == 1;
        }

        @Nullable
        @Override
        public ItemStack getReplacement(ItemStack container) {
            Item item = container.getItem();
            if (item != null && item.hasContainerItem(container)) return item.getContainerItem(container);
            return null;
        }
    }
}
