package me.reil.skybound.core.trade;

import me.reil.skybound.api.trade.TradeOffer;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class TradeOfferImpl implements TradeOffer {

    private final String id;
    private final UUID seller;
    private final String islandId;
    private final ItemStack offering;
    private final double price;
    private final long timestamp;
    private boolean active;

    public TradeOfferImpl(String id, UUID seller, String islandId, ItemStack offering, double price, long timestamp) {
        this.id = id;
        this.seller = seller;
        this.islandId = islandId;
        this.offering = offering;
        this.price = price;
        this.timestamp = timestamp;
        this.active = true;
    }

    @Override public String getId() { return id; }
    @Override public UUID getSeller() { return seller; }
    @Override public String getIslandId() { return islandId; }
    @Override public ItemStack getOffering() { return offering.clone(); }
    @Override public double getPrice() { return price; }
    @Override public long getTimestamp() { return timestamp; }
    @Override public boolean isActive() { return active; }

    public void setActive(boolean active) { this.active = active; }
}
