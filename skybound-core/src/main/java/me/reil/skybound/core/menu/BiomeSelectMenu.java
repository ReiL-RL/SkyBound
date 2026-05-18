package me.reil.skybound.core.menu;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Biome selection menu for changing island biome via GUI.
 */
public final class BiomeSelectMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final Island island;

    private static final BiomeOption[] BIOMES = {
            new BiomeOption("PLAINS", Material.GRASS_BLOCK, "&aPlains", "&7Classic green biome."),
            new BiomeOption("DESERT", Material.SAND, "&eDesert", "&7Hot and sandy."),
            new BiomeOption("FOREST", Material.OAK_SAPLING, "&2Forest", "&7Trees and greenery."),
            new BiomeOption("JUNGLE", Material.JUNGLE_SAPLING, "&aJungle", "&7Dense tropical forest."),
            new BiomeOption("SNOWY_TAIGA", Material.SNOW_BLOCK, "&bWinter", "&7Cold and snowy."),
            new BiomeOption("MUSHROOM_FIELDS", Material.RED_MUSHROOM_BLOCK, "&dMushroom", "&7Mycelium island."),
            new BiomeOption("BADLANDS", Material.RED_SAND, "&6Badlands", "&7Mesa with terracotta."),
            new BiomeOption("FLOWER_FOREST", Material.POPPY, "&dFlower Forest", "&7Colorful flowers."),
            new BiomeOption("DARK_FOREST", Material.DARK_OAK_SAPLING, "&8Dark Forest", "&7Thick dark trees."),
            new BiomeOption("SWAMP", Material.LILY_PAD, "&2Swamp", "&7Murky waters."),
            new BiomeOption("OCEAN", Material.WATER_BUCKET, "&9Ocean", "&7Deep blue water."),
            new BiomeOption("THE_VOID", Material.OBSIDIAN, "&8Void", "&7Empty darkness."),
    };

    public BiomeSelectMenu(Player player, SkyBoundPlugin plugin, Island island) {
        super(player);
        this.plugin = plugin;
        this.island = island;
    }

    @Override
    public String getTitle() {
        return lang().get("menu.biome.title");
    }

    @Override
    public int getSize() {
        return 36;
    }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());

        ItemStack border = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 27; i < 36; i++) inventory.setItem(i, border);

        int slot = 9;
        for (BiomeOption opt : BIOMES) {
            if (slot >= 27) break;
            ItemStack item = new ItemStack(opt.icon);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', opt.displayName));
                List<String> lore = new ArrayList<String>();
                lore.add(ChatColor.translateAlternateColorCodes('&', opt.description));
                lore.add("");
                lore.add(ChatColor.YELLOW + "\u25B6 Click to apply");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot++, item);
        }

        inventory.setItem(31, makeItem(Material.ARROW, ChatColor.RED + "Back"));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        if (slot == 31) {
            new IslandSettingsMenu(player, plugin, island).open();
            return;
        }

        int index = slot - 9;
        if (index < 0 || index >= BIOMES.length) return;

        BiomeOption opt = BIOMES[index];
        Biome biome = plugin.getBiomeService().parseBiome(opt.biomeId);
        if (biome == null) {
            player.sendMessage(ChatColor.RED + "Biome not available on this server version.");
            return;
        }

        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "Changing biome... This may take a moment.");

        plugin.getBiomeService().changeBiome(island, biome);
        player.sendMessage(ChatColor.GREEN + "Biome changed to " + ChatColor.translateAlternateColorCodes('&', opt.displayName) + ChatColor.GREEN + "!");
    }

    private ItemStack makeItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static final class BiomeOption {
        final String biomeId;
        final Material icon;
        final String displayName;
        final String description;

        BiomeOption(String biomeId, Material icon, String displayName, String description) {
            this.biomeId = biomeId;
            this.icon = icon;
            this.displayName = displayName;
            this.description = description;
        }
    }
}
