package me.reil.skybound.core.menu;

import me.reil.skybound.api.trade.TradeOffer;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.trade.TradeManager;
import org.bukkit.Bukkit;
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
        return lang().get("menu.trade.title");
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
        ItemStack info = makeItem(Material.BOOK, lang().get("menu.trade.info"));
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            List<String> lore = new ArrayList<String>();
            lore.add(lang().get("menu.trade.info-lore-buy"));
            lore.add(lang().get("menu.trade.info-lore-own"));
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }
        inventory.setItem(4, info);

        // Create offer button
        ItemStack createBtn = makeItem(Material.LIME_STAINED_GLASS_PANE, lang().get("menu.trade.create"));
        ItemMeta createMeta = createBtn.getItemMeta();
        if (createMeta != null) {
            List<String> lore = new ArrayList<String>();
            lore.add(lang().get("menu.trade.create-lore1"));
            lore.add(lang().get("menu.trade.create-lore2"));
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
                lore.add(lang().get("menu.trade.price", "{price}", String.format("%.0f", offer.getPrice())));
                String sellerName = Bukkit.getOfflinePlayer(offer.getSeller()).getName();
                lore.add(lang().get("menu.trade.seller", "{seller}", sellerName != null ? sellerName : "???"));
                if (offer.getSeller().equals(player.getUniqueId())) {
                    lore.add("");
                    lore.add(lang().get("menu.trade.right-cancel"));
                }
                lore.add("");
                lore.add(lang().get("menu.trade.left-buy"));
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(slot++, display);
        }

        // Back button
        addBackButton(45);
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
            TradeManager.TransactionResult result = plugin.getTradeManager().cancelOfferDetailed(player, offer.getId());
            sendTradeResult(result, false, offer);
            new TradeMenu(player, plugin).open();
            return;
        }

        if (event.isLeftClick()) {
            TradeManager.TransactionResult result = plugin.getTradeManager().acceptOfferDetailed(player, offer.getId());
            sendTradeResult(result, true, offer);
            if (result == TradeManager.TransactionResult.SUCCESS) {
                Player seller = Bukkit.getPlayer(offer.getSeller());
                if (seller != null) {
                    lang().send(seller, "trade.sold-notify", "{player}", player.getName(), "{price}", String.format("%.0f", offer.getPrice()));
                }
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

    private void sendTradeResult(TradeManager.TransactionResult result, boolean buying, TradeOffer offer) {
        switch (result) {
            case SUCCESS:
                lang().send(player, buying ? "trade.buy-success" : "trade.cancel-success");
                break;
            case OFFER_NOT_FOUND:
                lang().send(player, "trade.offer-not-found");
                break;
            case OWN_OFFER:
                lang().send(player, "trade.own-offer");
                break;
            case NO_MONEY:
                lang().send(player, "trade.no-money", "{price}", String.format("%.0f", offer.getPrice()));
                break;
            case NO_SPACE:
                lang().send(player, "trade.no-space");
                break;
            case NOT_SELLER:
                lang().send(player, "trade.not-seller");
                break;
            default:
                lang().send(player, "trade.failed");
                break;
        }
    }
}
