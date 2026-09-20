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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            NamespacedKey nsKey = new NamespacedKey(plugin, "skybound_" + sanitizeKey(key));

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

        List<String> shape = normalizeShape(section.getStringList("shape"));
        if (shape.isEmpty()) return;

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(shape.toArray(new String[0]));

        ConfigurationSection ingredients = section.getConfigurationSection("ingredients");
        Set<Character> required = getShapeKeys(shape);
        if (required.isEmpty()) return;
        Set<Character> ingredientsSet = new HashSet<Character>();
        if (ingredients != null) {
            for (String ingKey : ingredients.getKeys(false)) {
                String matName = ingredients.getString(ingKey, "STONE");
                Material mat = parseMaterial(matName);
                char ingredientKey = ingKey.length() == 1 ? ingKey.charAt(0) : '\0';
                if (mat != null && required.contains(ingredientKey)) {
                    recipe.setIngredient(ingredientKey, mat);
                    ingredientsSet.add(ingredientKey);
                }
            }
        }
        if (!ingredientsSet.containsAll(required)) return;

        Bukkit.addRecipe(recipe);
        registeredKeys.add(key);
    }

    private void registerShapeless(NamespacedKey key, ConfigurationSection section) {
        ItemStack result = parseResult(section.getConfigurationSection("result"));
        if (result == null) return;

        ShapelessRecipe recipe = new ShapelessRecipe(key, result);

        List<String> ingredientList = section.getStringList("ingredients");
        int ingredients = 0;
        for (String matName : ingredientList) {
            Material mat = parseMaterial(matName);
            if (mat != null) {
                recipe.addIngredient(mat);
                ingredients++;
            }
        }
        if (ingredients == 0) return;

        Bukkit.addRecipe(recipe);
        registeredKeys.add(key);
    }

    private ItemStack parseResult(ConfigurationSection section) {
        if (section == null) return null;

        String matName = section.getString("material", "STONE");
        Material mat = parseMaterial(matName);
        if (mat == null) return null;

        int amount = section.getInt("amount", 1);
        amount = Math.max(1, Math.min(amount, mat.getMaxStackSize()));
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

    private List<String> normalizeShape(List<String> raw) {
        List<String> shape = new ArrayList<String>();
        int width = -1;
        for (String row : raw) {
            if (row == null) continue;
            if (row.length() < 1 || row.length() > 3) return Collections.emptyList();
            if (width == -1) {
                width = row.length();
            } else if (row.length() != width) {
                return Collections.emptyList();
            }
            shape.add(row);
            if (shape.size() > 3) return Collections.emptyList();
        }
        return shape;
    }

    private Set<Character> getShapeKeys(List<String> shape) {
        Set<Character> keys = new HashSet<Character>();
        for (String row : shape) {
            for (int i = 0; i < row.length(); i++) {
                char ch = row.charAt(i);
                if (ch != ' ') {
                    keys.add(ch);
                }
            }
        }
        return keys;
    }

    private String sanitizeKey(String raw) {
        String lower = raw == null ? "recipe" : raw.toLowerCase();
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lower.length(); i++) {
            char ch = lower.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_' || ch == '-' || ch == '.') {
                out.append(ch);
            } else {
                out.append('_');
            }
        }
        return out.length() == 0 ? "recipe" : out.toString();
    }
}
