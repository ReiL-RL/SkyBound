package me.reil.skybound.api.trade;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Island trading system provider.
 */
public interface TradeProvider {

    /** Create a new trade offer (item for money). */
    TradeOffer createOffer(Player seller, ItemStack offering, double price);

    /** Accept a trade offer. */
    boolean acceptOffer(Player buyer, String offerId);

    /** Cancel a trade offer (seller only). */
    boolean cancelOffer(Player seller, String offerId);

    /** Get all active offers. */
    List<TradeOffer> getOffers();

    /** Get active offers for a specific island. */
    List<TradeOffer> getOffers(String islandId);

    /** Get a specific offer by id. */
    TradeOffer getOffer(String offerId);
}
