package me.reil.skybound.core.menu;

import me.reil.skybound.api.mission.MissionProgress;
import me.reil.skybound.api.mission.MissionType;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.mission.MissionConditionImpl;
import me.reil.skybound.core.mission.MissionImpl;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class MissionsListMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final String category;
    private final List<MissionImpl> missions = new ArrayList<MissionImpl>();

    public MissionsListMenu(Player player, SkyBoundPlugin plugin, String category) {
        super(player);
        this.plugin = plugin;
        this.category = category;
    }

    @Override public String getTitle() { return lang().get("menu.missions-list.title", "{category}", capitalize(category)); }
    @Override public int getSize() { return 54; }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());
        missions.clear();
        missions.addAll(plugin.getMissionManager().getMissionsByCategory(category));

        // Top decorative row (slots 0-8)
        ItemStack topGlass = decoration(Material.CYAN_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, topGlass);

        // Bottom decorative row (slots 45-53)
        ItemStack bottomGlass = decoration(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inventory.setItem(i, bottomGlass);

        // Header (slot 4)
        ItemStack header = new ItemStack(getCategoryIcon(category));
        ItemMeta hMeta = header.getItemMeta();
        if (hMeta != null) {
            hMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + capitalize(category));
            List<String> hLore = new ArrayList<String>();
            hLore.add(lang().get("menu.missions-list.count", "{count}", String.valueOf(missions.size())));
            int completed = 0;
            for (MissionImpl m : missions) {
                MissionProgress p = plugin.getMissionManager().getProgress(player.getUniqueId(), m.getId());
                if (p != null && p.isClaimed()) completed++;
            }
            hLore.add(lang().get("menu.missions-list.completed", "{completed}", String.valueOf(completed), "{total}", String.valueOf(missions.size())));
            hMeta.setLore(hLore);
            header.setItemMeta(hMeta);
        }
        inventory.setItem(4, header);

        // Mission slots (9-44)
        int slot = 9;
        for (MissionImpl mission : missions) {
            if (slot >= 45) break;
            // Skip border edges (10-16, 19-25, 28-34, 37-43 — central columns)
            if (slot % 9 == 0 || slot % 9 == 8) {
                inventory.setItem(slot, decoration(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " "));
                slot++;
                if (slot >= 45) break;
            }

            MissionProgress progress = plugin.getMissionManager().getProgress(player.getUniqueId(), mission.getId());
            boolean completed = progress != null && progress.isCompleted();
            boolean claimed = progress != null && progress.isClaimed();

            ItemStack item = new ItemStack(mission.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                // Status prefix in name
                String namePrefix;
                if (claimed) namePrefix = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "✔ ";
                else if (completed) namePrefix = ChatColor.GREEN + "" + ChatColor.BOLD + "★ ";
                else namePrefix = ChatColor.YELLOW + "" + ChatColor.BOLD + "▸ ";
                meta.setDisplayName(namePrefix + ChatColor.translateAlternateColorCodes('&', mission.getDisplayName()));

                List<String> lore = new ArrayList<String>();

                // Description (lore from yml)
                if (!mission.getDescription().isEmpty()) {
                    for (String line : mission.getDescription()) {
                        lore.add(ChatColor.translateAlternateColorCodes('&', line));
                    }
                    lore.add("");
                }

                // Auto-generated task description from conditions
                lore.add(lang().get("menu.missions-list.objectives"));
                List<MissionConditionImpl> conditions = mission.getConditionImpls();
                String mode = conditions.size() > 1 ? (mission.getConditionMode().name().equals("AND") ? lang().get("menu.missions-list.mode-and") : lang().get("menu.missions-list.mode-or")) : "";
                for (MissionConditionImpl cond : conditions) {
                    int cur = progress != null ? progress.getConditionProgress(cond.getId()) : 0;
                    int total = cond.getAmount();
                    int capped = Math.min(cur, total);

                    String desc = describeCondition(cond);
                    boolean done = cur >= total;
                    String tick = done ? ChatColor.GREEN + "✔" : ChatColor.GRAY + "○";
                    String pColor = done ? ChatColor.GREEN.toString() : ChatColor.AQUA.toString();

                    lore.add("  " + tick + ChatColor.GRAY + " " + desc
                            + ChatColor.DARK_GRAY + " ["
                            + pColor + capped + ChatColor.DARK_GRAY + "/" + pColor + total
                            + ChatColor.DARK_GRAY + "]");
                    // Progress bar
                    lore.add("    " + progressBar(capped, total));
                }
                if (!mode.isEmpty()) {
                    lore.add(lang().get("menu.missions-list.mode", "{mode}", mode));
                }
                lore.add("");

                // Status
                if (claimed) {
                    lore.add(lang().get("mission.completed-claimed"));
                } else if (completed) {
                    lore.add(lang().get("mission.completed-claim"));
                    lore.add(lang().get("menu.missions-list.claim-click"));
                } else {
                    lore.add(lang().get("mission.in-progress"));
                }

                // Rewards
                if (!claimed) {
                    lore.add("");
                    lore.add(lang().get("mission.rewards"));
                    if (mission.getMoneyReward() > 0) {
                        lore.add(lang().get("mission.reward-money", "{amount}", String.format("%.0f", mission.getMoneyReward())));
                    }
                    if (mission.getXpReward() > 0) {
                        lore.add(lang().get("mission.reward-xp", "{amount}", String.valueOf(mission.getXpReward())));
                    }
                    if (!mission.getItemRewards().isEmpty()) {
                        for (String it : mission.getItemRewards()) {
                            String[] parts = it.split(":");
                            String name = parts[0];
                            int amt = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                            lore.add(lang().get("mission.reward-item", "{amount}", String.valueOf(amt), "{item}", prettyMaterial(name)));
                        }
                    }
                }

                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);
            slot++;
        }

        // Edge-fill remaining inner slots
        for (int s = 9; s < 45; s++) {
            if (inventory.getItem(s) == null) {
                if (s % 9 == 0 || s % 9 == 8) {
                    inventory.setItem(s, decoration(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " "));
                }
            }
        }

        addBackButton();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (handleBack(slot)) return;
        int idx = computeMissionIndex(slot);
        if (idx < 0 || idx >= missions.size()) return;

        MissionImpl mission = missions.get(idx);
        boolean ok = plugin.getMissionManager().claimReward(player, mission.getId());
        if (ok) {
            lang().send(player, "mission.claimed", "{name}", mission.getDisplayName());
            new MissionsListMenu(player, plugin, category).withParent(parentMenu).open();
        }
    }

    /** Slot → mission index, accounting for skipped edge slots. */
    private int computeMissionIndex(int slot) {
        if (slot < 9 || slot >= 45) return -1;
        // Skip column 0 and 8 — they're decorative
        int col = slot % 9;
        if (col == 0 || col == 8) return -1;
        int row = (slot - 9) / 9;
        // Per row 7 mission slots (1..7)
        return row * 7 + (col - 1);
    }

    private String describeCondition(MissionConditionImpl cond) {
        MissionType type = cond.getType();
        String target = cond.getTarget();
        boolean any = target == null || target.isEmpty();

        switch (type) {
            case BREAK_BLOCK:
                return lang().get(any ? "mission.condition.break-block.any" : "mission.condition.break-block.target", "{target}", prettyMaterial(target));
            case PLACE_BLOCK:
                return lang().get(any ? "mission.condition.place-block.any" : "mission.condition.place-block.target", "{target}", prettyMaterial(target));
            case KILL_MOB:
                return lang().get(any ? "mission.condition.kill-mob.any" : "mission.condition.kill-mob.target", "{target}", prettyMaterial(target));
            case CRAFT_ITEM:
                return lang().get(any ? "mission.condition.craft-item.any" : "mission.condition.craft-item.target", "{target}", prettyMaterial(target));
            case SMELT_ITEM:
                return lang().get(any ? "mission.condition.smelt-item.any" : "mission.condition.smelt-item.target", "{target}", prettyMaterial(target));
            case BREW_POTION:
                return lang().get("mission.condition.brew-potion");
            case ENCHANT_ITEM:
                return lang().get("mission.condition.enchant-item");
            case FISH:
                return lang().get("mission.condition.fish");
            case HARVEST:
                return lang().get(any ? "mission.condition.harvest.any" : "mission.condition.harvest.target", "{target}", prettyMaterial(target));
            case SHEAR:
                return lang().get("mission.condition.shear");
            case BREED:
                return lang().get(any ? "mission.condition.breed.any" : "mission.condition.breed.target", "{target}", prettyMaterial(target));
            case TAME:
                return lang().get(any ? "mission.condition.tame.any" : "mission.condition.tame.target", "{target}", prettyMaterial(target));
            case ISLAND_LEVEL:
                return lang().get("mission.condition.island-level");
            case ISLAND_VALUE:
                return lang().get("mission.condition.island-value");
            case BANK_DEPOSIT:
                return lang().get("mission.condition.bank-deposit");
            case SHOP_BUY:
                return lang().get("mission.condition.shop-buy");
            case SHOP_SELL:
                return lang().get("mission.condition.shop-sell");
            case GENERATOR_COLLECT:
                return lang().get("mission.condition.generator-collect");
            case PICKUP_ITEM:
                return lang().get(any ? "mission.condition.pickup-item.any" : "mission.condition.pickup-item.target", "{target}", prettyMaterial(target));
            case EAT:
                return lang().get(any ? "mission.condition.eat.any" : "mission.condition.eat.target", "{target}", prettyMaterial(target));
            case WALK_DISTANCE:
                return lang().get("mission.condition.walk-distance");
            case GAIN_XP:
                return lang().get("mission.condition.gain-xp");
            case CUSTOM:
                return lang().get("mission.condition.custom");
            default:
                return type.name();
        }
    }

    private String progressBar(int current, int total) {
        int width = 20;
        double pct = total <= 0 ? 1.0 : Math.min(1.0, (double) current / total);
        int filled = (int) Math.round(pct * width);
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.DARK_GRAY).append("[");
        sb.append(ChatColor.GREEN);
        for (int i = 0; i < filled; i++) sb.append("▉");
        sb.append(ChatColor.DARK_GRAY);
        for (int i = filled; i < width; i++) sb.append("▉");
        sb.append(ChatColor.DARK_GRAY).append("] ");
        sb.append(ChatColor.GRAY).append((int) Math.round(pct * 100)).append("%");
        return sb.toString();
    }

    private String prettyMaterial(String raw) {
        if (raw == null || raw.isEmpty()) return lang().get("mission.any");
        String[] parts = raw.toLowerCase().split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (out.length() > 0) out.append(' ');
            if (!part.isEmpty()) {
                out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return out.toString();
    }

    private ItemStack decoration(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            it.setItemMeta(meta);
        }
        return it;
    }

    private Material getCategoryIcon(String category) {
        switch (category.toLowerCase()) {
            case "mining": return Material.IRON_PICKAXE;
            case "farming": return Material.WHEAT;
            case "combat": return Material.DIAMOND_SWORD;
            case "fishing": return Material.FISHING_ROD;
            case "crafting": return Material.CRAFTING_TABLE;
            case "building": return Material.BRICKS;
            case "economy": return Material.GOLD_INGOT;
            case "alchemy": return Material.BREWING_STAND;
            case "animals": return Material.LEAD;
            case "island": return Material.GRASS_BLOCK;
            case "generator": return Material.HOPPER;
            case "collection": return Material.CHEST;
            case "food": return Material.GOLDEN_APPLE;
            case "exploration": return Material.COMPASS;
            case "experience": return Material.EXPERIENCE_BOTTLE;
            case "daily": return Material.CLOCK;
            case "weekly": return Material.NETHER_STAR;
            case "challenge": return Material.TNT;
            case "special": return Material.BEACON;
            default: return Material.PAPER;
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
