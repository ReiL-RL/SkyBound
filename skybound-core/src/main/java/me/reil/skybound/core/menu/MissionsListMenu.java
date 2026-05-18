package me.reil.skybound.core.menu;

import me.reil.skybound.api.mission.MissionProgress;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.lang.LangManager;
import me.reil.skybound.core.mission.MissionImpl;
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

    @Override public String getTitle() { return lang().get("menu.missions-list.title", "{category}", category); }
    @Override public int getSize() { return 54; }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());
        LangManager l = lang();
        missions.addAll(plugin.getMissionManager().getMissionsByCategory(category));

        int slot = 0;
        for (MissionImpl mission : missions) {
            if (slot >= 45) break;
            MissionProgress progress = plugin.getMissionManager().getProgress(player.getUniqueId(), mission.getId());
            boolean completed = progress != null && progress.isCompleted();
            boolean claimed = progress != null && progress.isClaimed();

            ItemStack item = new ItemStack(mission.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(mission.getDisplayName()));
                List<String> lore = new ArrayList<String>();
                for (String line : mission.getDescription()) {
                    lore.add(color(line));
                }
                lore.add("");
                if (claimed) {
                    lore.add(l.get("mission.completed-claimed"));
                } else if (completed) {
                    lore.add(l.get("mission.completed-claim"));
                } else {
                    lore.add(l.get("mission.in-progress"));
                }
                lore.add("");
                lore.add(l.get("mission.rewards"));
                if (mission.getMoneyReward() > 0) lore.add(l.get("mission.reward-money", "{amount}", String.format("%.0f", mission.getMoneyReward())));
                if (mission.getXpReward() > 0) lore.add(l.get("mission.reward-xp", "{amount}", String.valueOf(mission.getXpReward())));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);
            slot++;
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot < 0 || slot >= missions.size()) return;

        MissionImpl mission = missions.get(slot);
        boolean ok = plugin.getMissionManager().claimReward(player, mission.getId());
        if (ok) {
            lang().send(player, "mission.claimed", "{name}", mission.getDisplayName());
            new MissionsListMenu(player, plugin, category).open();
        }
    }
}
