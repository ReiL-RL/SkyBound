package me.reil.skybound.core.shop;

import me.reil.skybound.api.shop.ShopCategory;
import me.reil.skybound.api.shop.ShopItem;
import me.reil.skybound.api.shop.ShopProvider;
import me.reil.skybound.core.config.CoreConfig;
import me.reil.skybound.core.economy.VaultEconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ShopManager implements ShopProvider {

    private final JavaPlugin plugin;
    private final CoreConfig config;
    private final VaultEconomyProvider economy;
    private final Map<String, ShopCategoryImpl> categories = new LinkedHashMap<String, ShopCategoryImpl>();
    private final Map<String, ShopItemImpl> allItems = new LinkedHashMap<String, ShopItemImpl>();

    public ShopManager(JavaPlugin plugin, CoreConfig config, VaultEconomyProvider economy) {
        this.plugin = plugin;
        this.config = config;
        this.economy = economy;
        loadShop();
    }

    public void reload() {
        categories.clear();
        allItems.clear();
        loadShop();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<ShopCategory> getCategories() {
        return Collections.unmodifiableCollection((Collection<? extends ShopCategory>) (Collection<?>) categories.values());
    }

    @Override
    public ShopCategory getCategory(String categoryId) {
        return categories.get(categoryId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<ShopItem> getItems(String categoryId) {
        ShopCategoryImpl category = categories.get(categoryId);
        if (category == null) return Collections.emptyList();
        return Collections.unmodifiableCollection((Collection<? extends ShopItem>) (Collection<?>) category.getItemImpls());
    }

    @Override
    public boolean buy(Player player, String itemId, int amount) {
        ShopItemImpl item = allItems.get(itemId);
        if (item == null || item.getBuyPrice() <= 0) return false;

        double totalCost = item.getBuyPrice() * amount;
        if (!economy.has(player.getUniqueId(), totalCost)) return false;

        economy.withdraw(player.getUniqueId(), totalCost);

        if (item.getCommands().isEmpty()) {
            ItemStack stack = new ItemStack(item.getMaterial(), amount);
            player.getInventory().addItem(stack);
        } else {
            for (String cmd : item.getCommands()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        cmd.replace("{player}", player.getName()).replace("{amount}", String.valueOf(amount)));
            }
        }
        return true;
    }

    @Override
    public boolean sell(Player player, String itemId, int amount) {
        ShopItemImpl item = allItems.get(itemId);
        if (item == null || item.getSellPrice() <= 0) return false;

        ItemStack toRemove = new ItemStack(item.getMaterial(), amount);
        if (!player.getInventory().containsAtLeast(toRemove, amount)) return false;

        player.getInventory().removeItem(toRemove);
        double totalEarned = item.getSellPrice() * amount;
        economy.deposit(player.getUniqueId(), totalEarned);
        return true;
    }

    @Override
    public double getBuyPrice(String itemId) {
        ShopItemImpl item = allItems.get(itemId);
        return item == null ? 0.0 : item.getBuyPrice();
    }

    @Override
    public double getSellPrice(String itemId) {
        ShopItemImpl item = allItems.get(itemId);
        return item == null ? 0.0 : item.getSellPrice();
    }

    private void loadShop() {
        File file = new File(plugin.getDataFolder(), "shop.yml");
        if (!file.exists()) {
            plugin.saveResource("shop.yml", false);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection catSection = cfg.getConfigurationSection("categories");
        if (catSection == null) return;

        for (String catId : catSection.getKeys(false)) {
            ConfigurationSection cs = catSection.getConfigurationSection(catId);
            if (cs == null) continue;

            String displayName = cs.getString("display-name", catId);
            Material icon = Material.matchMaterial(cs.getString("icon", "CHEST"));
            if (icon == null) icon = Material.CHEST;
            int slot = cs.getInt("slot", 0);

            ShopCategoryImpl category = new ShopCategoryImpl(catId, displayName,
                    Collections.<String>emptyList(), icon, slot);

            ConfigurationSection itemsSection = cs.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String itemId : itemsSection.getKeys(false)) {
                    ConfigurationSection is = itemsSection.getConfigurationSection(itemId);
                    if (is == null) continue;

                    String itemDisplayName = is.getString("display-name", itemId);
                    Material mat = Material.matchMaterial(is.getString("material", "STONE"));
                    if (mat == null) mat = Material.STONE;
                    double buyPrice = is.getDouble("buy-price", 0.0);
                    double sellPrice = is.getDouble("sell-price", 0.0);
                    int defaultAmount = is.getInt("default-amount", 1);
                    int itemSlot = is.getInt("slot", 0);
                    List<String> commands = is.getStringList("commands");
                    List<String> lore = is.getStringList("lore");

                    String fullId = catId + "_" + itemId;
                    ShopItemImpl shopItem = new ShopItemImpl(fullId, itemDisplayName, mat,
                            buyPrice, sellPrice, defaultAmount, itemSlot, commands, lore);
                    category.addItem(shopItem);
                    allItems.put(fullId, shopItem);
                }
            }

            categories.put(catId, category);
        }

        plugin.getLogger().info("Loaded " + categories.size() + " shop categories, " + allItems.size() + " items.");
    }
}
