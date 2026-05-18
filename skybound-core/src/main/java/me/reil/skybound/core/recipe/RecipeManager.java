package me.reil.skybound.core.recipe;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads and registers custom recipes from recipes.yml.
 * Supports shaped and shapeless recipes with custom item names.
 */
public final class RecipeManager {

    private final JavaPlugin plugin;
    private final List<NamespacedKey> registeredKeys = new ArrayList<NamespacedKey>();

    public RecipeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadRecipes();
    }

    public void reload() {
        // Remove old recipes
        for (NamespacedKey key : registeredKeys) {
            Bukkit.removeRecipe(key);
        }
        registeredKeys.clear();
        loadRecipes();
    }

    public void shutdown() {
        for (NamespacedKey key : registeredKeys) {
            Bukkit.removeRecipe(key);
        }
        registeredKeys.clear();
    }

    private void loadRecipes() {
        File file = new File(plugin.getDataFolder(), "recipes.yml");
        if (!file.exists()) {
            plugin.saveResource("recipes.yml", false);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection recipesSection = cfg.getConfigurationSection("recipes");
        if (recipesSection == null) return;

        int count = 0;
        for (String key : recipesSection.getKeys(false)) {
            ConfigurationSection rs = recipesSection.getConfigurationSection(key);
            if (rs == null) continue;

            String type = rs.getString("type", "shaped");
            NamespacedKey nsKey = new NamespacedKey(plugin, "skybound_" + key);

            try {
                if ("shaped".equalsIgnoreCase(type)) {
                    registerShaped(nsKey, rs);
                    count++;
                } else if ("shapeless".equalsIgnoreCase(type)) {
                    registerShapeless(nsKey, rs);
                    count++;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load recipe '" + key + "': " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + count + " custom recipes.");
    }

    private void registerShaped(NamespacedKey key, ConfigurationSection section) {
        ItemStack result = parseResult(section.getConfigurationSection("result"));
        if (result == null) return;

        List<String> shape = section.getStringList("shape");
        if (shape.isEmpty()) return;

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(shape.toArray(new String[0]));

        ConfigurationSection ingredients = section.getConfigurationSection("ingredients");
        if (ingredients != null) {
            for (String ingKey : ingredients.getKeys(false)) {
                String matName = ingredients.getString(ingKey, "STONE");
                Material mat = parseMaterial(matName);
                if (mat != null && ingKey.length() == 1) {
                    recipe.setIngredient(ingKey.charAt(0), mat);
                }
            }
        }

        Bukkit.addRecipe(recipe);
        registeredKeys.add(key);
    }

    private void registerShapeless(NamespacedKey key, ConfigurationSection section) {
        ItemStack result = parseResult(section.getConfigurationSection("result"));
        if (result == null) return;

        ShapelessRecipe recipe = new ShapelessRecipe(key, result);

        List<String> ingredientList = section.getStringList("ingredients");
        for (String matName : ingredientList) {
            Material mat = parseMaterial(matName);
            if (mat != null) {
                recipe.addIngredient(mat);
            }
        }

        Bukkit.addRecipe(recipe);
        registeredKeys.add(key);
    }

    private ItemStack parseResult(ConfigurationSection section) {
        if (section == null) return null;

        String matName = section.getString("material", "STONE");
        Material mat = parseMaterial(matName);
        if (mat == null) return null;

        int amount = section.getInt("amount", 1);
        ItemStack item = new ItemStack(mat, amount);

        String name = section.getString("name");
        if (name != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                item.setItemMeta(meta);
            }
        }

        List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<String> coloredLore = new ArrayList<String>();
                for (String line : lore) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(coloredLore);
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    private Material parseMaterial(String name) {
        if (name == null) return null;
        // Support sop:item_id syntax (future SopItems integration)
        if (name.startsWith("sop:")) {
            // SopItems integration would go here
            // For now, fall back to null
            plugin.getLogger().warning("SopItems recipe ingredient not yet supported: " + name);
            return null;
        }
        return Material.matchMaterial(name.toUpperCase());
    }
}
