package me.reil.skybound.core.island;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks island activity logs.
 * Keeps last 100 entries per island.
 */
public final class IslandLogManager {

    private static final int MAX_ENTRIES = 100;
    private final Map<String, List<IslandLogEntry>> logs = new LinkedHashMap<String, List<IslandLogEntry>>();

    public void log(String islandId, UUID playerId, String playerName, IslandLogEntry.LogAction action, String details) {
        List<IslandLogEntry> list = logs.get(islandId);
        if (list == null) {
            list = new ArrayList<IslandLogEntry>();
            logs.put(islandId, list);
        }
        list.add(new IslandLogEntry(System.currentTimeMillis(), playerId, playerName, action, details));
        if (list.size() > MAX_ENTRIES) {
            list.remove(0);
        }
    }

    public List<IslandLogEntry> getLogs(String islandId) {
        List<IslandLogEntry> list = logs.get(islandId);
        return list == null ? Collections.<IslandLogEntry>emptyList() : Collections.unmodifiableList(list);
    }

    public List<IslandLogEntry> getLogs(String islandId, int limit) {
        List<IslandLogEntry> list = logs.get(islandId);
        if (list == null) return Collections.emptyList();
        int start = Math.max(0, list.size() - limit);
        return Collections.unmodifiableList(list.subList(start, list.size()));
    }
}
