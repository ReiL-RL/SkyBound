package me.reil.skybound.core.island;

import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cross-island gifts: a player can send a single ItemStack to another player.
 * Daily limit per sender. Recipient receives a notification when online and
 * picks gifts up via /is gifts.
 */
public final class IslandGiftManager {

    public static final int DEFAULT_DAILY_LIMIT = 3;
    public static final int MAX_PENDING_GIFTS = 50;

    private final JavaPlugin plugin;
    private final File dataFile;

    /** recipient UUID -> pending gifts queue (each gift is a serialized ItemStack + sender + timestamp) */
    private final Map<UUID, List<Gift>> pending = new LinkedHashMap<UUID, List<Gift>>();
    /** sender UUID -> last reset day index */
    private final Map<UUID, Integer> dayIndex = new LinkedHashMap<UUID, Integer>();
    /** sender UUID -> sends today */
    private final Map<UUID, Integer> sentToday = new LinkedHashMap<UUID, Integer>();

    public IslandGiftManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data/gifts.yml");
        load();
    }

    public int getDailyLimit() {
        return plugin.getConfig().getInt("gifts.daily-limit", DEFAULT_DAILY_LIMIT);
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("gifts.enabled", true);
    }

    /** Returns gifts sent today by this player (auto-resets at day rollover). */
    public int getSentToday(UUID sender) {
        rolloverIfNeeded(sender);
        Integer s = sentToday.get(sender);
        return s == null ? 0 : s;
    }

    public int getRemainingToday(UUID sender) {
        return Math.max(0, getDailyLimit() - getSentToday(sender));
    }

    /**
     * Send a gift from sender to recipient.
     * @return null on success, or a localization key for the error
     */
    public String sendGift(Player sender, OfflinePlayer recipient, ItemStack item) {
        if (!isEnabled()) return "gift.disabled";
        if (item == null || item.getType().isAir()) return "gift.empty-hand";
        if (recipient == null) return "player-not-found";
        if (sender.getUniqueId().equals(recipient.getUniqueId())) return "gift.self";

        rolloverIfNeeded(sender.getUniqueId());
        int sent = sentToday.getOrDefault(sender.getUniqueId(), 0);
        int limit = getDailyLimit();
        if (sent >= limit) return "gift.limit-reached";

        List<Gift> queue = pending.get(recipient.getUniqueId());
        if (queue == null) {
            queue = new ArrayList<Gift>();
            pending.put(recipient.getUniqueId(), queue);
        }
        if (queue.size() >= MAX_PENDING_GIFTS) {
            return "gift.recipient-full";
        }

        Gift gift = new Gift(sender.getUniqueId(), sender.getName(), item.clone(), System.currentTimeMillis());
        queue.add(gift);
        sentToday.put(sender.getUniqueId(), sent + 1);
        save();

        // Notify online recipient
        Player onlineRecipient = recipient.getPlayer();
        if (onlineRecipient != null) {
            lang().send(onlineRecipient, "gift.received", "{player}", sender.getName());
        }
        return null;
    }

    private me.reil.skybound.core.lang.LangManager lang() {
        return ((SkyBoundPlugin) plugin).getLangManager();
    }

    /** Get pending gifts for recipient (mutable list — modify with caution). */
    public List<Gift> getPending(UUID recipient) {
        List<Gift> q = pending.get(recipient);
        return q == null ? new ArrayList<Gift>() : new ArrayList<Gift>(q);
    }

    /**
     * Try to claim a single gift by index. Returns the item if successful (caller adds to inventory),
     * or null if invalid index / queue empty.
     */
    public ItemStack claim(UUID recipient, int index) {
        List<Gift> q = pending.get(recipient);
        if (q == null || index < 0 || index >= q.size()) return null;
        Gift g = q.remove(index);
        if (q.isEmpty()) pending.remove(recipient);
        save();
        return g.item;
    }

    /** Pop all gifts; returns the list of items. */
    public List<ItemStack> claimAll(UUID recipient) {
        List<ItemStack> out = new ArrayList<ItemStack>();
        List<Gift> q = pending.remove(recipient);
        if (q != null) {
            for (Gift g : q) out.add(g.item);
            save();
        }
        return out;
    }

    private void rolloverIfNeeded(UUID sender) {
        int today = currentDayIndex();
        Integer last = dayIndex.get(sender);
        if (last == null || last != today) {
            dayIndex.put(sender, today);
            sentToday.put(sender, 0);
        }
    }

    private int currentDayIndex() {
        return (int) (System.currentTimeMillis() / 86_400_000L);
    }

    // --- Persistence ---

    public static final class Gift {
        public final UUID senderId;
        public final String senderName;
        public final ItemStack item;
        public final long timestamp;

        public Gift(UUID senderId, String senderName, ItemStack item, long timestamp) {
            this.senderId = senderId;
            this.senderName = senderName;
            this.item = item;
            this.timestamp = timestamp;
        }
    }

    public void load() {
        pending.clear();
        sentToday.clear();
        dayIndex.clear();
        if (!dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);

        ConfigurationSection p = cfg.getConfigurationSection("pending");
        if (p != null) {
            for (String key : p.getKeys(false)) {
                try {
                    UUID rec = UUID.fromString(key);
                    List<?> raw = p.getList(key);
                    if (raw == null) continue;
                    List<Gift> q = new ArrayList<Gift>();
                    for (Object o : raw) {
                        if (!(o instanceof Map)) continue;
                        Map<?, ?> m = (Map<?, ?>) o;
                        UUID sId = UUID.fromString(String.valueOf(m.get("sender")));
                        String sName = String.valueOf(m.get("sender-name"));
                        long ts = Long.parseLong(String.valueOf(m.get("timestamp")));
                        ItemStack item = deserializeItem(String.valueOf(m.get("item")));
                        if (item == null) continue;
                        q.add(new Gift(sId, sName, item, ts));
                    }
                    if (!q.isEmpty()) pending.put(rec, q);
                } catch (Exception e) {
                    plugin.getLogger().warning("Ignoring invalid pending gift queue for recipient '" + key + "': " + e.getMessage());
                }
            }
        }

        ConfigurationSection counters = cfg.getConfigurationSection("counters");
        if (counters != null) {
            for (String key : counters.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    sentToday.put(id, Math.max(0, counters.getInt(key + ".sent")));
                    dayIndex.put(id, counters.getInt(key + ".day"));
                } catch (Exception e) {
                    plugin.getLogger().warning("Ignoring invalid gift counter for sender '" + key + "': " + e.getMessage());
                }
            }
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, List<Gift>> e : pending.entrySet()) {
            List<Map<String, Object>> serialized = new ArrayList<Map<String, Object>>();
            for (Gift g : e.getValue()) {
                Map<String, Object> m = new LinkedHashMap<String, Object>();
                m.put("sender", g.senderId.toString());
                m.put("sender-name", g.senderName);
                m.put("timestamp", g.timestamp);
                m.put("item", serializeItem(g.item));
                serialized.add(m);
            }
            cfg.set("pending." + e.getKey(), serialized);
        }
        for (Map.Entry<UUID, Integer> e : sentToday.entrySet()) {
            cfg.set("counters." + e.getKey() + ".sent", e.getValue());
            cfg.set("counters." + e.getKey() + ".day", dayIndex.getOrDefault(e.getKey(), currentDayIndex()));
        }
        me.reil.skybound.core.storage.YamlFiles.saveAtomically(plugin, cfg, dataFile, "gifts.yml");
    }

    private static String serializeItem(ItemStack item) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new BukkitObjectOutputStream(bos)) {
            oos.writeObject(item);
            oos.flush();
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

    private static ItemStack deserializeItem(String data) {
        if (data == null || data.isEmpty()) return null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             ObjectInputStream ois = new BukkitObjectInputStream(bis)) {
            Object o = ois.readObject();
            return o instanceof ItemStack ? (ItemStack) o : null;
        } catch (Exception e) {
            return null;
        }
    }
}
