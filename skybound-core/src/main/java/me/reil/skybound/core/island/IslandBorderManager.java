package me.reil.skybound.core.island;

import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-island border settings: visibility on/off and color.
 * The actual particles are drawn by {@link me.reil.skybound.core.listener.BorderVisualListener}
 * which queries this manager.
 */
public final class IslandBorderManager {

    public enum BorderColor {
        RED(255, 60, 60),
        GREEN(60, 220, 90),
        BLUE(60, 120, 255),
        YELLOW(255, 230, 60),
        PURPLE(180, 60, 220),
        WHITE(240, 240, 240),
        ORANGE(255, 140, 40);

        public final int r;
        public final int g;
        public final int b;

        BorderColor(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public Color toBukkit() {
            return Color.fromRGB(r, g, b);
        }
    }

    private final JavaPlugin plugin;
    /** islandId -> color */
    private final Map<String, BorderColor> colors = new LinkedHashMap<String, BorderColor>();
    /** islandId -> visible (default: true) */
    private final Map<String, Boolean> visibility = new LinkedHashMap<String, Boolean>();
    private final File dataFile;

    public IslandBorderManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data/island-borders.yml");
        load();
    }

    public BorderColor getColor(String islandId) {
        BorderColor c = colors.get(islandId);
        return c == null ? BorderColor.RED : c;
    }

    public void setColor(String islandId, BorderColor color) {
        colors.put(islandId, color);
        save();
    }

    public BorderColor cycleColor(String islandId) {
        BorderColor cur = getColor(islandId);
        BorderColor[] order = BorderColor.values();
        BorderColor next = order[(cur.ordinal() + 1) % order.length];
        setColor(islandId, next);
        return next;
    }

    /** Result of one click on the border button. */
    public enum BorderCycle { OFF, RED, GREEN, BLUE }

    /**
     * Single-click cycle for the border:
     * off → red → green → blue → off.
     * Only red/green/blue are used because the vanilla world border can only
     * render those three states.
     */
    public BorderCycle cycleState(String islandId) {
        if (!isVisible(islandId)) {
            setColor(islandId, BorderColor.RED);
            setVisible(islandId, true);
            return BorderCycle.RED;
        }
        BorderColor cur = getColor(islandId);
        if (cur == BorderColor.RED) {
            setColor(islandId, BorderColor.GREEN);
            return BorderCycle.GREEN;
        }
        if (cur == BorderColor.GREEN) {
            setColor(islandId, BorderColor.BLUE);
            return BorderCycle.BLUE;
        }
        // BLUE (or anything else) → turn off
        setVisible(islandId, false);
        return BorderCycle.OFF;
    }

    public boolean isVisible(String islandId) {
        Boolean v = visibility.get(islandId);
        return v == null ? true : v;
    }

    public void setVisible(String islandId, boolean visible) {
        visibility.put(islandId, visible);
        save();
    }

    public boolean toggleVisibility(String islandId) {
        boolean newState = !isVisible(islandId);
        setVisible(islandId, newState);
        return newState;
    }

    public void removeIsland(String islandId) {
        if (islandId == null || islandId.isEmpty()) return;
        boolean changed = colors.remove(islandId) != null;
        changed = visibility.remove(islandId) != null || changed;
        if (changed) save();
    }

    // --- Persistence ---

    public void load() {
        colors.clear();
        visibility.clear();
        if (!dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection sec = cfg.getConfigurationSection("borders");
        if (sec == null) return;
        for (String islandId : sec.getKeys(false)) {
            String colorStr = sec.getString(islandId + ".color", "RED");
            boolean vis = sec.getBoolean(islandId + ".visible", true);
            try {
                colors.put(islandId, BorderColor.valueOf(colorStr));
            } catch (IllegalArgumentException e) {
                colors.put(islandId, BorderColor.RED);
            }
            visibility.put(islandId, vis);
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, BorderColor> e : colors.entrySet()) {
            cfg.set("borders." + e.getKey() + ".color", e.getValue().name());
        }
        for (Map.Entry<String, Boolean> e : visibility.entrySet()) {
            cfg.set("borders." + e.getKey() + ".visible", e.getValue());
        }
        me.reil.skybound.core.storage.YamlFiles.saveAtomically(plugin, cfg, dataFile, "island-borders.yml");
    }
}
