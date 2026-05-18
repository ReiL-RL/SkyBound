package me.reil.skybound.core.lang;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Localization manager.
 * Loads messages from lang_XX.yml files.
 */
public final class LangManager {

    private final JavaPlugin plugin;
    private final Map<String, String> messages = new LinkedHashMap<String, String>();
    private String language;

    public LangManager(JavaPlugin plugin, String language) {
        this.plugin = plugin;
        this.language = language;
        load();
    }

    public void reload() {
        messages.clear();
        load();
    }

    public String get(String key) {
        String msg = messages.get(key);
        if (msg == null) return key;
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public String get(String key, String... replacements) {
        String msg = get(key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    public void send(Player player, String key, String... replacements) {
        String msg = get(key, replacements);
        if (!msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }

    private void load() {
        String fileName = "lang_" + language + ".yml";
        File file = new File(plugin.getDataFolder(), "lang/" + fileName);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            if (plugin.getResource("lang/" + fileName) != null) {
                plugin.saveResource("lang/" + fileName, false);
            } else {
                // Save default English
                plugin.saveResource("lang/lang_en.yml", false);
                file = new File(plugin.getDataFolder(), "lang/lang_en.yml");
            }
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String key : cfg.getKeys(true)) {
            if (cfg.isString(key)) {
                messages.put(key, cfg.getString(key));
            }
        }

        plugin.getLogger().info("Loaded " + messages.size() + " messages (" + language + ").");
    }
}
