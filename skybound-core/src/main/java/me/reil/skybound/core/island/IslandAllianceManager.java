package me.reil.skybound.core.island;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandProvider;
import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Alliances between islands.
 * One island can be in at most one alliance.
 * Each alliance has a name, leader (island), members (set of island ids),
 * and shared chat (toggle per player).
 */
public final class IslandAllianceManager {

    public static final class Alliance {
        public final String id;
        public String name;
        public String leaderIsland;
        public final Set<String> islands = new LinkedHashSet<String>();
        public final long createdAt;

        public Alliance(String id, String name, String leaderIsland, long createdAt) {
            this.id = id;
            this.name = name;
            this.leaderIsland = leaderIsland;
            this.createdAt = createdAt;
            this.islands.add(leaderIsland);
        }
    }

    private final JavaPlugin plugin;
    private final IslandProvider islandProvider;
    private final File dataFile;

    /** allianceId -> alliance */
    private final Map<String, Alliance> alliances = new LinkedHashMap<String, Alliance>();
    /** islandId -> allianceId */
    private final Map<String, String> islandToAlliance = new LinkedHashMap<String, String>();
    /** pending invites: targetIslandId -> set of allianceIds that invited it */
    private final Map<String, Set<String>> pendingInvites = new LinkedHashMap<String, Set<String>>();
    /** players who have alliance chat toggled on */
    private final Set<UUID> chatToggled = new HashSet<UUID>();

    public IslandAllianceManager(JavaPlugin plugin, IslandProvider islandProvider) {
        this.plugin = plugin;
        this.islandProvider = islandProvider;
        this.dataFile = new File(plugin.getDataFolder(), "data/alliances.yml");
        load();
    }

    public Alliance getByIsland(String islandId) {
        String allianceId = islandToAlliance.get(islandId);
        return allianceId == null ? null : alliances.get(allianceId);
    }

    public Alliance getById(String allianceId) {
        return alliances.get(allianceId);
    }

    /** Create a new alliance led by the player's island. */
    public Alliance create(Player leader, String name) {
        Island island = islandProvider.getPlayerIsland(leader.getUniqueId());
        if (island == null) return null;
        if (!island.getOwner().equals(leader.getUniqueId())) return null;
        if (islandToAlliance.containsKey(island.getId())) return null;

        String id = UUID.randomUUID().toString().substring(0, 8);
        Alliance alliance = new Alliance(id, name, island.getId(), System.currentTimeMillis());
        alliances.put(id, alliance);
        islandToAlliance.put(island.getId(), id);
        save();
        return alliance;
    }

    /** Invite an island to the alliance. */
    public boolean invite(Player inviter, String targetIslandId) {
        Island inviterIsland = islandProvider.getPlayerIsland(inviter.getUniqueId());
        if (inviterIsland == null) return false;
        Alliance alliance = getByIsland(inviterIsland.getId());
        if (alliance == null) return false;
        if (!alliance.leaderIsland.equals(inviterIsland.getId())) return false;
        if (alliance.islands.contains(targetIslandId)) return false;
        if (islandToAlliance.containsKey(targetIslandId)) return false;

        Set<String> set = pendingInvites.get(targetIslandId);
        if (set == null) {
            set = new HashSet<String>();
            pendingInvites.put(targetIslandId, set);
        }
        set.add(alliance.id);
        return true;
    }

    /** Accept invitation. */
    public boolean accept(Player accepter, String allianceId) {
        Island island = islandProvider.getPlayerIsland(accepter.getUniqueId());
        if (island == null || !island.getOwner().equals(accepter.getUniqueId())) return false;

        Set<String> invites = pendingInvites.get(island.getId());
        if (invites == null || !invites.contains(allianceId)) return false;

        Alliance alliance = alliances.get(allianceId);
        if (alliance == null) return false;
        if (islandToAlliance.containsKey(island.getId())) return false;

        alliance.islands.add(island.getId());
        islandToAlliance.put(island.getId(), allianceId);
        invites.clear();
        pendingInvites.remove(island.getId());
        save();
        return true;
    }

    /** Leave the alliance. If leader leaves, alliance dissolves. */
    public boolean leave(Player player) {
        Island island = islandProvider.getPlayerIsland(player.getUniqueId());
        if (island == null) return false;
        Alliance alliance = getByIsland(island.getId());
        if (alliance == null) return false;
        if (!island.getOwner().equals(player.getUniqueId())) return false;

        if (alliance.leaderIsland.equals(island.getId())) {
            // Dissolve alliance
            for (String id : new ArrayList<String>(alliance.islands)) {
                islandToAlliance.remove(id);
            }
            alliances.remove(alliance.id);
        } else {
            alliance.islands.remove(island.getId());
            islandToAlliance.remove(island.getId());
        }
        save();
        return true;
    }

    public void removeIsland(String islandId) {
        if (islandId == null || islandId.isEmpty()) return;

        boolean changed = pendingInvites.remove(islandId) != null;
        for (Set<String> invites : pendingInvites.values()) {
            changed = invites.removeIf(allianceId -> {
                Alliance alliance = alliances.get(allianceId);
                return alliance == null || alliance.islands.contains(islandId);
            }) || changed;
        }

        Alliance alliance = getByIsland(islandId);
        if (alliance != null) {
            if (alliance.leaderIsland.equals(islandId)) {
                for (String memberIslandId : new ArrayList<String>(alliance.islands)) {
                    islandToAlliance.remove(memberIslandId);
                }
                alliances.remove(alliance.id);
            } else {
                alliance.islands.remove(islandId);
                islandToAlliance.remove(islandId);
                if (alliance.islands.isEmpty()) {
                    alliances.remove(alliance.id);
                }
            }
            changed = true;
        } else {
            changed = islandToAlliance.remove(islandId) != null || changed;
        }

        if (changed) {
            pendingInvites.values().removeIf(Set::isEmpty);
            save();
        }
    }

    public Set<String> getPendingInvites(String islandId) {
        Set<String> s = pendingInvites.get(islandId);
        return s == null ? new HashSet<String>() : new HashSet<String>(s);
    }

    /** Toggle alliance chat for player. Returns new state. */
    public boolean toggleChat(UUID playerId) {
        if (chatToggled.contains(playerId)) {
            chatToggled.remove(playerId);
            return false;
        }
        chatToggled.add(playerId);
        return true;
    }

    public boolean isChatToggled(UUID playerId) {
        return chatToggled.contains(playerId);
    }

    public void disableChat(UUID playerId) {
        chatToggled.remove(playerId);
    }

    /** Send message to all members of the alliance the player belongs to. */
    public void sendAllianceMessage(Player from, String message) {
        Island island = islandProvider.getPlayerIsland(from.getUniqueId());
        if (island == null) {
            sendLang(from, "no-island");
            return;
        }
        Alliance alliance = getByIsland(island.getId());
        if (alliance == null) {
            sendLang(from, "alliance.chat.not-in");
            return;
        }
        String formatted = getLang("alliance.chat.format",
                "{alliance}", alliance.name,
                "{player}", from.getName(),
                "{message}", message);
        for (String islandId : alliance.islands) {
            Island ai = islandProvider.getIsland(islandId);
            if (ai == null) continue;
            for (UUID memberId : ai.getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null) member.sendMessage(formatted);
            }
        }
    }

    public List<Alliance> getAllAlliances() {
        return new ArrayList<Alliance>(alliances.values());
    }

    // --- Persistence ---

    public void load() {
        alliances.clear();
        islandToAlliance.clear();
        if (!dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection sec = cfg.getConfigurationSection("alliances");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection a = sec.getConfigurationSection(id);
            if (a == null) continue;
            String name = a.getString("name", id);
            String leader = a.getString("leader-island");
            long createdAt = a.getLong("created-at", System.currentTimeMillis());
            if (leader == null) continue;
            Alliance alliance = new Alliance(id, name, leader, createdAt);
            for (String islandId : a.getStringList("islands")) {
                alliance.islands.add(islandId);
                islandToAlliance.put(islandId, id);
            }
            alliances.put(id, alliance);
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Alliance alliance : alliances.values()) {
            String base = "alliances." + alliance.id + ".";
            cfg.set(base + "name", alliance.name);
            cfg.set(base + "leader-island", alliance.leaderIsland);
            cfg.set(base + "created-at", alliance.createdAt);
            cfg.set(base + "islands", new ArrayList<String>(alliance.islands));
        }
        me.reil.skybound.core.storage.YamlFiles.saveAtomically(plugin, cfg, dataFile, "alliances.yml");
    }

    private void sendLang(Player player, String key, String... replacements) {
        if (plugin instanceof SkyBoundPlugin) {
            ((SkyBoundPlugin) plugin).getLangManager().send(player, key, replacements);
        }
    }

    private String getLang(String key, String... replacements) {
        if (plugin instanceof SkyBoundPlugin) {
            return ((SkyBoundPlugin) plugin).getLangManager().get(key, replacements);
        }
        return ChatColor.translateAlternateColorCodes('&', key);
    }
}
