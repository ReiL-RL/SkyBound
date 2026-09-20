package me.reil.skybound.core.menu;

import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.island.IslandGiftManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Inbox of pending gifts. Click an item to claim it into your inventory.
 */
public final class GiftsMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private List<IslandGiftManager.Gift> snapshot;
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd.MM HH:mm");

    public GiftsMenu(Player player, SkyBoundPlugin plugin) {
        super(player);
        this.plugin = plugin;
    }

    @Override public String getTitle() { return lang().get("menu.gifts.title"); }
    @Override public int getSize() { return 54; }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());
        snapshot = plugin.getIslandGiftManager().getPending(player.getUniqueId());

        // Borders
        ItemStack topGlass = decoration(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) if (i != 4) inventory.setItem(i, topGlass);
        ItemStack bottomGlass = decoration(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            if (i == 45 || i == 49 || i == 53) continue;
            inventory.setItem(i, bottomGlass);
        }

        // Header
        ItemStack header = new ItemStack(Material.CHEST);
        ItemMeta hMeta = header.getItemMeta();
        if (hMeta != null) {
            hMeta.setDisplayName(lang().get("menu.gifts.header"));
            List<String> hLore = new ArrayList<String>();
            hLore.add("");
            hLore.add(lang().get("menu.gifts.received", "{count}", String.valueOf(snapshot.size())));
            hLore.add(lang().get("menu.gifts.daily-limit", "{limit}", String.valueOf(plugin.getIslandGiftManager().getDailyLimit())));
            hLore.add(lang().get("menu.gifts.sent-today", "{count}", String.valueOf(plugin.getIslandGiftManager().getSentToday(player.getUniqueId()))));
            hMeta.setLore(hLore);
            header.setItemMeta(hMeta);
        }
        inventory.setItem(4, header);

        // Gift items (slots 9-44)
        for (int i = 0; i < snapshot.size() && i < 36; i++) {
            int slot = 9 + i;
            IslandGiftManager.Gift g = snapshot.get(i);
            ItemStack display = g.item.clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore() != null ? new ArrayList<String>(meta.getLore()) : new ArrayList<String>();
                lore.add("");
                lore.add(lang().get("menu.gifts.from", "{player}", g.senderName));
                lore.add(lang().get("menu.gifts.when", "{time}", DATE_FMT.format(new Date(g.timestamp))));
                lore.add("");
                lore.add(lang().get("menu.gifts.claim"));
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(slot, display);
        }

        // "Claim all" button (slot 49)
        if (!snapshot.isEmpty()) {
            ItemStack claimAll = new ItemStack(Material.HOPPER);
            ItemMeta cm = claimAll.getItemMeta();
            if (cm != null) {
                cm.setDisplayName(lang().get("menu.gifts.claim-all"));
                List<String> cLore = new ArrayList<String>();
                cLore.add(lang().get("menu.gifts.claim-all-lore", "{count}", String.valueOf(snapshot.size())));
                cm.setLore(cLore);
                claimAll.setItemMeta(cm);
            }
            inventory.setItem(49, claimAll);
        } else {
            // Show empty marker
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta em = empty.getItemMeta();
            if (em != null) {
                em.setDisplayName(lang().get("menu.gifts.empty"));
                em.setLore(java.util.Collections.singletonList(lang().get("menu.gifts.empty-lore")));
                empty.setItemMeta(em);
            }
            inventory.setItem(22, empty);
        }

        addBackButton(45);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (handleBack(slot, 45)) return;
        if (slot == 49 && !snapshot.isEmpty()) {
            List<ItemStack> items = plugin.getIslandGiftManager().claimAll(player.getUniqueId());
            for (ItemStack it : items) {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(it);
                for (ItemStack drop : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
            lang().send(player, "gift.claimed-all", "{count}", String.valueOf(items.size()));
            new GiftsMenu(player, plugin).withParent(parentMenu).open();
            return;
        }
        // Per-gift slots: 9..44
        if (slot < 9 || slot > 44) return;
        int idx = slot - 9;
        if (idx < 0 || idx >= snapshot.size()) return;

        ItemStack item = plugin.getIslandGiftManager().claim(player.getUniqueId(), idx);
        if (item == null) return;

        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        for (ItemStack drop : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
        lang().send(player, "gift.claimed");
        new GiftsMenu(player, plugin).withParent(parentMenu).open();
    }

    private ItemStack decoration(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
