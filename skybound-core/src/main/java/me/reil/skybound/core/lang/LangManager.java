package me.reil.skybound.core.lang;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Localization manager.
 * Loads messages from lang_XX.yml files.
 *
 * Supports interactive markup in messages:
 *   [click:/is delete|&a&lПодтвердить]    — clickable button that runs a command
 *   [suggest:/is delete|&aПодтвердить]    — clickable button that puts command in chat
 *   [hover:&a&lText|tooltip line]         — text with hover tooltip
 *
 * If a message contains any of these tags it is sent as a BaseComponent
 * (Spigot built-in chat components, no extra libraries needed). Otherwise
 * it falls back to a plain string with `&` colour codes.
 */
public final class LangManager {

    private static final Pattern TAG_PATTERN = Pattern.compile("\\[(click|suggest|hover):([^|]+)\\|([^\\]]+)\\]");

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

    /**
     * Get a message and replace placeholders. Interactive tags are NOT processed —
     * they remain in the string. Use {@link #send(Player, String, String...)} for that.
     */
    public String get(String key, String... replacements) {
        String raw = messages.get(key);
        if (raw == null) return key;
        for (int i = 0; i < replacements.length - 1; i += 2) {
            raw = raw.replace(replacements[i], replacements[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    /** Get raw message (no colour codes processed) — used for component parsing. */
    private String getRaw(String key, String... replacements) {
        String raw = messages.get(key);
        if (raw == null) return key;
        for (int i = 0; i < replacements.length - 1; i += 2) {
            raw = raw.replace(replacements[i], replacements[i + 1]);
        }
        return raw;
    }

    /**
     * Send a message to the player. If the message contains [click:...] / [suggest:...] / [hover:...]
     * tags, it is sent as a chat component with interactive elements. Otherwise plain text.
     *
     * Pipeline:
     *   1. Plugin placeholders (e.g. {player}) replaced
     *   2. PlaceholderAPI placeholders (%skybound_island_name%) resolved if PAPI is present
     *   3. Interactive tags ([click:...] / [hover:...]) parsed into chat components
     *
     * Because the parser runs LAST, you can put [click:...] inside a placeholder value
     * and it will still be interpreted as a button.
     */
    public void send(Player player, String key, String... replacements) {
        String raw = getRaw(key, replacements);
        if (raw == null || raw.isEmpty()) return;

        // Step 2: resolve PAPI placeholders if PAPI is present
        raw = applyPapi(player, raw);

        if (containsInteractiveTags(raw)) {
            BaseComponent[] components = buildComponents(raw);
            if (components != null) {
                player.spigot().sendMessage(components);
                return;
            }
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', raw));
    }

    /**
     * Apply PlaceholderAPI substitutions if PAPI is loaded. Safe no-op otherwise.
     */
    private String applyPapi(Player player, String raw) {
        if (raw == null) return null;
        // Quick exit if no % present
        if (raw.indexOf('%') < 0) return raw;
        try {
            if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, raw);
            }
        } catch (Throwable ignored) {}
        return raw;
    }

    private static boolean containsInteractiveTags(String s) {
        return s.indexOf("[click:") >= 0 || s.indexOf("[suggest:") >= 0 || s.indexOf("[hover:") >= 0;
    }

    /**
     * Parse a string with [click:...] / [suggest:...] / [hover:...] tags into chat components.
     * Each line (separated by \n) is sent as part of the same component group.
     */
    private BaseComponent[] buildComponents(String raw) {
        try {
            List<BaseComponent> result = new ArrayList<BaseComponent>();
            // Process line by line — each \n splits into separate visible line
            String[] lines = raw.split("\\n", -1);
            for (int li = 0; li < lines.length; li++) {
                String line = lines[li];
                buildLineComponents(line, result);
                if (li < lines.length - 1) {
                    result.add(new TextComponent("\n"));
                }
            }
            return result.toArray(new BaseComponent[0]);
        } catch (Throwable t) {
            // If anything fails, fall back to plain text
            return null;
        }
    }

    private void buildLineComponents(String line, List<BaseComponent> output) {
        Matcher m = TAG_PATTERN.matcher(line);
        int last = 0;
        while (m.find()) {
            // Plain text before tag
            if (m.start() > last) {
                String before = line.substring(last, m.start());
                output.add(plainText(before));
            }
            String type = m.group(1);
            String arg = m.group(2);
            String label = m.group(3);

            TextComponent comp = plainText(label);
            switch (type) {
                case "click":
                    comp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, arg));
                    comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new Text(ChatColor.translateAlternateColorCodes('&', "&7Клик — выполнить:\n&e" + arg))));
                    break;
                case "suggest":
                    comp.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, arg));
                    comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new Text(ChatColor.translateAlternateColorCodes('&', "&7Клик — вставить в чат:\n&e" + arg))));
                    break;
                case "hover":
                    comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new Text(ChatColor.translateAlternateColorCodes('&', arg))));
                    break;
                default:
                    break;
            }
            output.add(comp);
            last = m.end();
        }
        if (last < line.length()) {
            output.add(plainText(line.substring(last)));
        }
    }

    private TextComponent plainText(String legacy) {
        String colored = ChatColor.translateAlternateColorCodes('&', legacy);
        return new TextComponent(TextComponent.fromLegacyText(colored));
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
