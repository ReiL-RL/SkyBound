package me.reil.skybound.core.shop;

import me.reil.skybound.api.shop.ShopItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ShopItemImpl implements ShopItem {

    private final String id;
    private final String displayName;
    private final Material material;
    private final double buyPrice;
    private final double sellPrice;
    private final int defaultAmount;
    private final int slot;
    private final List<String> commands;
    private final List<String> lore;
    private final ItemStack itemStack;

    public ShopItemImpl(String id, String displayName, Material material, double buyPrice, double sellPrice, int defaultAmount, int slot, List<String> commands, List<String> lore) {
        this(id, displayName, material, buyPrice, sellPrice, defaultAmount, slot, commands, lore, null);
    }

    public ShopItemImpl(String id, String displayName, Material material, double buyPrice, double sellPrice, int defaultAmount, int slot, List<String> commands, List<String> lore, ItemStack itemStack) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.defaultAmount = defaultAmount;
        this.slot = slot;
        this.commands = commands != null ? Collections.unmodifiableList(new ArrayList<String>(commands)) : Collections.<String>emptyList();
        this.lore = lore != null ? Collections.unmodifiableList(new ArrayList<String>(lore)) : Collections.<String>emptyList();
        this.itemStack = itemStack != null ? itemStack.clone() : null;
    }

    @Override public String getId() { return id; }
    @Override public String getDisplayName() { return displayName; }
    @Override public Material getMaterial() { return material; }
    @Override public double getBuyPrice() { return buyPrice; }
    @Override public double getSellPrice() { return sellPrice; }
    @Override public int getDefaultAmount() { return defaultAmount; }
    @Override public int getSlot() { return slot; }
    @Override public List<String> getCommands() { return commands; }
    @Override public List<String> getLore() { return lore; }

    public ItemStack createStack(int amount) {
        ItemStack stack = itemStack != null ? itemStack.clone() : new ItemStack(material);
        stack.setAmount(amount);
        return stack;
    }

    public boolean matches(ItemStack stack) {
        if (stack == null) return false;
        ItemStack template = itemStack != null ? itemStack : new ItemStack(material);
        return stack.isSimilar(template);
    }
}
