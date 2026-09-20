package me.reil.skybound.core.menu;

import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.Bukkit;
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
                meta.setDisplayName(lang().get("menu.events.empty"));
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
                        lore.add(lang().get("menu.events.active", "{time}", formatTime(info.remainingSeconds)));
                        lore.add(lang().get("menu.events.players", "{count}", String.valueOf(info.participants)));
                        lore.add("");
                        lore.add(lang().get("menu.events.join", "{id}", info.id));
                    } else {
                        lore.add(lang().get("menu.events.inactive"));
                    }
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                inventory.setItem(slot++, item);
            }
        }

        addBackButton(22);
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

    private String formatTime(long seconds) {
        if (seconds <= 0) return lang().get("time.none");
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return lang().get("time.hours-minutes", "{hours}", String.valueOf(h), "{minutes}", String.valueOf(m));
        if (m > 0) return lang().get("time.minutes-seconds", "{minutes}", String.valueOf(m), "{seconds}", String.valueOf(s));
        return lang().get("time.seconds", "{seconds}", String.valueOf(s));
    }
}
