package com.gtnewhorizon.structurelib.fluid;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import com.gtnewhorizon.structurelib.SortedRegistry;

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
     * Extract fluid from the main inventory of given player.
     * <p>
     * Armor slots are excluded. Containers nested inside another item, e.g. inside a backpack, are not looked into. A
     * mod that wants to support those can register its own extractor, or hand its own fluid source to autoplace through
     * its autoplace environment.
     *
     * @param player   where to take fluid from
     * @param resource what fluid to take, and how much of it at most
     * @param simulate whether to actually take the fluid
     * @return what has actually been taken. Never null, and never holding more than requested.
     */
    public static FluidStack takeFromPlayer(EntityPlayerMP player, FluidStack resource, boolean simulate) {
        // Only the main inventory, as there is no fluid container that goes into an armor slot.
        FluidStack taken = takeFromInventory(
                player.inventory,
                resource,
                simulate,
                player,
                player.inventory.mainInventory.length);
        // A fake player, e.g. one a structure preview tool uses, has no connection to sync the inventory over.
        if (!simulate && taken.amount > 0 && player.playerNetServerHandler != null) {
            player.inventoryContainer.detectAndSendChanges();
        }
        return taken;
    }

    /**
     * Extract fluid from given inventory.
     *
     * @param inv      where to take fluid from
     * @param resource what fluid to take, and how much of it at most
     * @param simulate whether to actually take the fluid
     * @return what has actually been taken. Never null, and never holding more than requested.
     */
    public static FluidStack takeFromInventory(IInventory inv, FluidStack resource, boolean simulate) {
        return takeFromInventory(inv, resource, simulate, null, -1);
    }

    private static FluidStack takeFromInventory(IInventory inv, FluidStack resource, boolean simulate,
            @Nullable EntityPlayerMP player, int maxSlot) {
        if (inv == null) throw new IllegalArgumentException();
        if (resource == null || resource.getFluid() == null) throw new IllegalArgumentException();
        int slots = maxSlot < 0 ? inv.getSizeInventory() : Math.min(maxSlot, inv.getSizeInventory());
        int remaining = resource.amount;
        for (int slot = 0; slot < slots && remaining > 0; slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack == null || stack.getItem() == null || stack.stackSize <= 0) continue;
            for (FluidContainerExtractor extractor : EXTRACTORS.getPlayerOrdering(player)) {
                if (!extractor.isValidSource(stack)) continue;
                FluidStack request = new FluidStack(resource, remaining);
                boolean converts = extractor.convertsContainer(stack, request);
                ItemStack replacement = converts ? extractor.getReplacement(stack) : null;
                // A container that turns into another item can only be replaced when it is the only item in its slot.
                if (replacement != null && stack.stackSize != 1) continue;
                int drained = extractor.drain(stack, request, simulate);
                if (drained <= 0) continue;
                remaining -= Math.min(remaining, drained);
                if (!simulate && converts) {
                    // A null replacement means the whole slot is spent, e.g. the drops of a fluid that was paid in
                    // full.
                    inv.setInventorySlotContents(slot, replacement);
                    inv.markDirty();
                }
                break;
            }
        }
        return new FluidStack(resource, resource.amount - remaining);
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
