package me.reil.skybound.core.addon;

import me.reil.skybound.api.addon.AddonRegistry;
import me.reil.skybound.api.addon.SkyBoundAddon;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Simple implementation of the addon registry.
 */
public final class SimpleAddonRegistry implements AddonRegistry {

    private static final Logger LOGGER = Logger.getLogger("SkyBound");
    private final Map<String, SkyBoundAddon> addons = new LinkedHashMap<String, SkyBoundAddon>();

    @Override
    public void register(SkyBoundAddon addon) {
        if (addon == null || addon.getAddonId() == null) return;
        addons.put(addon.getAddonId(), addon);
        LOGGER.info("[SkyBound] Addon registered: " + addon.getAddonName() + " v" + addon.getAddonVersion());
        addon.onAddonEnable();
    }

    @Override
    public void unregister(String addonId) {
        SkyBoundAddon addon = addons.remove(addonId);
        if (addon != null) {
            addon.onAddonDisable();
            LOGGER.info("[SkyBound] Addon unregistered: " + addon.getAddonName());
        }
    }

    @Override
    public SkyBoundAddon getAddon(String addonId) {
        return addons.get(addonId);
    }

    @Override
    public Collection<SkyBoundAddon> getAddons() {
        return Collections.unmodifiableCollection(addons.values());
    }

    @Override
    public boolean isRegistered(String addonId) {
        return addons.containsKey(addonId);
    }

    /**
     * Disable all addons (called on plugin shutdown).
     */
    public void disableAll() {
        for (SkyBoundAddon addon : addons.values()) {
            try {
                addon.onAddonDisable();
            } catch (Exception e) {
                LOGGER.warning("[SkyBound] Error disabling addon " + addon.getAddonName() + ": " + e.getMessage());
            }
        }
        addons.clear();
    }
}
