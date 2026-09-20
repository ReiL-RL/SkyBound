package me.reil.skybound.core.menu;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
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
            new BiomeOption("PLAINS", Material.GRASS_BLOCK, "menu.biome.plains", "menu.biome.plains-lore"),
            new BiomeOption("DESERT", Material.SAND, "menu.biome.desert", "menu.biome.desert-lore"),
            new BiomeOption("FOREST", Material.OAK_SAPLING, "menu.biome.forest", "menu.biome.forest-lore"),
            new BiomeOption("JUNGLE", Material.JUNGLE_SAPLING, "menu.biome.jungle", "menu.biome.jungle-lore"),
            new BiomeOption("SNOWY_TAIGA", Material.SNOW_BLOCK, "menu.biome.winter", "menu.biome.winter-lore"),
            new BiomeOption("MUSHROOM_FIELDS", Material.RED_MUSHROOM_BLOCK, "menu.biome.mushroom", "menu.biome.mushroom-lore"),
            new BiomeOption("BADLANDS", Material.RED_SAND, "menu.biome.badlands", "menu.biome.badlands-lore"),
            new BiomeOption("FLOWER_FOREST", Material.POPPY, "menu.biome.flower", "menu.biome.flower-lore"),
            new BiomeOption("DARK_FOREST", Material.DARK_OAK_SAPLING, "menu.biome.dark-forest", "menu.biome.dark-forest-lore"),
            new BiomeOption("SWAMP", Material.LILY_PAD, "menu.biome.swamp", "menu.biome.swamp-lore"),
            new BiomeOption("OCEAN", Material.WATER_BUCKET, "menu.biome.ocean", "menu.biome.ocean-lore"),
            new BiomeOption("THE_VOID", Material.OBSIDIAN, "menu.biome.void", "menu.biome.void-lore"),
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
                meta.setDisplayName(lang().get(opt.displayKey));
                List<String> lore = new ArrayList<String>();
                lore.add(lang().get(opt.descriptionKey));
                lore.add("");
                lore.add(lang().get("menu.biome.click"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot++, item);
        }
        addBackButton(31);
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
            lang().send(player, "biome.unavailable");
            return;
        }

        player.closeInventory();
        lang().send(player, "biome.changing");

        final String displayName = lang().get(opt.displayKey);
        plugin.getBiomeService().changeBiomeBatched(island, biome, new Runnable() {
            @Override
            public void run() {
                lang().send(player, "biome.changed", "{biome}", displayName);
            }
        });
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
        final String displayKey;
        final String descriptionKey;

        BiomeOption(String biomeId, Material icon, String displayKey, String descriptionKey) {
            this.biomeId = biomeId;
            this.icon = icon;
            this.displayKey = displayKey;
            this.descriptionKey = descriptionKey;
        }
    }
}
