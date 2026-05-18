package me.reil.skybound.core.menu;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Island creation menu — player selects an island type/schematic.
 * Opens when player does /is without having an island.
 */
public final class IslandCreateMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final List<SchematicOption> options = new ArrayList<SchematicOption>();

    public IslandCreateMenu(Player player, SkyBoundPlugin plugin) {
        super(player);
        this.plugin = plugin;
        loadOptions();
    }

    @Override
    public String getTitle() {
        return lang().get("menu.create.title");
    }

    @Override
    public int getSize() {
        return 45;
    }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());

        // Fill border with glass
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < getSize(); i++) {
            inventory.setItem(i, border);
        }

        // Place schematic options
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < options.size() && i < slots.length; i++) {
            SchematicOption opt = options.get(i);
            ItemStack item = new ItemStack(opt.icon);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', opt.displayName));
                List<String> lore = new ArrayList<String>();
                lore.add("");
                for (String line : opt.description) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                lore.add("");
                lore.add(ChatColor.YELLOW + "\u25B6 Click to create!");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slots[i], item);
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

        for (int i = 0; i < options.size() && i < slots.length; i++) {
            if (slot == slots[i]) {
                SchematicOption opt = options.get(i);
                player.closeInventory();

                // Create island
                Island island = plugin.getIslandManager().createIsland(player, opt.schematicFile);
                if (island != null) {
                    plugin.getSchematicService().paste(opt.schematicFile, island.getCenter());

                    // Remember schematic name for regen
                    ((me.reil.skybound.core.island.IslandImpl) island).setSchematicName(opt.schematicFile);

                    // Find safe spawn: highest block at center + 1
                    org.bukkit.Location home = island.getCenter().clone();
                    home.setY(home.getWorld().getHighestBlockYAt(home.getBlockX(), home.getBlockZ()) + 1);
                    home.setX(home.getBlockX() + 0.5);
                    home.setZ(home.getBlockZ() + 0.5);
                    island.setHome(home);

                    player.teleport(island.getHome());
                    player.sendMessage("");
                    player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "\u2726 Island Created! \u2726");
                    player.sendMessage(ChatColor.GRAY + "Type: " + ChatColor.translateAlternateColorCodes('&', opt.displayName));
                    player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/is help " + ChatColor.GRAY + "to see all commands.");
                    player.sendMessage("");
                } else {
                    player.sendMessage(ChatColor.RED + "Could not create island.");
                }
                return;
            }
        }
    }

    private void loadOptions() {
        File file = new File(plugin.getDataFolder(), "schematics.yml");
        if (!file.exists()) {
            plugin.saveResource("schematics.yml", false);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("schematics");
        if (section == null) {
            // Fallback defaults
            options.add(new SchematicOption("default", "&a\u2726 Classic Island", Material.GRASS_BLOCK, new String[]{"&7A classic skyblock island", "&7with a tree and chest."}, "default.schem"));
            options.add(new SchematicOption("desert", "&e\u2726 Desert Island", Material.SAND, new String[]{"&7A sandy island", "&7with a cactus."}, "desert.schem"));
            options.add(new SchematicOption("winter", "&b\u2726 Winter Island", Material.SNOW_BLOCK, new String[]{"&7A frozen island", "&7with ice and snow."}, "winter.schem"));
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection ss = section.getConfigurationSection(key);
            if (ss == null) continue;

            String displayName = ss.getString("display-name", key);
            Material icon = Material.matchMaterial(ss.getString("icon", "GRASS_BLOCK"));
            if (icon == null) icon = Material.GRASS_BLOCK;
            List<String> desc = ss.getStringList("description");
            String schematicFile = ss.getString("file", key + ".schem");

            options.add(new SchematicOption(key, displayName, icon, desc.toArray(new String[0]), schematicFile));
        }
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static final class SchematicOption {
        final String id;
        final String displayName;
        final Material icon;
        final String[] description;
        final String schematicFile;

        SchematicOption(String id, String displayName, Material icon, String[] description, String schematicFile) {
            this.id = id;
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
            this.schematicFile = schematicFile;
        }
    }
}
