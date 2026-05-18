package me.reil.skybound.api;

import me.reil.skybound.api.island.IslandProvider;
import me.reil.skybound.api.economy.EconomyProvider;
import me.reil.skybound.api.team.TeamProvider;
import me.reil.skybound.api.mission.MissionProvider;
import me.reil.skybound.api.upgrade.UpgradeProvider;
import me.reil.skybound.api.generator.GeneratorProvider;
import me.reil.skybound.api.shop.ShopProvider;
import me.reil.skybound.api.bank.BankProvider;
import me.reil.skybound.api.booster.BoosterProvider;
import me.reil.skybound.api.leaderboard.LeaderboardProvider;
import me.reil.skybound.api.season.SeasonProvider;
import me.reil.skybound.api.trade.TradeProvider;
import me.reil.skybound.api.visit.VisitProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Central service registry for SkyBound.
 * Core registers providers; addons retrieve them.
 */
public final class SkyBoundAPI {

    private static SkyBoundAPI instance;
    private final Map<Class<?>, Object> services = new HashMap<Class<?>, Object>();

    private SkyBoundAPI() {}

    public static SkyBoundAPI get() {
        if (instance == null) {
            throw new IllegalStateException("SkyBoundAPI has not been initialized. Is SkyBound-Core loaded?");
        }
        return instance;
    }

    public static void initialize() {
        if (instance != null) {
            throw new IllegalStateException("SkyBoundAPI is already initialized.");
        }
        instance = new SkyBoundAPI();
    }

    public static void shutdown() {
        instance = null;
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass) {
        Object service = services.get(serviceClass);
        if (service == null) {
            throw new IllegalStateException("Service not registered: " + serviceClass.getName());
        }
        return (T) service;
    }

    public <T> boolean hasService(Class<T> serviceClass) {
        return services.containsKey(serviceClass);
    }

    public <T> void register(Class<T> serviceClass, T provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Provider cannot be null for: " + serviceClass.getName());
        }
        services.put(serviceClass, provider);
    }

    // Convenience getters
    public IslandProvider getIslandProvider() { return getService(IslandProvider.class); }
    public EconomyProvider getEconomyProvider() { return getService(EconomyProvider.class); }
    public TeamProvider getTeamProvider() { return getService(TeamProvider.class); }
    public MissionProvider getMissionProvider() { return getService(MissionProvider.class); }
    public UpgradeProvider getUpgradeProvider() { return getService(UpgradeProvider.class); }
    public GeneratorProvider getGeneratorProvider() { return getService(GeneratorProvider.class); }
    public ShopProvider getShopProvider() { return getService(ShopProvider.class); }
    public BankProvider getBankProvider() { return getService(BankProvider.class); }
    public BoosterProvider getBoosterProvider() { return getService(BoosterProvider.class); }
    public LeaderboardProvider getLeaderboardProvider() { return getService(LeaderboardProvider.class); }
    public SeasonProvider getSeasonProvider() { return getService(SeasonProvider.class); }
    public TradeProvider getTradeProvider() { return getService(TradeProvider.class); }
    public VisitProvider getVisitProvider() { return getService(VisitProvider.class); }
}
