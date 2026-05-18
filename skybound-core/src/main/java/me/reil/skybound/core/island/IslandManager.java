package me.reil.skybound.core.island;

import me.reil.skybound.api.event.IslandCreateEvent;
import me.reil.skybound.api.event.IslandDeleteEvent;
import me.reil.skybound.api.event.IslandLevelUpEvent;
import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandProvider;
import me.reil.skybound.core.config.CoreConfig;
import me.reil.skybound.core.storage.StorageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class IslandManager implements IslandProvider {

    private final JavaPlugin plugin;
    private final CoreConfig config;
    private final StorageManager storage;
    private final Map<String, IslandImpl> islands = new LinkedHashMap<String, IslandImpl>();
    private final Map<UUID, String> playerIslandMap = new LinkedHashMap<UUID, String>();
    private int nextGridIndex = 0;

    public IslandManager(JavaPlugin plugin, CoreConfig config, StorageManager storage) {
        this.plugin = plugin;
        this.config = config;
        this.storage = storage;
        loadData();
    }

    private void loadData() {
        Map<String, IslandImpl> loaded = storage.loadIslands();
        for (Map.Entry<String, IslandImpl> entry : loaded.entrySet()) {
            islands.put(entry.getKey(), entry.getValue());
            for (UUID member : entry.getValue().getMembers()) {
                playerIslandMap.put(member, entry.getKey());
            }
        }
        nextGridIndex = islands.size();
        plugin.getLogger().info("Loaded " + islands.size() + " islands.");
    }

    public void saveData() {
        storage.saveIslands(islands);
    }

    @Override
    public Island getIsland(String islandId) {
        return islands.get(islandId);
    }

    @Override
    public Island getPlayerIsland(UUID playerId) {
        String islandId = playerIslandMap.get(playerId);
        return islandId == null ? null : islands.get(islandId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Island> getAllIslands() {
        return Collections.unmodifiableCollection((Collection<? extends Island>) (Collection<?>) islands.values());
    }

    @Override
    public Island createIsland(Player owner, String schematicName) {
        if (getPlayerIsland(owner.getUniqueId()) != null) {
            return null;
        }

        // Normalize schematic name — use "desert" as default if null/empty
        if (schematicName == null || schematicName.isEmpty()) {
            schematicName = "desert";
        }

        String islandId = UUID.randomUUID().toString().substring(0, 8);
        Location center = calculateNextCenter();

        IslandImpl island = new IslandImpl(islandId, owner.getUniqueId(), center, config.getDefaultRadius());
        island.setName(owner.getName());
        island.setHome(center.clone().add(0.5, 1, 0.5));

        plugin.getLogger().info("Island created: " + islandId + " for " + owner.getName()
                + " (schematic: " + schematicName + ")"
                + " world=" + (center.getWorld() != null ? center.getWorld().getName() : "NULL")
                + " at " + center.getBlockX() + "," + center.getBlockY() + "," + center.getBlockZ());

        // Register island in maps FIRST so addons can find it via API
        islands.put(islandId, island);
        playerIslandMap.put(owner.getUniqueId(), islandId);

        // Fire event AFTER island is registered — addons (island-core) listen to this
        IslandCreateEvent event = new IslandCreateEvent(owner, island, schematicName);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            // Rollback registration
            islands.remove(islandId);
            playerIslandMap.remove(owner.getUniqueId());
            return null;
        }

        // Direct notification to Island Core addon (backup if event doesn't reach)
        try {
            org.bukkit.plugin.Plugin islandCorePlugin = Bukkit.getPluginManager().getPlugin("SkyBound-IslandCore");
            if (islandCorePlugin != null && islandCorePlugin.isEnabled()) {
                plugin.getLogger().info("Notifying IslandCore addon...");
                // Give cores directly via scheduled task (ensures plugin is fully ready)
                final org.bukkit.entity.Player finalOwner = owner;
                Bukkit.getScheduler().runTaskLater((org.bukkit.plugin.java.JavaPlugin) plugin, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            java.lang.reflect.Method m = islandCorePlugin.getClass().getMethod("onIslandCreated", org.bukkit.entity.Player.class, String.class);
                            m.setAccessible(true);
                            m.invoke(islandCorePlugin, finalOwner, islandId);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to notify IslandCore: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                        }
                    }
                }, 2L);
            } else {
                plugin.getLogger().info("IslandCore addon not found or not enabled.");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("IslandCore notification error: " + e.getMessage());
        }

        plugin.getLogger().info("Island created: " + islandId + " for " + owner.getName() + " (schematic: " + schematicName + ")");
        saveData();
        return island;
    }

    @Override
    public boolean deleteIsland(String islandId) {
        IslandImpl island = islands.get(islandId);
        if (island == null) return false;

        IslandDeleteEvent event = new IslandDeleteEvent(island, island.getOwner());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        // Teleport all online members to spawn
        Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
        for (UUID member : island.getMembers()) {
            Player p = Bukkit.getPlayer(member);
            if (p != null && p.isOnline()) {
                p.teleport(spawn);
                p.sendMessage("\u00a7e\u2726 Остров удалён.");
            }
            playerIslandMap.remove(member);
        }

        // Clear island blocks asynchronously on next tick
        final IslandImpl toDelete = island;
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                clearIslandBlocks(toDelete);
            }
        });

        islands.remove(islandId);
        saveData();
        plugin.getLogger().info("Island deleted: " + islandId);
        return true;
    }

    @Override
    public boolean regenerateIsland(String islandId, String schematicName) {
        IslandImpl island = islands.get(islandId);
        if (island == null) return false;
        // Clear blocks in radius
        clearIslandBlocks(island);
        // Schematic paste is handled by the caller (IslandCommand.cmdRegen)
        plugin.getLogger().info("Island regenerated: " + islandId + " (schematic: " + schematicName + ")");

        // Notify Island Core addon to give cores
        try {
            org.bukkit.plugin.Plugin islandCorePlugin = Bukkit.getPluginManager().getPlugin("SkyBound-IslandCore");
            if (islandCorePlugin != null && islandCorePlugin.isEnabled()) {
                org.bukkit.entity.Player owner = Bukkit.getPlayer(island.getOwner());
                if (owner != null) {
                    final org.bukkit.entity.Player finalOwner = owner;
                    Bukkit.getScheduler().runTaskLater((org.bukkit.plugin.java.JavaPlugin) plugin, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                java.lang.reflect.Method m = islandCorePlugin.getClass().getMethod("onIslandCreated", org.bukkit.entity.Player.class, String.class);
                                m.setAccessible(true);
                                m.invoke(islandCorePlugin, finalOwner, islandId);
                            } catch (Exception e) {
                                plugin.getLogger().warning("Failed to notify IslandCore on regen: " + e.getMessage());
                            }
                        }
                    }, 2L);
                }
            }
        } catch (Exception ignored) {}

        saveData();
        return true;
    }

    @Override
    public Island getIslandAt(Location location) {
        if (location == null || location.getWorld() == null) return null;
        String worldName = location.getWorld().getName();
        if (!worldName.startsWith(config.getIslandWorldName())) return null;

        for (IslandImpl island : islands.values()) {
            if (island.isWithinBounds(location)) {
                return island;
            }
        }
        return null;
    }

    @Override
    public Location getIslandHome(String islandId) {
        Island island = islands.get(islandId);
        return island == null ? null : island.getHome();
    }

    @Override
    public void setIslandHome(String islandId, Location location) {
        IslandImpl island = islands.get(islandId);
        if (island != null) {
            island.setHome(location);
        }
    }

    @Override
    public int getIslandCount() {
        return islands.size();
    }

    public void registerMember(UUID playerId, String islandId) {
        playerIslandMap.put(playerId, islandId);
    }

    public void unregisterMember(UUID playerId) {
        playerIslandMap.remove(playerId);
    }

    /**
     * Recalculate island value based on placed blocks.
     */
    public double recalculateValue(IslandImpl island) {
        Location center = island.getCenter();
        World world = center.getWorld();
        if (world == null) return 0.0;

        int radius = island.getRadius();
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        double totalValue = 0.0;

        Map<String, Integer> blockValues = config.getBlockValues();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                for (int y = 0; y < world.getMaxHeight(); y++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material mat = block.getType();
                    if (mat == Material.AIR) continue;
                    Integer value = blockValues.get(mat.name());
                    if (value != null) {
                        totalValue += value;
                    }
                }
            }
        }

        island.setValue(totalValue);
        return totalValue;
    }

    /**
     * Add XP and check for level up.
     */
    public void addExperience(IslandImpl island, long amount) {
        addExperience(island, amount, false);
    }

    /**
     * Add XP and check for level up.
     * @param passive true if this is passive XP (from generators, block placement, etc.)
     */
    public void addExperience(IslandImpl island, long amount, boolean passive) {
        // Check if passive XP should be blocked by island-core addon
        if (passive && config.isDisablePassiveXpIfAddon()) {
            if (me.reil.skybound.api.SkyBoundAPI.isAvailable()) {
                me.reil.skybound.api.SkyBoundAPI api = me.reil.skybound.api.SkyBoundAPI.get();
                if (api.hasService(me.reil.skybound.api.addon.AddonRegistry.class)) {
                    me.reil.skybound.api.addon.AddonRegistry registry = api.getService(me.reil.skybound.api.addon.AddonRegistry.class);
                    if (registry.isRegistered("island-core")) {
                        return; // XP only through core blocks
                    }
                }
            }
        }

        int oldLevel = island.getLevel();
        island.addExperience(amount);

        // Check level up
        long xp = island.getExperience();
        int xpPerLevel = config.getXpPerLevel();
        int newLevel = (int) (xp / xpPerLevel) + 1;

        if (newLevel > oldLevel) {
            island.setLevel(newLevel);
            IslandLevelUpEvent event = new IslandLevelUpEvent(island, oldLevel, newLevel);
            Bukkit.getPluginManager().callEvent(event);

            // Notify online members
            for (UUID memberId : island.getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null) {
                    member.sendMessage("\u00a76\u00a7lIsland Level Up! \u00a7e" + oldLevel + " \u00a77\u2192 \u00a7a" + newLevel);
                }
            }
        }
    }

    /**
     * Get internal map for persistence.
     */
    public Map<String, IslandImpl> getIslandsMap() {
        return islands;
    }

    public void clearIslandBlocks(IslandImpl island) {
        Location center = island.getCenter();
        World world = center.getWorld();
        if (world == null) return;
        int radius = island.getRadius();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                for (int y = 0; y < world.getMaxHeight(); y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
    }

    private Location calculateNextCenter() {
        World world = Bukkit.getWorld(config.getIslandWorldName());
        if (world == null) {
            // Try to create the world if it doesn't exist
            plugin.getLogger().warning("Island world '" + config.getIslandWorldName() + "' not found! Using default world.");
            world = Bukkit.getWorlds().get(0);
        }
        int spacing = config.getIslandSpacing();
        int baseY = config.getBaseY();

        int index = nextGridIndex++;
        int x = 0, z = 0;
        if (index > 0) {
            int layer = (int) Math.ceil((Math.sqrt(index + 1) - 1) / 2.0);
            int leg = (int) Math.floor((index - (2 * layer - 1) * (2 * layer - 1)) / (2.0 * layer));
            int offset = index - (2 * layer - 1) * (2 * layer - 1) - 2 * layer * leg;

            switch (leg) {
                case 0: x = layer; z = -layer + offset + 1; break;
                case 1: x = layer - offset - 1; z = layer; break;
                case 2: x = -layer; z = layer - offset - 1; break;
                case 3: x = -layer + offset + 1; z = -layer; break;
            }
        }

        return new Location(world, x * spacing, baseY, z * spacing);
    }
}
