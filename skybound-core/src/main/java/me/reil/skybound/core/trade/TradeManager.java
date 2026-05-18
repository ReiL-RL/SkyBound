package me.reil.skybound.core.trade;

import me.reil.skybound.api.economy.EconomyProvider;
import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.trade.TradeOffer;
import me.reil.skybound.api.trade.TradeProvider;
import me.reil.skybound.core.island.IslandManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages island trade offers.
 * Players can sell items for money through a shared marketplace.
 */
public final class TradeManager implements TradeProvider {

    private final JavaPlugin plugin;
    private final IslandManager islandManager;
    private final EconomyProvider economy;
    private final Map<String, TradeOfferImpl> offers = new LinkedHashMap<String, TradeOfferImpl>();
    private int nextId = 1;

    public TradeManager(JavaPlugin plugin, IslandManager islandManager, EconomyProvider economy) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.economy = economy;
        loadOffers();
    }

    @Override
    public TradeOffer createOffer(Player seller, ItemStack offering, double price) {
        Island island = islandManager.getPlayerIsland(seller.getUniqueId());
        if (island == null) return null;
        if (offering == null || offering.getAmount() <= 0) return null;
        if (price <= 0) return null;

        String id = "trade_" + (nextId++);
        TradeOfferImpl offer = new TradeOfferImpl(id, seller.getUniqueId(), island.getId(),
                offering.clone(), price, System.currentTimeMillis());
        offers.put(id, offer);

        // Remove item from seller's inventory
        seller.getInventory().removeItem(offering.clone());
        saveOffers();
        return offer;
    }

    @Override
    public boolean acceptOffer(Player buyer, String offerId) {
        TradeOfferImpl offer = offers.get(offerId);
        if (offer == null || !offer.isActive()) return false;
        if (offer.getSeller().equals(buyer.getUniqueId())) return false;

        // Check buyer has enough money
        double balance = economy.getBalance(buyer.getUniqueId());
        if (balance < offer.getPrice()) return false;

        // Transfer money
        economy.withdraw(buyer.getUniqueId(), offer.getPrice());
        economy.deposit(offer.getSeller(), offer.getPrice());

        // Give item to buyer
        buyer.getInventory().addItem(offer.getOffering());

        // Deactivate offer
        offer.setActive(false);
        saveOffers();
        return true;
    }

    @Override
    public boolean cancelOffer(Player seller, String offerId) {
        TradeOfferImpl offer = offers.get(offerId);
        if (offer == null || !offer.isActive()) return false;
        if (!offer.getSeller().equals(seller.getUniqueId())) return false;

        // Return item to seller
        seller.getInventory().addItem(offer.getOffering());
        offer.setActive(false);
        saveOffers();
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TradeOffer> getOffers() {
        List<TradeOffer> result = new ArrayList<TradeOffer>();
        for (TradeOfferImpl offer : offers.values()) {
            if (offer.isActive()) result.add(offer);
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TradeOffer> getOffers(String islandId) {
        List<TradeOffer> result = new ArrayList<TradeOffer>();
        for (TradeOfferImpl offer : offers.values()) {
            if (offer.isActive() && offer.getIslandId().equals(islandId)) {
                result.add(offer);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public TradeOffer getOffer(String offerId) {
        return offers.get(offerId);
    }

    public void saveOffers() {
        File file = new File(plugin.getDataFolder(), "trades.yml");
        FileConfiguration cfg = new YamlConfiguration();

        cfg.set("next-id", nextId);

        // Only save active offers
        for (TradeOfferImpl offer : offers.values()) {
            if (!offer.isActive()) continue;
            String path = "offers." + offer.getId();
            cfg.set(path + ".seller", offer.getSeller().toString());
            cfg.set(path + ".island-id", offer.getIslandId());
            cfg.set(path + ".offering", offer.getOffering());
            cfg.set(path + ".price", offer.getPrice());
            cfg.set(path + ".timestamp", offer.getTimestamp());
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save trades: " + e.getMessage());
        }
    }

    private void loadOffers() {
        File file = new File(plugin.getDataFolder(), "trades.yml");
        if (!file.exists()) return;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        this.nextId = cfg.getInt("next-id", 1);

        ConfigurationSection offersSection = cfg.getConfigurationSection("offers");
        if (offersSection == null) return;

        for (String key : offersSection.getKeys(false)) {
            ConfigurationSection os = offersSection.getConfigurationSection(key);
            if (os == null) continue;

            UUID seller = UUID.fromString(os.getString("seller", ""));
            String islandId = os.getString("island-id", "");
            ItemStack offering = os.getItemStack("offering");
            double price = os.getDouble("price", 0.0);
            long timestamp = os.getLong("timestamp", 0L);

            if (offering == null) continue;

            TradeOfferImpl offer = new TradeOfferImpl(key, seller, islandId, offering, price, timestamp);
            offers.put(key, offer);
        }

        plugin.getLogger().info("Loaded " + offers.size() + " trade offers.");
    }
}
