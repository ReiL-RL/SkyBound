package me.reil.skybound.core.island;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reviews and ratings (1-5 stars) for islands.
 * Each player can leave one review per island. Editing replaces the previous review.
 */
public final class IslandReviewManager {

    public static final class Review {
        public final UUID author;
        public final String authorName;
        public final int stars;        // 1..5
        public final String comment;   // optional
        public final long timestamp;

        public Review(UUID author, String authorName, int stars, String comment, long timestamp) {
            this.author = author;
            this.authorName = authorName;
            this.stars = stars;
            this.comment = comment == null ? "" : comment;
            this.timestamp = timestamp;
        }
    }

    private final JavaPlugin plugin;
    private final File dataFile;
    /** islandId -> list of reviews (one per author). */
    private final Map<String, List<Review>> reviews = new LinkedHashMap<String, List<Review>>();

    public IslandReviewManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data/island-reviews.yml");
        load();
    }

    /** Replace or insert review by this author. */
    public void submitReview(String islandId, UUID author, String authorName, int stars, String comment) {
        if (stars < 1) stars = 1;
        if (stars > 5) stars = 5;
        List<Review> list = reviews.get(islandId);
        if (list == null) {
            list = new ArrayList<Review>();
            reviews.put(islandId, list);
        }
        // Remove old review by same author
        list.removeIf(r -> r.author.equals(author));
        list.add(new Review(author, authorName, stars, comment, System.currentTimeMillis()));
        save();
    }

    public List<Review> getReviews(String islandId) {
        List<Review> list = reviews.get(islandId);
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    public Review getReviewBy(String islandId, UUID author) {
        List<Review> list = reviews.get(islandId);
        if (list == null) return null;
        for (Review r : list) {
            if (r.author.equals(author)) return r;
        }
        return null;
    }

    public double getAverageRating(String islandId) {
        List<Review> list = reviews.get(islandId);
        if (list == null || list.isEmpty()) return 0.0;
        long sum = 0;
        for (Review r : list) sum += r.stars;
        return (double) sum / list.size();
    }

    public int getReviewCount(String islandId) {
        List<Review> list = reviews.get(islandId);
        return list == null ? 0 : list.size();
    }

    public void delete(String islandId, UUID author) {
        List<Review> list = reviews.get(islandId);
        if (list == null) return;
        list.removeIf(r -> r.author.equals(author));
        if (list.isEmpty()) reviews.remove(islandId);
        save();
    }

    public void removeIsland(String islandId) {
        if (islandId == null || islandId.isEmpty()) return;
        if (reviews.remove(islandId) != null) {
            save();
        }
    }

    // --- Persistence ---

    public void load() {
        reviews.clear();
        if (!dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection sec = cfg.getConfigurationSection("reviews");
        if (sec == null) return;
        for (String islandId : sec.getKeys(false)) {
            List<?> raw = sec.getList(islandId);
            if (raw == null) continue;
            List<Review> list = new ArrayList<Review>();
            for (Object o : raw) {
                if (!(o instanceof Map)) continue;
                Map<?, ?> m = (Map<?, ?>) o;
                try {
                    UUID author = UUID.fromString(String.valueOf(m.get("author")));
                    String name = String.valueOf(m.get("name"));
                    int stars = Integer.parseInt(String.valueOf(m.get("stars")));
                    if (stars < 1) stars = 1;
                    if (stars > 5) stars = 5;
                    String comment = m.containsKey("comment") ? String.valueOf(m.get("comment")) : "";
                    long ts = Long.parseLong(String.valueOf(m.get("timestamp")));
                    list.add(new Review(author, name, stars, comment, ts));
                } catch (Exception e) {
                    plugin.getLogger().warning("Ignoring invalid review for island '" + islandId + "': " + e.getMessage());
                }
            }
            if (!list.isEmpty()) reviews.put(islandId, list);
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, List<Review>> e : reviews.entrySet()) {
            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            for (Review r : e.getValue()) {
                Map<String, Object> m = new LinkedHashMap<String, Object>();
                m.put("author", r.author.toString());
                m.put("name", r.authorName);
                m.put("stars", r.stars);
                m.put("comment", r.comment);
                m.put("timestamp", r.timestamp);
                list.add(m);
            }
            cfg.set("reviews." + e.getKey(), list);
        }
        me.reil.skybound.core.storage.YamlFiles.saveAtomically(plugin, cfg, dataFile, "island-reviews.yml");
    }
}
