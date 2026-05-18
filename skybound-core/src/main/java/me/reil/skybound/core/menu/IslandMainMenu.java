package me.reil.skybound.core.menu;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.lang.LangManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Main island menu (/is).
 */
public final class IslandMainMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final Island island;
    private final LangManager lang;

    public IslandMainMenu(Player player, SkyBoundPlugin plugin, Island island) {
        super(player);
        this.plugin = plugin;
        this.island = island;
        this.lang = plugin.getLangManager();
    }

    @Override
    public String getTitle() {
        return lang.get("menu.main.title");
    }

    @Override
    public int getSize() {
        return 45;
    }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());

        // Border
        ItemStack border = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 36; i < 45; i++) inventory.setItem(i, border);

        // Island info (slot 4)
        setItem(4, Material.GRASS_BLOCK, "&6" + island.getName(),
                "&7\u2726 \u0423\u0440\u043e\u0432\u0435\u043d\u044c: &e" + island.getLevel(),
                "&7\u2726 \u0423\u0447\u0430\u0441\u0442\u043d\u0438\u043a\u0438: &e" + island.getMembers().size(),
                "&7\u2726 \u0411\u0430\u043d\u043a: &e$" + String.format("%.0f", island.getBankBalance()),
                "&7\u2726 \u0421\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c: &e" + String.format("%.0f", island.getValue()));

        // Row 1
        setItem(10, Material.OAK_DOOR, lang.get("menu.main.teleport"), "&7\u041d\u0430\u0436\u043c\u0438 \u0434\u043b\u044f \u0442\u0435\u043b\u0435\u043f\u043e\u0440\u0442\u0430.");
        setItem(12, Material.PLAYER_HEAD, lang.get("menu.main.members"), "&7\u0423\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u043a\u043e\u043c\u0430\u043d\u0434\u043e\u0439.");

        // Upgrades button — check if blocked by Island Core addon
        boolean addonRegistered = isAddonRegistered();
        if (addonRegistered && plugin.getCoreConfig().isIslandCoreDisableUpgradeMenu()) {
            setItem(14, Material.GRAY_DYE, "&8\u0423\u043b\u0443\u0447\u0448\u0435\u043d\u0438\u044f", "&7\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0439 &6\u042f\u0434\u0440\u043e \u0423\u043b\u0443\u0447\u0448\u0435\u043d\u0438\u0439");
        } else {
            setItem(14, Material.DIAMOND, lang.get("menu.main.upgrades"), "&7\u0423\u043b\u0443\u0447\u0448\u0438 \u0441\u0432\u043e\u0439 \u043e\u0441\u0442\u0440\u043e\u0432.");
        }

        // Boosters button — check if blocked by Island Core addon
        if (addonRegistered && plugin.getCoreConfig().isIslandCoreDisableBoosterMenu()) {
            setItem(16, Material.GRAY_DYE, "&8\u0411\u0443\u0441\u0442\u0435\u0440\u044b", "&7\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0439 &d\u042f\u0434\u0440\u043e \u0411\u0443\u0441\u0442\u0435\u0440\u0430");
        } else {
            setItem(16, Material.BREWING_STAND, lang.get("menu.main.boosters"), "&7\u0412\u0440\u0435\u043c\u0435\u043d\u043d\u044b\u0435 \u0431\u0430\u0444\u044b.");
        }

        // Row 2
        setItem(19, Material.BOOK, lang.get("menu.main.missions"), "&7\u0417\u0430\u0434\u0430\u043d\u0438\u044f \u0437\u0430 \u043d\u0430\u0433\u0440\u0430\u0434\u044b.");
        setItem(21, Material.EMERALD, lang.get("menu.main.shop"), "&7\u041f\u043e\u043a\u0443\u043f\u043a\u0430 \u0438 \u043f\u0440\u043e\u0434\u0430\u0436\u0430.");
        setItem(23, Material.GOLD_INGOT, lang.get("menu.main.bank"), "&7\u0411\u0430\u043d\u043a \u043e\u0441\u0442\u0440\u043e\u0432\u0430.");
        setItem(25, Material.NETHER_STAR, lang.get("menu.main.top"), "&7\u0422\u0430\u0431\u043b\u0438\u0446\u0430 \u043b\u0438\u0434\u0435\u0440\u043e\u0432.");

        // Row 3
        setItem(30, Material.ENDER_PEARL, lang.get("menu.main.warps"), "&7\u0412\u0430\u0440\u043f\u044b \u043e\u0441\u0442\u0440\u043e\u0432\u0430.");
        setItem(32, Material.COMPARATOR, lang.get("menu.main.settings"), "&7\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u043e\u0441\u0442\u0440\u043e\u0432\u0430.");

        // Events button (only if VoidRift is installed)
        if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("VoidRift")) {
            setItem(34, Material.ENDER_EYE, "&5\u2726 \u0421\u043e\u0431\u044b\u0442\u0438\u044f", "&7\u0410\u043a\u0442\u0438\u0432\u043d\u044b\u0435 \u0441\u043e\u0431\u044b\u0442\u0438\u044f VoidRift.");
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        boolean addonRegistered = isAddonRegistered();

        switch (slot) {
            case 10: // Home
                player.closeInventory();
                player.teleport(island.getHome());
                lang.send(player, "island.teleported-home");
                break;
            case 12: // Members
                new IslandMembersMenu(player, plugin, island).open();
                break;
            case 14: // Upgrades
                if (addonRegistered && plugin.getCoreConfig().isIslandCoreDisableUpgradeMenu()) {
                    lang.send(player, "island-core.upgrade-via-core");
                } else {
                    new UpgradesMenu(player, plugin, island).open();
                }
                break;
            case 16: // Boosters
                if (addonRegistered && plugin.getCoreConfig().isIslandCoreDisableBoosterMenu()) {
                    lang.send(player, "island-core.booster-via-core");
                } else {
                    new BoostersMenu(player, plugin, island).open();
                }
                break;
            case 19: // Missions
                new MissionCategoryMenu(player, plugin).open();
                break;
            case 21: // Shop
                new ShopCategoryMenu(player, plugin).open();
                break;
            case 23: // Bank
                new BankMenu(player, plugin, island).open();
                break;
            case 25: // Top
                new TopIslandsMenu(player, plugin).open();
                break;
            case 30: // Warps
                new WarpsMenu(player, plugin, island).open();
                break;
            case 32: // Settings
                new IslandSettingsMenu(player, plugin, island).open();
                break;
            case 34: // Events (VoidRift)
                new EventsMenu(player, plugin).open();
                break;
        }
    }

    private boolean isAddonRegistered() {
        try {
            return plugin.getAddonRegistry().isRegistered("island-core");
        } catch (Exception e) {
            return false;
        }
    }

    private void setItem(int slot, Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<String>();
                for (String line : lore) {
                    loreList.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
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
}
