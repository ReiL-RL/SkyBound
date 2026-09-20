package me.reil.skybound.core.shop;

import me.reil.skybound.api.shop.ShopCategory;
import me.reil.skybound.api.shop.ShopItem;
import me.reil.skybound.api.shop.ShopProvider;
import me.reil.skybound.core.config.CoreConfig;
import me.reil.skybound.core.economy.VaultEconomyProvider;
import me.reil.skybound.core.util.InventoryUtil;
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

    public enum TransactionResult {
        SUCCESS,
        NOT_AVAILABLE,
        NO_MONEY,
        NO_SPACE,
        NO_ITEMS
    }

    private final JavaPlugin plugin;
    private final CoreConfig config;
    private final VaultEconomyProvider economy;
    private final Map<String, ShopCategoryImpl> categories = new LinkedHashMap<String, ShopCategoryImpl>();
    private final Map<String, ShopItemImpl> allItems = new LinkedHashMap<String, ShopItemImpl>();
    private final Map<Material, Double> materialSellPrices = new LinkedHashMap<Material, Double>();
    private final List<ShopItemImpl> customSellItems = new ArrayList<ShopItemImpl>();

    public ShopManager(JavaPlugin plugin, CoreConfig config, VaultEconomyProvider economy) {
        this.plugin = plugin;
        this.config = config;
        this.economy = economy;
        loadShop();
    }

    public void reload() {
        categories.clear();
        allItems.clear();
        materialSellPrices.clear();
        customSellItems.clear();
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
        return buyDetailed(player, itemId, amount) == TransactionResult.SUCCESS;
    }

    public TransactionResult buyDetailed(Player player, String itemId, int amount) {
        ShopItemImpl item = allItems.get(itemId);
        if (player == null || amount <= 0) return TransactionResult.NOT_AVAILABLE;
        if (item == null || !isValidAmount(item.getBuyPrice())) return TransactionResult.NOT_AVAILABLE;

        double totalCost = item.getBuyPrice() * amount;
        if (!isValidAmount(totalCost)) return TransactionResult.NOT_AVAILABLE;
        if (!economy.has(player.getUniqueId(), totalCost)) return TransactionResult.NO_MONEY;

        if (item.getCommands().isEmpty()) {
            ItemStack stack = item.createStack(Math.min(amount, getMaxStackSize(item)));
            stack.setAmount(amount);
            if (!InventoryUtil.canFit(player.getInventory(), stack)) return TransactionResult.NO_SPACE;
            if (!economy.withdraw(player.getUniqueId(), totalCost)) return TransactionResult.NO_MONEY;
            giveItems(player, item, amount);
        } else {
            if (!economy.withdraw(player.getUniqueId(), totalCost)) return TransactionResult.NO_MONEY;
            for (String cmd : item.getCommands()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        cmd.replace("{player}", player.getName()).replace("{amount}", String.valueOf(amount)));
            }
        }
        return TransactionResult.SUCCESS;
    }

    public int countSellable(Player player, String itemId) {
        ShopItemImpl item = allItems.get(itemId);
        if (player == null || item == null || !isValidAmount(item.getSellPrice())) return 0;
        int found = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (item.matches(stack)) {
                found += stack.getAmount();
            }
        }
        return found;
    }

    @Override
    public boolean sell(Player player, String itemId, int amount) {
        return sellDetailed(player, itemId, amount) == TransactionResult.SUCCESS;
    }

    public TransactionResult sellDetailed(Player player, String itemId, int amount) {
        ShopItemImpl item = allItems.get(itemId);
        if (player == null || amount <= 0) return TransactionResult.NOT_AVAILABLE;
        if (item == null || !isValidAmount(item.getSellPrice())) return TransactionResult.NOT_AVAILABLE;

        if (!hasSimilar(player, item, amount)) return TransactionResult.NO_ITEMS;

        double totalEarned = item.getSellPrice() * amount;
        if (!isValidAmount(totalEarned)) return TransactionResult.NOT_AVAILABLE;
        if (!economy.deposit(player.getUniqueId(), totalEarned)) return TransactionResult.NOT_AVAILABLE;
        removeSimilar(player, item, amount);
        return TransactionResult.SUCCESS;
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

    public double getSellPrice(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return 0.0;
        for (ShopItemImpl item : customSellItems) {
            if (item.matches(stack)) {
                return item.getSellPrice();
            }
        }
        Double indexed = materialSellPrices.get(stack.getType());
        return indexed == null ? 0.0 : indexed.doubleValue();
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
            int slot = clampSlot(cs.getInt("slot", 0), 0, 54);

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
                    ItemStack configuredStack = loadConfiguredItem(is);
                    double buyPrice = sanitizePrice(is.getDouble("buy-price", 0.0) * config.getShopBuyMultiplier());
                    double sellPrice = sanitizePrice(is.getDouble("sell-price", 0.0) * config.getShopSellMultiplier());
                    int defaultAmount = clampAmount(is.getInt("default-amount", 1), configuredStack != null ? configuredStack.getType() : mat);
                    int itemSlot = clampSlot(is.getInt("slot", 0), 0, 45);
                    List<String> commands = sanitizeCommands(is.getStringList("commands"));
                    List<String> lore = is.getStringList("lore");

                    String fullId = catId + "_" + itemId;
                    ShopItemImpl shopItem = new ShopItemImpl(fullId, itemDisplayName, mat,
                            buyPrice, sellPrice, defaultAmount, itemSlot, commands, lore, configuredStack);
                    category.addItem(shopItem);
                    allItems.put(fullId, shopItem);
                    if (sellPrice > 0) {
                        if (configuredStack != null) {
                            customSellItems.add(shopItem);
                        } else if (!materialSellPrices.containsKey(mat)) {
                            materialSellPrices.put(mat, sellPrice);
                        }
                    }
                }
            }

            categories.put(catId, category);
        }

        plugin.getLogger().info("Loaded " + categories.size() + " shop categories, " + allItems.size() + " items.");
    }

    private ItemStack loadConfiguredItem(ConfigurationSection section) {
        ItemStack stack = section.getItemStack("item");
        if (stack == null) {
            Object raw = section.get("item");
            if (raw instanceof ItemStack) {
                stack = (ItemStack) raw;
            }
        }
        if (stack == null || stack.getType() == Material.AIR) return null;
        ItemStack copy = stack.clone();
        copy.setAmount(clampAmount(copy.getAmount(), copy.getType()));
        return copy;
    }

    private boolean hasSimilar(Player player, ShopItemImpl item, int amount) {
        int found = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (item.matches(stack)) {
                found += stack.getAmount();
                if (found >= amount) return true;
            }
        }
        return false;
    }

    private void removeSimilar(Player player, ShopItemImpl item, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (!item.matches(stack)) continue;

            int take = Math.min(stack.getAmount(), remaining);
            stack.setAmount(stack.getAmount() - take);
            remaining -= take;
            if (stack.getAmount() <= 0) {
                contents[i] = null;
            }
        }
        player.getInventory().setStorageContents(contents);
    }

    private void giveItems(Player player, ShopItemImpl item, int amount) {
        int remaining = amount;
        int maxStack = getMaxStackSize(item);
        while (remaining > 0) {
            int take = Math.min(maxStack, remaining);
            player.getInventory().addItem(item.createStack(take));
            remaining -= take;
        }
    }

    private int getMaxStackSize(ShopItemImpl item) {
        Material material = item == null ? null : item.getMaterial();
        return material == null ? 64 : Math.max(1, material.getMaxStackSize());
    }

    private boolean isValidAmount(double amount) {
        return amount > 0.0 && !Double.isNaN(amount) && !Double.isInfinite(amount);
    }

    private double sanitizePrice(double price) {
        if (Double.isNaN(price) || Double.isInfinite(price) || price < 0.0) {
            return 0.0;
        }
        return price;
    }

    private int clampAmount(int amount, Material material) {
        int max = material == null ? 64 : Math.max(1, material.getMaxStackSize());
        if (amount < 1) return 1;
        return Math.min(amount, max);
    }

    private int clampSlot(int slot, int fallback, int maxExclusive) {
        return slot < 0 || slot >= maxExclusive ? fallback : slot;
    }

    private List<String> sanitizeCommands(List<String> raw) {
        List<String> commands = new ArrayList<String>();
        for (String command : raw) {
            if (command == null) continue;
            String sanitized = command.trim();
            if (sanitized.startsWith("/")) {
                sanitized = sanitized.substring(1).trim();
            }
            if (sanitized.length() > 256) {
                plugin.getLogger().warning("Ignoring overlong shop command.");
                continue;
            }
            if (!sanitized.isEmpty()) {
                commands.add(sanitized);
            }
        }
        return commands;
    }
}
