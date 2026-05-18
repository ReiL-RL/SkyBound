package me.reil.skybound.core.island;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages confirmation for dangerous actions (delete, regen).
 * Players must confirm within 15 seconds.
 */
public final class ConfirmationManager {

    private static final long EXPIRY_MS = 15000L;

    private final Map<UUID, PendingAction> pending = new LinkedHashMap<UUID, PendingAction>();

    /**
     * Request confirmation for an action.
     * @return true if this is a new request (player needs to confirm)
     */
    public boolean requestConfirmation(UUID playerId, String action) {
        PendingAction existing = pending.get(playerId);
        if (existing != null && existing.action.equals(action) && !existing.isExpired()) {
            // Already confirmed
            pending.remove(playerId);
            return false; // false = confirmed, proceed
        }
        // New request
        pending.put(playerId, new PendingAction(action, System.currentTimeMillis()));
        return true; // true = needs confirmation
    }

    /**
     * Clean up expired confirmations.
     */
    public void cleanup() {
        Iterator<Map.Entry<UUID, PendingAction>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isExpired()) {
                it.remove();
            }
        }
    }

    private static final class PendingAction {
        final String action;
        final long timestamp;

        PendingAction(String action, long timestamp) {
            this.action = action;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > EXPIRY_MS;
        }
    }
}
