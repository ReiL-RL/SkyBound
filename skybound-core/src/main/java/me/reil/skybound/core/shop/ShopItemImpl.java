package me.reil.skybound.core.shop;

import me.reil.skybound.api.shop.ShopItem;
import org.bukkit.Material;

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

    public ShopItemImpl(String id, String displayName, Material material, double buyPrice, double sellPrice, int defaultAmount, int slot, List<String> commands, List<String> lore) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.defaultAmount = defaultAmount;
        this.slot = slot;
        this.commands = commands != null ? commands : Collections.<String>emptyList();
        this.lore = lore != null ? lore : Collections.<String>emptyList();
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
}
