package me.reil.skybound.api.addon;

import java.util.Collection;

/**
 * Registry for managing SkyBound addons.
 * Core maintains this registry; addons register themselves on enable.
 */
public interface AddonRegistry {

    /**
     * Register an addon.
     */
    void register(SkyBoundAddon addon);

    /**
     * Unregister an addon.
     */
    void unregister(String addonId);

    /**
     * Get a registered addon by id.
     */
    SkyBoundAddon getAddon(String addonId);

    /**
     * Get all registered addons.
     */
    Collection<SkyBoundAddon> getAddons();

    /**
     * Check if an addon is registered.
     */
    boolean isRegistered(String addonId);
}
