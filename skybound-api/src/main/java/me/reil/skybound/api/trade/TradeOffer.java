package me.reil.skybound.api.trade;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents a trade offer.
 */
public interface TradeOffer {

    /** Unique offer id. */
    String getId();

    /** Seller UUID. */
    UUID getSeller();

    /** Seller's island id. */
    String getIslandId();

    /** Item being offered. */
    ItemStack getOffering();

    /** Price in money. */
    double getPrice();

    /** Timestamp when offer was created. */
    long getTimestamp();

    /** Whether the offer is still active. */
    boolean isActive();
}
