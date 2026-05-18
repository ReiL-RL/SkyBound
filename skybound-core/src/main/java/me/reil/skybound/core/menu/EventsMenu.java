package me.reil.skybound.core.menu;

import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Events menu — shows active VoidRift events.
 * Only available when VoidRift plugin is installed.
 * Uses reflection to access VoidRift API without compile-time dependency.
 */
public final class EventsMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final List<EventInfo> events = new ArrayList<EventInfo>();

    public EventsMenu(Player player, SkyBoundPlugin plugin) {
        super(player);
        this.plugin = plugin;
    }

    @Override
    public String getTitle() {
        return lang().get("menu.events.title");
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());
        loadEventsFromVoidRift();

        if (events.isEmpty()) {
            ItemStack noEvents = new ItemStack(Material.BARRIER);
            ItemMeta meta = noEvents.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GRAY + "\u041d\u0435\u0442 \u0430\u043a\u0442\u0438\u0432\u043d\u044b\u0445 \u0441\u043e\u0431\u044b\u0442\u0438\u0439");
                noEvents.setItemMeta(meta);
            }
            inventory.setItem(13, noEvents);
        } else {
            int slot = 10;
            for (EventInfo info : events) {
                if (slot > 16) break;
                Material icon = info.active ? Material.ENDER_EYE : Material.ENDER_PEARL;
                ItemStack item = new ItemStack(icon);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(color(info.displayName));
                    List<String> lore = new ArrayList<String>();
                    lore.add(color(info.description));
                    lore.add("");
                    if (info.active) {
                        lore.add(ChatColor.GREEN + "\u25CF \u0410\u043a\u0442\u0438\u0432\u043d\u043e (" + info.remainingSeconds + "\u0441)");
                        lore.add(ChatColor.GRAY + "\u0418\u0433\u0440\u043e\u043a\u043e\u0432: " + info.participants);
                        lore.add("");
                        lore.add(ChatColor.YELLOW + "\u25B6 /event join " + info.id);
                    } else {
                        lore.add(ChatColor.GRAY + "\u25CB \u041d\u0435\u0430\u043a\u0442\u0438\u0432\u043d\u043e");
                    }
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                inventory.setItem(slot++, item);
            }
        }

        // Back
        inventory.setItem(22, new ItemStack(Material.ARROW));
        ItemMeta backMeta = inventory.getItem(22).getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(lang().get("button.back"));
            inventory.getItem(22).setItemMeta(backMeta);
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.getSlot() == 22) {
            me.reil.skybound.api.island.Island island = plugin.getIslandManager().getPlayerIsland(player.getUniqueId());
            if (island != null) new IslandMainMenu(player, plugin, island).open();
        }
    }

    /**
     * Load event data from VoidRift via reflection.
     */
    private void loadEventsFromVoidRift() {
        Plugin voidRift = Bukkit.getPluginManager().getPlugin("VoidRift");
        if (voidRift == null) return;

        try {
            Method getEventManager = voidRift.getClass().getMethod("getEventManager");
            Object eventManager = getEventManager.invoke(voidRift);

            // Get definitions
            Method getDefinitions = eventManager.getClass().getMethod("getDefinitions");
            Collection<?> definitions = (Collection<?>) getDefinitions.invoke(eventManager);

            for (Object def : definitions) {
                Method getId = def.getClass().getMethod("getId");
                Method getDisplayName = def.getClass().getMethod("getDisplayName");
                Method getDescription = def.getClass().getMethod("getDescription");

                String id = (String) getId.invoke(def);
                String displayName = (String) getDisplayName.invoke(def);
                String description = (String) getDescription.invoke(def);

                // Check if active
                Method getActiveEvent = eventManager.getClass().getMethod("getActiveEvent", String.class);
                Object active = getActiveEvent.invoke(eventManager, id);

                EventInfo info = new EventInfo();
                info.id = id;
                info.displayName = displayName;
                info.description = description;
                info.active = active != null;

                if (active != null) {
                    Method getRemainingSeconds = active.getClass().getMethod("getRemainingSeconds");
                    Method getParticipants = active.getClass().getMethod("getParticipants");
                    info.remainingSeconds = (Long) getRemainingSeconds.invoke(active);
                    info.participants = ((java.util.Set<?>) getParticipants.invoke(active)).size();
                }

                events.add(info);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load VoidRift events: " + e.getMessage());
        }
    }

    private static final class EventInfo {
        String id;
        String displayName;
        String description;
        boolean active;
        long remainingSeconds;
        int participants;
    }
}
