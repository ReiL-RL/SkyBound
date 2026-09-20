package me.reil.skybound.core.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class InventoryUtil {

    private InventoryUtil() {}

    public static boolean canFit(PlayerInventory inventory, ItemStack stack) {
        if (inventory == null || stack == null || stack.getAmount() <= 0) return false;

        int remaining = stack.getAmount();
        ItemStack[] contents = inventory.getStorageContents();
        for (ItemStack current : contents) {
            if (current == null || current.getType().isAir()) {
                remaining -= Math.min(stack.getMaxStackSize(), remaining);
            } else if (current.isSimilar(stack)) {
                remaining -= Math.max(0, current.getMaxStackSize() - current.getAmount());
            }
            if (remaining <= 0) return true;
        }
        return false;
    }
}
