package me.reil.skybound.core.island;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prestige Shop: items that can be purchased for prestige tokens.
 * Tokens are earned by prestiging the island. Each prestige level grants
 * 1 token (configurable). Items defined in prestige-shop.yml.
 *
 * Rewards are built via the Bukkit API (version-safe) rather than raw /give
 * commands, because vanilla NBT/component syntax differs between MC versions
 * (the {NBT} form was replaced by [components] in 1.20.5+). Overflow that does
 * not fit the inventory is dropped at the player's feet.
 */
public final class PrestigeShopManager {

    private static final int CURRENT_SHOP_VERSION = 2;

    /** A single concrete item handed out when an entry is purchased. */
    public static final class Reward {
        public final Material material;
        public final int amount;
        public final List<String> enchantments; // "key:level"
        public final EntityType spawnerType;     // nullable
        public final int fireworkFlight;         // -1 if not a firework

        public Reward(Material material, int amount, List<String> enchantments,
                      EntityType spawnerType, int fireworkFlight) {
            this.material = material;
            this.amount = amount;
            this.enchantments = enchantments;
            this.spawnerType = spawnerType;
            this.fireworkFlight = fireworkFlight;
        }
    }

    public static final class PrestigeItem {
        public final String id;
        public final Material material;
        public final String displayName;
        public final List<String> lore;
        public final int cost;
        public final List<Reward> rewards;  // items built & given via Bukkit API
        public final List<String> commands; // console commands (e.g. LuckPerms), {player} substituted

        public PrestigeItem(String id, Material material, String displayName,
                            List<String> lore, int cost, List<Reward> rewards, List<String> commands) {
            this.id = id;
            this.material = material;
            this.displayName = displayName;
            this.lore = lore;
            this.cost = cost;
            this.rewards = rewards;
            this.commands = commands;
        }
    }

    private final JavaPlugin plugin;
    private final Map<String, PrestigeItem> items = new LinkedHashMap<String, PrestigeItem>();
    /** islandId -> available tokens to spend in prestige-shop. */
    private final Map<String, Integer> tokens = new LinkedHashMap<String, Integer>();
    private final File dataFile;

    public PrestigeShopManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data/prestige-tokens.yml");
        loadShop();
        loadTokens();
    }

    public Map<String, PrestigeItem> getItems() {
        return Collections.unmodifiableMap(items);
    }

    public int getTokens(String islandId) {
        Integer t = tokens.get(islandId);
        return t == null ? 0 : t;
    }

    public void addTokens(String islandId, int amount) {
        if (islandId == null || islandId.isEmpty() || amount <= 0) return;
        Integer cur = tokens.get(islandId);
        long next = (long) (cur == null ? 0 : cur) + amount;
        tokens.put(islandId, next > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) next);
        saveTokens();
    }

    public boolean spendTokens(String islandId, int amount) {
        if (islandId == null || islandId.isEmpty() || amount <= 0) return false;
        int cur = getTokens(islandId);
        if (cur < amount) return false;
        int remaining = cur - amount;
        if (remaining <= 0) {
            tokens.remove(islandId);
        } else {
            tokens.put(islandId, remaining);
        }
        saveTokens();
        return true;
    }

    /** Set token balance directly (admin). Negative values are clamped to 0. */
    public void setTokens(String islandId, int amount) {
        if (amount <= 0) {
            tokens.remove(islandId);
        } else {
            tokens.put(islandId, amount);
        }
        saveTokens();
    }

    /** Remove up to {@code amount} tokens (admin take). Never goes below 0. */
    public void removeTokens(String islandId, int amount) {
        if (amount <= 0) return;
        int cur = getTokens(islandId);
        setTokens(islandId, Math.max(0, cur - amount));
    }

    public void removeIsland(String islandId) {
        if (islandId == null || islandId.isEmpty()) return;
        if (tokens.remove(islandId) != null) {
            saveTokens();
        }
    }

    private void loadShop() {
        items.clear();
        File file = new File(plugin.getDataFolder(), "prestige-shop.yml");
        boolean haveResource = plugin.getResource("prestige-shop.yml") != null;

        if (!file.exists()) {
            if (haveResource) {
                plugin.saveResource("prestige-shop.yml", false);
            } else {
                writeMinimalDefault(file);
            }
        } else if (haveResource) {
            // Auto-migrate old configs (e.g. command-based v1) to the bundled version.
            YamlConfiguration existing = YamlConfiguration.loadConfiguration(file);
            if (existing.getInt("config-version", 1) < CURRENT_SHOP_VERSION) {
                File backup = new File(plugin.getDataFolder(), "prestige-shop.yml.old");
                if (backup.exists()) backup.delete();
                if (file.renameTo(backup)) {
                    plugin.saveResource("prestige-shop.yml", false);
                    plugin.getLogger().info("prestige-shop.yml updated to v" + CURRENT_SHOP_VERSION
                            + " (your old file was saved as prestige-shop.yml.old).");
                }
            }
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = cfg.getConfigurationSection("items");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            String matStr = sec.getString(id + ".material", "STONE");
            Material mat;
            try { mat = Material.valueOf(matStr.toUpperCase()); }
            catch (IllegalArgumentException e) { mat = Material.STONE; }
            String name = sec.getString(id + ".name", id);
            List<String> lore = sec.getStringList(id + ".lore");
            int cost = sec.getInt(id + ".cost", 1);
            if (cost < 1) cost = 1;
            List<String> commands = sanitizeCommands(sec.getStringList(id + ".commands"));
            List<Reward> rewards = parseRewards(sec.getMapList(id + ".rewards"));
            items.put(id, new PrestigeItem(id, mat, name, lore, cost, rewards, commands));
        }
        plugin.getLogger().info("Loaded " + items.size() + " prestige shop items.");
    }

    private List<Reward> parseRewards(List<Map<?, ?>> rewardMaps) {
        List<Reward> rewards = new ArrayList<Reward>();
        if (rewardMaps == null) return rewards;
        for (Map<?, ?> rm : rewardMaps) {
            String matStr = str(rm.get("material"));
            if (matStr == null) continue;
            Material rmat;
            try { rmat = Material.valueOf(matStr.toUpperCase()); }
            catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("prestige-shop.yml: unknown material '" + matStr + "', skipping reward.");
                continue;
            }
            int amount = (rm.get("amount") instanceof Number) ? ((Number) rm.get("amount")).intValue() : 1;
            amount = Math.max(1, Math.min(amount, rmat.getMaxStackSize()));

            List<String> ench = new ArrayList<String>();
            Object eo = rm.get("enchantments");
            if (eo instanceof List) {
                for (Object o : (List<?>) eo) ench.add(String.valueOf(o));
            }

            EntityType spawner = null;
            String sp = str(rm.get("spawner"));
            if (sp != null) {
                try { spawner = EntityType.valueOf(sp.toUpperCase()); }
                catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("prestige-shop.yml: unknown spawner entity '" + sp + "'.");
                }
            }

            int flight = (rm.get("flight") instanceof Number) ? ((Number) rm.get("flight")).intValue() : -1;
            rewards.add(new Reward(rmat, amount, ench, spawner, flight));
        }
        return rewards;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private void writeMinimalDefault(File file) {
        try {
            file.getParentFile().mkdirs();
            YamlConfiguration y = new YamlConfiguration();
            y.set("config-version", CURRENT_SHOP_VERSION);
            y.set("items.beacon.material", "BEACON");
            y.set("items.beacon.name", "&5Маяк престижа");
            y.set("items.beacon.lore", java.util.Arrays.asList("&7Эксклюзив для престижей"));
            y.set("items.beacon.cost", 1);
            Map<String, Object> rw = new LinkedHashMap<String, Object>();
            rw.put("material", "BEACON");
            rw.put("amount", 1);
            y.set("items.beacon.rewards", java.util.Arrays.asList(rw));
            me.reil.skybound.core.storage.YamlFiles.saveAtomically(plugin, y, file, "prestige-shop.yml");
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Could not create default prestige-shop.yml: " + e.getMessage());
        }
    }

    // === Granting purchases ===

    /**
     * Hand a purchased entry to the player: build & give every reward (overflow
     * dropped at the player's feet), then run any console commands.
     */
    public void grant(Player player, PrestigeItem item) {
        if (item == null || player == null) return;
        for (Reward r : item.rewards) {
            ItemStack stack = buildReward(r);
            if (stack != null) giveOrDrop(player, stack);
        }
        for (String cmd : item.commands) {
            String resolved = cmd.replace("{player}", player.getName());
            if (!resolved.isEmpty()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
            }
        }
    }

    /** Add to inventory; whatever doesn't fit is dropped naturally at the player. */
    private void giveOrDrop(Player player, ItemStack stack) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        for (ItemStack rem : leftover.values()) {
            if (rem != null && rem.getAmount() > 0) {
                player.getWorld().dropItemNaturally(player.getLocation(), rem);
            }
        }
    }

    private ItemStack buildReward(Reward r) {
        if (r == null || r.material == null) return null;
        ItemStack item = new ItemStack(r.material, Math.max(1, r.amount));

        if (r.enchantments != null && !r.enchantments.isEmpty()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                for (String e : r.enchantments) {
                    if (e == null) continue;
                    String[] parts = e.split(":");
                    if (parts.length < 2) continue;
                    Enchantment ench = resolveEnchantment(parts[0].trim());
                    int lvl;
                    try { lvl = Integer.parseInt(parts[1].trim()); }
                    catch (NumberFormatException ex) { continue; }
                    if (lvl < 1) continue;
                    if (ench != null) meta.addEnchant(ench, lvl, true);
                }
                item.setItemMeta(meta);
            }
        }

        if (r.spawnerType != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof BlockStateMeta) {
                BlockStateMeta bsm = (BlockStateMeta) meta;
                BlockState bs = bsm.getBlockState();
                if (bs instanceof CreatureSpawner) {
                    ((CreatureSpawner) bs).setSpawnedType(r.spawnerType);
                    bsm.setBlockState(bs);
                    item.setItemMeta(bsm);
                }
            }
        }

        if (r.fireworkFlight >= 0) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof FireworkMeta) {
                ((FireworkMeta) meta).setPower(r.fireworkFlight);
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    private static Enchantment resolveEnchantment(String name) {
        if (name == null || name.isEmpty()) return null;
        try {
            Enchantment e = Enchantment.getByKey(NamespacedKey.minecraft(name.toLowerCase()));
            if (e != null) return e;
        } catch (Throwable ignored) {}
        try {
            Enchantment e = Enchantment.getByName(name.toUpperCase());
            if (e != null) return e;
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Force the vanilla command for item-giving (only relevant for command-based
     * entries); EssentialsX otherwise hijacks /give.
     */
    private static String normalizeCommand(String cmd) {
        if (cmd == null) return "";
        String trimmed = cmd.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1).trim();
        }
        if (trimmed.toLowerCase().startsWith("give ")) {
            return "minecraft:" + trimmed;
        }
        return trimmed;
    }

    private List<String> sanitizeCommands(List<String> raw) {
        List<String> commands = new ArrayList<String>();
        for (String command : raw) {
            String sanitized = normalizeCommand(command);
            if (sanitized.length() > 256) {
                plugin.getLogger().warning("Ignoring overlong prestige-shop command.");
                continue;
            }
            if (!sanitized.isEmpty()) {
                commands.add(sanitized);
            }
        }
        return commands;
    }

    private void loadTokens() {
        tokens.clear();
        if (!dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection sec = cfg.getConfigurationSection("tokens");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            int value = sec.getInt(key);
            if (value > 0) {
                tokens.put(key, value);
            }
        }
    }

    public void saveTokens() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, Integer> e : tokens.entrySet()) {
            if (e.getValue() != null && e.getValue() > 0) {
                cfg.set("tokens." + e.getKey(), e.getValue());
            }
        }
        me.reil.skybound.core.storage.YamlFiles.saveAtomically(plugin, cfg, dataFile, "prestige-tokens.yml");
    }
}
