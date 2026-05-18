package me.reil.skybound.core.shop;

import me.reil.skybound.api.shop.ShopCategory;
import me.reil.skybound.api.shop.ShopItem;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ShopCategoryImpl implements ShopCategory {

    private final String id;
    private final String displayName;
    private final List<String> description;
    private final Material icon;
    private final int slot;
    private final List<ShopItemImpl> items = new ArrayList<ShopItemImpl>();

    public ShopCategoryImpl(String id, String displayName, List<String> description, Material icon, int slot) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.slot = slot;
    }

    @Override public String getId() { return id; }
    @Override public String getDisplayName() { return displayName; }
    @Override public List<String> getDescription() { return description; }
    @Override public Material getIcon() { return icon; }
    @Override public int getSlot() { return slot; }

    @Override
    @SuppressWarnings("unchecked")
    public List<ShopItem> getItems() {
        return Collections.unmodifiableList((List<? extends ShopItem>) (List<?>) items);
    }

    public List<ShopItemImpl> getItemImpls() { return items; }

    public void addItem(ShopItemImpl item) { items.add(item); }
}
