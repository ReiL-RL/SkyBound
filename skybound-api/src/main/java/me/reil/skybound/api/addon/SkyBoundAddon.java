package me.reil.skybound.api.addon;

/**
 * Interface for SkyBound addons.
 * Addons can be either:
 * 1. Standalone plugins that also work as SkyBound addons
 * 2. Pure addons that require SkyBound core
 *
 * When SkyBound core is present, addons register through this interface
 * to gain access to the API and be managed by the core.
 */
public interface SkyBoundAddon {

    /**
     * Unique addon identifier.
     */
    String getAddonId();

    /**
     * Display name.
     */
    String getAddonName();

    /**
     * Version string.
     */
    String getAddonVersion();

    /**
     * Called when the addon is enabled and SkyBound core is available.
     * Use this to register services, listeners, etc.
     */
    void onAddonEnable();

    /**
     * Called when the addon is being disabled.
     */
    void onAddonDisable();

    /**
     * Whether this addon can function without SkyBound core (standalone mode).
     */
    boolean supportsStandalone();
}
