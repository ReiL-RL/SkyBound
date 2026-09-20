package me.reil.skybound.core.island;

import me.reil.skybound.api.event.IslandCreateEvent;
import me.reil.skybound.api.event.IslandDeleteEvent;
import me.reil.skybound.api.event.IslandLevelUpEvent;
import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandProvider;
import me.reil.skybound.api.island.IslandRole;
import me.reil.skybound.core.config.CoreConfig;
import me.reil.skybound.core.storage.StorageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

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
    private final Map<String, String> islandGridIndex = new LinkedHashMap<String, String>();
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
            indexIsland(entry.getValue());
            for (UUID member : entry.getValue().getMembers()) {
                if (entry.getValue().getMemberRole(member).isAtLeast(IslandRole.MEMBER)) {
                    playerIslandMap.put(member, entry.getKey());
                }
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
        indexIsland(island);
        playerIslandMap.put(owner.getUniqueId(), islandId);

        // Fire event AFTER island is registered — addons (island-core) listen to this
        IslandCreateEvent event = new IslandCreateEvent(owner, island, schematicName);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            // Rollback registration
            islands.remove(islandId);
            unindexIsland(island);
            playerIslandMap.remove(owner.getUniqueId());
            return null;
        }

        // The addon (SkyBound-IslandCore) places its cores by listening to
        // IslandCreateEvent above. We must NOT also call onIslandCreated() directly
        // here — doing both makes the addon place TWO XP cores. The event is the
        // single source of truth on creation.

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
                sendLang(p, "island.deleted");
            }
            if (island.getMemberRole(member).isAtLeast(IslandRole.MEMBER)) {
                playerIslandMap.remove(member);
            }
        }

        // Clear island blocks asynchronously on next tick
        final IslandImpl toDelete = island;
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                clearIslandBlocksBatched(toDelete, null);
            }
        });

        islands.remove(islandId);
        unindexIsland(island);
        saveData();
        plugin.getLogger().info("Island deleted: " + islandId);
        return true;
    }

    @Override
    public boolean regenerateIsland(String islandId, String schematicName) {
        IslandImpl island = islands.get(islandId);
        if (island == null) return false;

        clearIslandBlocks(island);
        finishRegeneration(island, schematicName);
        return true;
    }

    public boolean regenerateIslandBatched(final String islandId, final String schematicName, final Runnable done) {
        final IslandImpl island = islands.get(islandId);
        if (island == null) return false;

        clearIslandBlocksBatched(island, new Runnable() {
            @Override
            public void run() {
                finishRegeneration(island, schematicName);
                if (done != null) {
                    done.run();
                }
            }
        });
        return true;
    }

    private void finishRegeneration(final IslandImpl island, String schematicName) {
        plugin.getLogger().info("Island regenerated: " + island.getId() + " (schematic: " + schematicName + ")");
        notifyIslandCoreOnRegen(island);
        saveData();
    }

    private void notifyIslandCoreOnRegen(final IslandImpl island) {
        try {
            final org.bukkit.plugin.Plugin islandCorePlugin = Bukkit.getPluginManager().getPlugin("SkyBound-IslandCore");
            if (islandCorePlugin != null && islandCorePlugin.isEnabled()) {
                org.bukkit.entity.Player owner = Bukkit.getPlayer(island.getOwner());
                if (owner != null) {
                    final org.bukkit.entity.Player finalOwner = owner;
                    Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                java.lang.reflect.Method m = islandCorePlugin.getClass().getMethod("onIslandCreated", org.bukkit.entity.Player.class, String.class);
                                m.setAccessible(true);
                                m.invoke(islandCorePlugin, finalOwner, island.getId());
                            } catch (Exception e) {
                                plugin.getLogger().warning("Failed to notify IslandCore on regen: " + e.getMessage());
                            }
                        }
                    }, 2L);
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public Island getIslandAt(Location location) {
        if (location == null || location.getWorld() == null) return null;
        String worldName = location.getWorld().getName();
        if (!worldName.startsWith(config.getIslandWorldName())) return null;

        int spacing = Math.max(1, config.getIslandSpacing());
        int gridX = (int) Math.round(location.getX() / spacing);
        int gridZ = (int) Math.round(location.getZ() / spacing);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                String islandId = islandGridIndex.get(gridKey(worldName, gridX + dx, gridZ + dz));
                IslandImpl island = islandId == null ? null : islands.get(islandId);
                if (island != null && island.isWithinBounds(location)) {
                    return island;
                }
            }
        }

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

    public void unregisterMember(UUID playerId, String islandId) {
        if (playerId == null || islandId == null) return;
        String currentIslandId = playerIslandMap.get(playerId);
        if (islandId.equals(currentIslandId)) {
            playerIslandMap.remove(playerId);
        }
    }

    /**
     * Recalculate island value based on placed blocks.
     * If the island-core addon is registered AND configured to drive value,
     * we skip the block scan and keep the accumulated value (set via {@link #addValue(IslandImpl, double)}).
     */
    public double recalculateValue(IslandImpl island) {
        // When the island-core addon is active, value is accumulated via XP Core deposits.
        // Don't override that with a block scan.
        if (isIslandCoreActive()) {
            return island.getValue();
        }

        Location center = island.getCenter();
        World world = center.getWorld();
        if (world == null) return 0.0;

        int radius = island.getRadius();
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        int minY = getMinHeight(world);
        int maxY = world.getMaxHeight();
        double totalValue = 0.0;

        Map<String, Integer> blockValues = config.getBlockValues();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                for (int y = minY; y < maxY; y++) {
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

    public boolean recalculateValueBatched(final IslandImpl island, final Runnable done) {
        if (island == null) return false;
        if (isIslandCoreActive()) {
            if (done != null) done.run();
            return true;
        }

        Location center = island.getCenter();
        final World world = center.getWorld();
        if (world == null) return false;

        final int radius = island.getRadius();
        final int minX = center.getBlockX() - radius;
        final int maxX = center.getBlockX() + radius;
        final int minZ = center.getBlockZ() - radius;
        final int maxZ = center.getBlockZ() + radius;
        final int minY = getMinHeight(world);
        final int maxY = world.getMaxHeight();
        final int blocksPerTick = Math.max(1, plugin.getConfig().getInt("performance.value-blocks-per-tick", 4000));
        final Map<String, Integer> blockValues = config.getBlockValues();

        new BukkitRunnable() {
            private int x = minX;
            private int z = minZ;
            private int y = minY;
            private double totalValue = 0.0;

            @Override
            public void run() {
                int processed = 0;
                while (x <= maxX && processed < blocksPerTick) {
                    Material mat = world.getBlockAt(x, y, z).getType();
                    if (mat != Material.AIR) {
                        Integer value = blockValues.get(mat.name());
                        if (value != null) {
                            totalValue += value;
                        }
                    }

                    processed++;
                    y++;
                    if (y >= maxY) {
                        y = minY;
                        z++;
                        if (z > maxZ) {
                            z = minZ;
                            x++;
                        }
                    }
                }

                if (x > maxX) {
                    island.setValue(totalValue);
                    if (done != null) done.run();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
        return true;
    }

    /**
     * Add to island value (used by Island Core XP Core deposits).
     */
    public void addValue(IslandImpl island, double amount) {
        if (amount <= 0) return;
        island.setValue(island.getValue() + amount);
    }

    /**
     * Check if the island-core addon is registered.
     */
    public boolean isIslandCoreActive() {
        try {
            if (me.reil.skybound.api.SkyBoundAPI.isAvailable()) {
                me.reil.skybound.api.SkyBoundAPI api = me.reil.skybound.api.SkyBoundAPI.get();
                if (api.hasService(me.reil.skybound.api.addon.AddonRegistry.class)) {
                    return api.getService(me.reil.skybound.api.addon.AddonRegistry.class).isRegistered("island-core");
                }
            }
        } catch (Throwable ignored) {}
        return false;
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
                    sendLang(member, "island.level-up",
                            "{old}", String.valueOf(oldLevel),
                            "{new}", String.valueOf(newLevel));
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
        int cz = center.getBlockZ();
        int minY = getMinHeight(world);
        int maxY = world.getMaxHeight();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                for (int y = minY; y < maxY; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }

        // Also clear all entities on the island
        clearIslandEntities(island);
    }

    public void clearIslandBlocksBatched(final IslandImpl island, final Runnable done) {
        Location center = island.getCenter();
        final World world = center.getWorld();
        if (world == null) {
            if (done != null) done.run();
            return;
        }

        final int radius = island.getRadius();
        final int minX = center.getBlockX() - radius;
        final int maxX = center.getBlockX() + radius;
        final int minZ = center.getBlockZ() - radius;
        final int maxZ = center.getBlockZ() + radius;
        final int minY = getMinHeight(world);
        final int maxY = world.getMaxHeight();
        final int blocksPerTick = Math.max(1, plugin.getConfig().getInt("performance.regen-blocks-per-tick", 4000));

        new BukkitRunnable() {
            private int x = minX;
            private int z = minZ;
            private int y = minY;

            @Override
            public void run() {
                int processed = 0;
                while (x <= maxX && processed < blocksPerTick) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                    processed++;

                    y++;
                    if (y >= maxY) {
                        y = minY;
                        z++;
                        if (z > maxZ) {
                            z = minZ;
                            x++;
                        }
                    }
                }

                if (x > maxX) {
                    clearIslandEntities(island);
                    if (done != null) done.run();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /**
     * Удаляет все сущности на острове (кроме игроков), чтобы они не накапливались.
     * Очищает: мобов, животных, дропы, рамки, картины, армор-стенды и т.д.
     */
    public void clearIslandEntities(IslandImpl island) {
        Location center = island.getCenter();
        World world = center.getWorld();
        if (world == null) return;
        int radius = island.getRadius();
        int cx = center.getBlockX();
        int cz = center.getBlockZ();

        int removed = 0;
        for (org.bukkit.entity.Entity entity : world.getNearbyEntities(
                center,
                radius + 1.0,
                Math.max(1.0, world.getMaxHeight() - getMinHeight(world)),
                radius + 1.0)) {
            // Skip players
            if (entity instanceof org.bukkit.entity.Player) continue;

            Location eLoc = entity.getLocation();
            int ex = eLoc.getBlockX();
            int ez = eLoc.getBlockZ();

            // Check if entity is within island bounds
            if (ex >= cx - radius && ex <= cx + radius
                    && ez >= cz - radius && ez <= cz + radius) {
                entity.remove();
                removed++;
            }
        }

        if (removed > 0) {
            plugin.getLogger().info("Removed " + removed + " entities from island " + island.getId());
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

    private int getMinHeight(World world) {
        try {
            Object value = world.getClass().getMethod("getMinHeight").invoke(world);
            if (value instanceof Integer) {
                return ((Integer) value).intValue();
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private void indexIsland(IslandImpl island) {
        Location center = island.getCenter();
        World world = center.getWorld();
        if (world == null) return;

        int spacing = Math.max(1, config.getIslandSpacing());
        int gridX = (int) Math.round(center.getX() / spacing);
        int gridZ = (int) Math.round(center.getZ() / spacing);
        islandGridIndex.put(gridKey(world.getName(), gridX, gridZ), island.getId());
    }

    private void unindexIsland(IslandImpl island) {
        Location center = island.getCenter();
        World world = center.getWorld();
        if (world == null) return;

        int spacing = Math.max(1, config.getIslandSpacing());
        int gridX = (int) Math.round(center.getX() / spacing);
        int gridZ = (int) Math.round(center.getZ() / spacing);
        islandGridIndex.remove(gridKey(world.getName(), gridX, gridZ));
    }

    private String gridKey(String worldName, int gridX, int gridZ) {
        return worldName + ":" + gridX + ":" + gridZ;
    }

    private void sendLang(Player player, String key, String... replacements) {
        if (plugin instanceof me.reil.skybound.core.SkyBoundPlugin) {
            ((me.reil.skybound.core.SkyBoundPlugin) plugin).getLangManager().send(player, key, replacements);
        }
    }
}
