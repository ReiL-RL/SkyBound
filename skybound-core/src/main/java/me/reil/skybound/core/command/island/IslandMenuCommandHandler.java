package me.reil.skybound.core.command.island;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.menu.BankMenu;
import me.reil.skybound.core.menu.BoostersMenu;
import me.reil.skybound.core.menu.GiftsMenu;
import me.reil.skybound.core.menu.IslandCreateMenu;
import me.reil.skybound.core.menu.IslandMainMenu;
import me.reil.skybound.core.menu.IslandMembersMenu;
import me.reil.skybound.core.menu.IslandSettingsMenu;
import me.reil.skybound.core.menu.MissionCategoryMenu;
import me.reil.skybound.core.menu.PlayerStatsMenu;
import me.reil.skybound.core.menu.PrestigeShopMenu;
import me.reil.skybound.core.menu.ShopCategoryMenu;
import me.reil.skybound.core.menu.TopIslandsMenu;
import me.reil.skybound.core.menu.UpgradesMenu;
import me.reil.skybound.core.menu.WarpsMenu;
import org.bukkit.entity.Player;

public final class IslandMenuCommandHandler implements IslandSubCommandHandler {

    private final SkyBoundPlugin plugin;
    private final IslandCommandContext context;

    public IslandMenuCommandHandler(IslandCommandContext context) {
        this.context = context;
        this.plugin = context.plugin();
    }

    public void openDefault(Player player) {
        Island island = context.getIsland(player);
        if (island == null) {
            new IslandCreateMenu(player, plugin).open();
        } else {
            new IslandMainMenu(player, plugin, island).open();
        }
    }

    @Override
    public boolean handle(Player player, String[] args) {
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "top":
                new TopIslandsMenu(player, plugin).open();
                return true;
            case "shop":
                new ShopCategoryMenu(player, plugin).open();
                return true;
            case "missions":
                new MissionCategoryMenu(player, plugin).open();
                return true;
            case "gifts":
                new GiftsMenu(player, plugin).open();
                return true;
            case "stats":
                new PlayerStatsMenu(player, plugin).open();
                return true;
            case "prestigeshop":
            case "pshop":
                new PrestigeShopMenu(player, plugin).open();
                return true;
            case "warps":
                return openIslandMenu(player, MenuType.WARPS);
            case "bank":
                return openIslandMenu(player, MenuType.BANK);
            case "upgrades":
                return openIslandMenu(player, MenuType.UPGRADES);
            case "boosters":
                return openIslandMenu(player, MenuType.BOOSTERS);
            case "members":
                return openIslandMenu(player, MenuType.MEMBERS);
            case "settings":
                return openIslandMenu(player, MenuType.SETTINGS);
            case "menu":
                return openIslandMenu(player, MenuType.MAIN);
            case "chest":
                Island island = context.requireIsland(player);
                if (island == null) {
                    return true;
                }
                plugin.getIslandChestManager().open(player, island.getId(), island.getName());
                return true;
            default:
                return false;
        }
    }

    private boolean openIslandMenu(Player player, MenuType type) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return true;
        }

        switch (type) {
            case WARPS:
                new WarpsMenu(player, plugin, island).open();
                return true;
            case BANK:
                new BankMenu(player, plugin, island).open();
                return true;
            case UPGRADES:
                new UpgradesMenu(player, plugin, island).open();
                return true;
            case BOOSTERS:
                new BoostersMenu(player, plugin, island).open();
                return true;
            case MEMBERS:
                new IslandMembersMenu(player, plugin, island).open();
                return true;
            case SETTINGS:
                new IslandSettingsMenu(player, plugin, island).open();
                return true;
            case MAIN:
                new IslandMainMenu(player, plugin, island).open();
                return true;
            default:
                return false;
        }
    }

    private enum MenuType {
        WARPS,
        BANK,
        UPGRADES,
        BOOSTERS,
        MEMBERS,
        SETTINGS,
        MAIN
    }
}
