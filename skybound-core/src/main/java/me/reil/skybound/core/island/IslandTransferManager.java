package me.reil.skybound.core.island;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandRole;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.economy.VaultEconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Transfer ownership of an island, optionally for a price (sell).
 *
 * Free transfer: /is transfer <player>
 * Sell:          /is sell <player> <price>
 *   - target must accept with /is buy
 *   - on accept: economy charges target, deposits to seller, ownership changes
 */
public final class IslandTransferManager {

    public static final class SaleOffer {
        public final UUID seller;
        public final String islandId;
        public final UUID buyer;
        public final double price;
        public final long createdAt;

        public SaleOffer(UUID seller, String islandId, UUID buyer, double price) {
            this.seller = seller;
            this.islandId = islandId;
            this.buyer = buyer;
            this.price = price;
            this.createdAt = System.currentTimeMillis();
        }
    }

    public static final long OFFER_TTL_MS = 5 * 60 * 1000L; // 5 minutes

    private final JavaPlugin plugin;
    private final IslandManager islandManager;
    private final VaultEconomyProvider economy;
    /** buyer UUID -> sale offer */
    private final Map<UUID, SaleOffer> pendingOffers = new LinkedHashMap<UUID, SaleOffer>();

    public IslandTransferManager(JavaPlugin plugin, IslandManager islandManager, VaultEconomyProvider economy) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.economy = economy;
    }

    /** Transfer ownership without payment (current owner -> target who must be a member). */
    public String transfer(Player owner, OfflinePlayer target) {
        Island island = islandManager.getPlayerIsland(owner.getUniqueId());
        if (island == null) return "island.no-island";
        if (!island.getOwner().equals(owner.getUniqueId())) return "transfer.owner-only";
        if (target == null) return "player-not-found";
        if (target.getUniqueId().equals(owner.getUniqueId())) return "transfer.self";
        Island targetIsland = islandManager.getPlayerIsland(target.getUniqueId());
        if (targetIsland != null && !targetIsland.getId().equals(island.getId())) return "island.already-has";

        if (!island.getMembers().contains(target.getUniqueId())) {
            // Add as member first
            island.addMember(target.getUniqueId(), IslandRole.MEMBER);
        }
        // Demote old owner to admin, promote new
        island.setOwner(target.getUniqueId());
        island.setMemberRole(target.getUniqueId(), IslandRole.OWNER);
        island.setMemberRole(owner.getUniqueId(), IslandRole.ADMIN);
        islandManager.registerMember(target.getUniqueId(), island.getId());
        islandManager.registerMember(owner.getUniqueId(), island.getId());
        islandManager.saveData();
        return null;
    }

    /** Create a sale offer. Buyer must run /is buy to confirm. */
    public String createSaleOffer(Player seller, OfflinePlayer buyer, double price) {
        Island island = islandManager.getPlayerIsland(seller.getUniqueId());
        if (island == null) return "island.no-island";
        if (!island.getOwner().equals(seller.getUniqueId())) return "transfer.sell-owner-only";
        if (buyer == null) return "player-not-found";
        if (buyer.getUniqueId().equals(seller.getUniqueId())) return "transfer.self";
        if (islandManager.getPlayerIsland(buyer.getUniqueId()) != null) return "island.already-has";
        if (!isValidPrice(price)) return "number.price-positive";

        SaleOffer offer = new SaleOffer(seller.getUniqueId(), island.getId(), buyer.getUniqueId(), price);
        pendingOffers.put(buyer.getUniqueId(), offer);

        Player buyerOnline = buyer.getPlayer();
        if (buyerOnline != null) {
            lang().send(buyerOnline, "transfer.sale-offer",
                    "{player}", seller.getName(),
                    "{price}", String.valueOf(price));
            // Clickable button
            try {
                net.md_5.bungee.api.chat.TextComponent btn =
                        new net.md_5.bungee.api.chat.TextComponent(lang().get("transfer.buy-button", "{price}", String.valueOf(price)));
                btn.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                        net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/is buy"));
                btn.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                        net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                        new net.md_5.bungee.api.chat.hover.content.Text(lang().get("transfer.buy-hover"))));
                buyerOnline.spigot().sendMessage(btn);
            } catch (Throwable t) {
                lang().send(buyerOnline, "transfer.buy-fallback");
            }
        }
        return null;
    }

    /** Accept the pending sale offer for this buyer. */
    public String acceptOffer(Player buyer) {
        SaleOffer offer = pendingOffers.get(buyer.getUniqueId());
        if (offer == null) return "transfer.no-offers";
        if (System.currentTimeMillis() - offer.createdAt > OFFER_TTL_MS) {
            pendingOffers.remove(buyer.getUniqueId());
            return "transfer.offer-expired";
        }
        if (islandManager.getPlayerIsland(buyer.getUniqueId()) != null) {
            return "island.already-has";
        }
        Island island = islandManager.getIsland(offer.islandId);
        if (island == null) return "transfer.island-gone";
        if (!island.getOwner().equals(offer.seller)) {
            pendingOffers.remove(buyer.getUniqueId());
            return "transfer.offer-invalid";
        }

        if (!economy.has(buyer.getUniqueId(), offer.price)) {
            return "transfer.no-money";
        }

        Player seller = Bukkit.getPlayer(offer.seller);

        if (!economy.withdraw(buyer.getUniqueId(), offer.price)) {
            return "transfer.no-money";
        }
        if (!economy.deposit(offer.seller, offer.price)) {
            economy.deposit(buyer.getUniqueId(), offer.price);
            return "transfer.payment-failed";
        }

        island.setOwner(buyer.getUniqueId());
        if (!island.getMembers().contains(buyer.getUniqueId())) {
            island.addMember(buyer.getUniqueId(), IslandRole.OWNER);
        }
        island.setMemberRole(buyer.getUniqueId(), IslandRole.OWNER);
        island.setMemberRole(offer.seller, IslandRole.ADMIN);
        islandManager.registerMember(buyer.getUniqueId(), island.getId());
        islandManager.registerMember(offer.seller, island.getId());
        islandManager.saveData();
        pendingOffers.remove(buyer.getUniqueId());

        if (seller != null) {
            lang().send(seller, "transfer.sold",
                    "{player}", buyer.getName(),
                    "{price}", String.valueOf(offer.price));
        }
        return null;
    }

    /** Cancel offer (by buyer or seller). */
    public boolean cancelOffer(Player who) {
        // Cancel by buyer
        if (pendingOffers.remove(who.getUniqueId()) != null) return true;
        // Cancel by seller — find any offer where this player is the seller
        return pendingOffers.values().removeIf(o -> o.seller.equals(who.getUniqueId()));
    }

    public SaleOffer getOfferFor(UUID buyer) {
        return pendingOffers.get(buyer);
    }

    private me.reil.skybound.core.lang.LangManager lang() {
        return ((SkyBoundPlugin) plugin).getLangManager();
    }

    private boolean isValidPrice(double price) {
        return price > 0.0 && !Double.isNaN(price) && !Double.isInfinite(price);
    }
}
