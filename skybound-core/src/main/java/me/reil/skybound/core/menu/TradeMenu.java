package me.reil.skybound.core.menu;

import me.reil.skybound.api.trade.TradeOffer;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.trade.TradeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Trade marketplace GUI.
 * Shows all active trade offers. Click to buy.
 */
public final class TradeMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final List<TradeOffer> displayedOffers = new ArrayList<TradeOffer>();

    public TradeMenu(Player player, SkyBoundPlugin plugin) {
        super(player);
        this.plugin = plugin;
    }

    @Override
    public String getTitle() {
        return ChatColor.DARK_GREEN + "Торговая площадка";
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());

        ItemStack border = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        // Info item
        ItemStack info = makeItem(Material.BOOK, ChatColor.GOLD + "Торговая площадка");
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.GRAY + "Нажмите на предмет чтобы купить.");
            lore.add(ChatColor.GRAY + "Ваши предложения отмечены зелёным.");
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }
        inventory.setItem(4, info);

        // Create offer button
        ItemStack createBtn = makeItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "Создать предложение");
        ItemMeta createMeta = createBtn.getItemMeta();
        if (createMeta != null) {
            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.GRAY + "Возьмите предмет в руку и");
            lore.add(ChatColor.GRAY + "используйте /is trade sell <цена>");
            createMeta.setLore(lore);
            createBtn.setItemMeta(createMeta);
        }
        inventory.setItem(49, createBtn);

        // Load offers
        TradeManager tradeManager = plugin.getTradeManager();
        List<TradeOffer> offers = tradeManager.getOffers();
        displayedOffers.clear();

        int slot = 9;
        for (TradeOffer offer : offers) {
            if (slot >= 45) break;
            displayedOffers.add(offer);

            ItemStack display = offer.getOffering().clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore() != null ? new ArrayList<String>(meta.getLore()) : new ArrayList<String>();
                lore.add("");
                lore.add(ChatColor.GOLD + "Цена: " + ChatColor.WHITE + String.format("%.0f", offer.getPrice()) + " монет");
                String sellerName = Bukkit.getOfflinePlayer(offer.getSeller()).getName();
                lore.add(ChatColor.GRAY + "Продавец: " + (sellerName != null ? sellerName : "???"));
                if (offer.getSeller().equals(player.getUniqueId())) {
                    lore.add("");
                    lore.add(ChatColor.RED + "ПКМ — отменить");
                }
                lore.add("");
                lore.add(ChatColor.YELLOW + "ЛКМ — купить");
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(slot++, display);
        }

        // Back button
        inventory.setItem(45, makeItem(Material.ARROW, ChatColor.WHITE + "Назад"));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        if (slot == 45) {
            player.closeInventory();
            return;
        }

        if (slot < 9 || slot >= 45) return;

        int index = slot - 9;
        if (index >= displayedOffers.size()) return;

        TradeOffer offer = displayedOffers.get(index);

        if (event.isRightClick() && offer.getSeller().equals(player.getUniqueId())) {
            // Cancel own offer
            boolean cancelled = plugin.getTradeManager().cancelOffer(player, offer.getId());
            if (cancelled) {
                player.sendMessage(ChatColor.GREEN + "Предложение отменено. Предмет возвращён.");
            }
            new TradeMenu(player, plugin).open();
            return;
        }

        if (event.isLeftClick()) {
            if (offer.getSeller().equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Нельзя купить свой предмет.");
                return;
            }
            boolean bought = plugin.getTradeManager().acceptOffer(player, offer.getId());
            if (bought) {
                player.sendMessage(ChatColor.GREEN + "Покупка успешна!");
                Player seller = Bukkit.getPlayer(offer.getSeller());
                if (seller != null) {
                    seller.sendMessage(ChatColor.GREEN + "Ваш предмет куплен игроком " + player.getName() + "!");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Не удалось купить. Недостаточно средств?");
            }
            new TradeMenu(player, plugin).open();
        }
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
