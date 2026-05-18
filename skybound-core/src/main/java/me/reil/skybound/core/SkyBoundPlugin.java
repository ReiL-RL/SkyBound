package me.reil.skybound.core;

import me.reil.skybound.api.SkyBoundAPI;
import me.reil.skybound.api.addon.AddonRegistry;
import me.reil.skybound.api.bank.BankProvider;
import me.reil.skybound.api.booster.BoosterProvider;
import me.reil.skybound.api.economy.EconomyProvider;
import me.reil.skybound.api.generator.GeneratorProvider;
import me.reil.skybound.api.island.IslandProvider;
import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.leaderboard.LeaderboardProvider;
import me.reil.skybound.api.mission.MissionProvider;
import me.reil.skybound.api.shop.ShopProvider;
import me.reil.skybound.api.team.TeamProvider;
import me.reil.skybound.api.upgrade.UpgradeProvider;
import me.reil.skybound.api.season.SeasonProvider;
import me.reil.skybound.api.trade.TradeProvider;
import me.reil.skybound.api.visit.VisitProvider;
import me.reil.skybound.core.addon.SimpleAddonRegistry;
import me.reil.skybound.core.bank.BankManager;
import me.reil.skybound.core.booster.BoosterManager;
import me.reil.skybound.core.command.IslandCommand;
import me.reil.skybound.core.command.SkyAdminCommand;
import me.reil.skybound.core.config.CoreConfig;
import me.reil.skybound.core.economy.VaultEconomyProvider;
import me.reil.skybound.core.generator.GeneratorManager;
import me.reil.skybound.core.generator.OfflineGeneratorManager;
import me.reil.skybound.core.integration.SopLibIntegration;
import me.reil.skybound.core.island.BiomeService;
import me.reil.skybound.core.island.ConfirmationManager;
import me.reil.skybound.core.island.IslandChestManager;
import me.reil.skybound.core.island.IslandLogManager;
import me.reil.skybound.core.island.IslandManager;
import me.reil.skybound.core.island.IslandPermissionManager;
import me.reil.skybound.core.island.PrestigeManager;
import me.reil.skybound.core.lang.LangManager;
import me.reil.skybound.core.leaderboard.LeaderboardManager;
import me.reil.skybound.core.listener.AutosellListener;
import me.reil.skybound.core.listener.BorderVisualListener;
import me.reil.skybound.core.listener.EntityLimitListener;
import me.reil.skybound.core.listener.GeneratorListener;
import me.reil.skybound.core.listener.IslandFlyListener;
import me.reil.skybound.core.listener.IslandProtectionListener;
import me.reil.skybound.core.listener.MissionTrackingListener;
import me.reil.skybound.core.listener.PlayerJoinListener;
import me.reil.skybound.core.listener.PortalListener;
import me.reil.skybound.core.listener.SpawnerStackListener;
import me.reil.skybound.core.listener.VoidFallListener;
import me.reil.skybound.core.menu.MenuListener;
import me.reil.skybound.core.schematic.SchematicService;
import me.reil.skybound.core.mission.MissionManager;
import me.reil.skybound.core.recipe.RecipeManager;
import me.reil.skybound.core.season.SeasonConfig;
import me.reil.skybound.core.season.SeasonManager;
import me.reil.skybound.core.shop.ShopManager;
import me.reil.skybound.core.storage.StorageManager;
import me.reil.skybound.core.team.TeamManager;
import me.reil.skybound.core.trade.TradeManager;
import me.reil.skybound.core.upgrade.UpgradeManager;
import me.reil.skybound.core.visit.VisitManager;
import me.reil.skybound.core.world.VoidWorldGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * SkyBound Core Plugin.
 * Provides the island system, economy, teams, upgrades, generators,
 * missions, shop, bank, boosters, and leaderboard.
 *
 * Addons register through the SkyBoundAPI service registry.
 */
public final class SkyBoundPlugin extends JavaPlugin {

    private CoreConfig coreConfig;
    private SopLibIntegration sopLib;
    private StorageManager storageManager;
    private IslandManager islandManager;
    private VaultEconomyProvider economyProvider;
    private TeamManager teamManager;
    private MissionManager missionManager;
    private UpgradeManager upgradeManager;
    private GeneratorManager generatorManager;
    private ShopManager shopManager;
    private BankManager bankManager;
    private BoosterManager boosterManager;
    private LeaderboardManager leaderboardManager;
    private SimpleAddonRegistry addonRegistry;
    private SchematicService schematicService;
    private LangManager langManager;
    private BorderVisualListener borderVisualListener;
    private IslandLogManager islandLogManager;
    private IslandChestManager islandChestManager;
    private ConfirmationManager confirmationManager;
    private BiomeService biomeService;
    private AutosellListener autosellListener;
    private PrestigeManager prestigeManager;
    private OfflineGeneratorManager offlineGeneratorManager;
    private IslandPermissionManager islandPermissionManager;
    private SeasonConfig seasonConfig;
    private SeasonManager seasonManager;
    private TradeManager tradeManager;
    private VisitManager visitManager;
    private RecipeManager recipeManager;
    private BukkitTask autosaveTask;
    private BukkitTask boosterTickTask;

    @Override
    public void onEnable() {
        // Initialize API
        SkyBoundAPI.initialize();

        // Config
        this.coreConfig = new CoreConfig(this);
        this.coreConfig.load();

        // SopLib integration (multi-version support)
        this.sopLib = new SopLibIntegration(this);

        // Storage
        this.storageManager = new StorageManager(this, coreConfig);
        this.storageManager.initialize();

        // Ensure island world exists
        ensureIslandWorld();

        // Core managers
        this.islandManager = new IslandManager(this, coreConfig, storageManager);
        this.economyProvider = new VaultEconomyProvider(this);
        this.teamManager = new TeamManager(this, islandManager);
        this.bankManager = new BankManager(this, coreConfig, islandManager, economyProvider);
        this.upgradeManager = new UpgradeManager(this, coreConfig, islandManager, bankManager);
        this.generatorManager = new GeneratorManager(this, coreConfig, islandManager, upgradeManager);
        this.boosterManager = new BoosterManager(this, coreConfig, islandManager, bankManager);
        this.missionManager = new MissionManager(this, coreConfig, islandManager, economyProvider);
        this.shopManager = new ShopManager(this, coreConfig, economyProvider);
        this.leaderboardManager = new LeaderboardManager(this, islandManager);
        this.addonRegistry = new SimpleAddonRegistry();
        this.schematicService = new SchematicService(this);
        this.langManager = new LangManager(this, coreConfig.getLanguage());
        this.islandLogManager = new IslandLogManager();
        this.islandChestManager = new IslandChestManager();
        this.confirmationManager = new ConfirmationManager();
        this.biomeService = new BiomeService();
        this.prestigeManager = new PrestigeManager(this, coreConfig, islandManager);
        this.offlineGeneratorManager = new OfflineGeneratorManager(this, islandManager, generatorManager);
        this.islandPermissionManager = new IslandPermissionManager();

        // Season system
        this.seasonConfig = new SeasonConfig(this);
        this.seasonConfig.load();
        this.seasonManager = new SeasonManager(this, seasonConfig, islandManager, leaderboardManager);

        // Trade system
        this.tradeManager = new TradeManager(this, islandManager, economyProvider);

        // Visit/rating system
        this.visitManager = new VisitManager(this);

        // Custom recipes
        this.recipeManager = new RecipeManager(this);

        // Register all services in API
        SkyBoundAPI api = SkyBoundAPI.get();
        api.register(IslandProvider.class, islandManager);
        api.register(EconomyProvider.class, economyProvider);
        api.register(TeamProvider.class, teamManager);
        api.register(MissionProvider.class, missionManager);
        api.register(UpgradeProvider.class, upgradeManager);
        api.register(GeneratorProvider.class, generatorManager);
        api.register(ShopProvider.class, shopManager);
        api.register(BankProvider.class, bankManager);
        api.register(BoosterProvider.class, boosterManager);
        api.register(LeaderboardProvider.class, leaderboardManager);
        api.register(AddonRegistry.class, addonRegistry);
        api.register(SeasonProvider.class, seasonManager);
        api.register(TradeProvider.class, tradeManager);
        api.register(VisitProvider.class, visitManager);

        // Register commands
        registerCommands();

        // Register listeners
        registerListeners();

        // Start tasks
        startAutosave();
        startBoosterTick();
        startValueRecalculation();

        // Wire persistence save callback
        storageManager.setSaveCallback(new Runnable() {
            @Override
            public void run() {
                islandManager.saveData();
                storageManager.saveMissionProgress(missionManager.getAllProgress());
                storageManager.saveUpgrades(upgradeManager.getAllUpgradeLevels());
                storageManager.saveBoosters(boosterManager.getAllActiveBoosters());
            }
        });

        // Load mission progress from disk
        missionManager.setAllProgress(storageManager.loadMissionProgress());
        upgradeManager.setAllUpgradeLevels(storageManager.loadUpgrades());
        boosterManager.setAllActiveBoosters(storageManager.loadBoosters());

        getLogger().info("SkyBound Core v" + getDescription().getVersion() + " enabled.");
        getLogger().info("API ready. Addons can now register.");
    }

    @Override
    public void onDisable() {
        // Stop tasks
        if (autosaveTask != null) autosaveTask.cancel();
        if (boosterTickTask != null) boosterTickTask.cancel();
        if (borderVisualListener != null) borderVisualListener.stop();

        // Shutdown new managers
        if (seasonManager != null) seasonManager.shutdown();
        if (visitManager != null) visitManager.shutdown();
        if (recipeManager != null) recipeManager.shutdown();
        if (tradeManager != null) tradeManager.saveOffers();

        // Disable addons
        if (addonRegistry != null) {
            addonRegistry.disableAll();
        }

        // Save data
        if (storageManager != null) {
            storageManager.saveAll();
            storageManager.close();
        }

        // Shutdown API
        SkyBoundAPI.shutdown();

        getLogger().info("SkyBound Core disabled.");
    }

    private void ensureIslandWorld() {
        String worldName = coreConfig.getIslandWorldName();
        World world = Bukkit.getWorld(worldName);
        if (world != null) return;

        WorldCreator creator = new WorldCreator(worldName);
        creator.generator(new VoidWorldGenerator());
        creator.generateStructures(false);
        Bukkit.createWorld(creator);
        getLogger().info("Created island world: " + worldName);

        // Nether world (if dimensional islands enabled)
        if (coreConfig.isNetherEnabled()) {
            String netherName = worldName + "_nether";
            if (Bukkit.getWorld(netherName) == null) {
                WorldCreator netherCreator = new WorldCreator(netherName);
                netherCreator.environment(World.Environment.NETHER);
                netherCreator.generator(new VoidWorldGenerator());
                netherCreator.generateStructures(false);
                Bukkit.createWorld(netherCreator);
                getLogger().info("Created nether world: " + netherName);
            }
        }

        // End world (if dimensional islands enabled)
        if (coreConfig.isEndEnabled()) {
            String endName = worldName + "_the_end";
            if (Bukkit.getWorld(endName) == null) {
                WorldCreator endCreator = new WorldCreator(endName);
                endCreator.environment(World.Environment.THE_END);
                endCreator.generator(new VoidWorldGenerator());
                endCreator.generateStructures(false);
                Bukkit.createWorld(endCreator);
                getLogger().info("Created end world: " + endName);
            }
        }
    }

    private void registerCommands() {
        PluginCommand isCmd = getCommand("is");
        if (isCmd != null) {
            IslandCommand executor = new IslandCommand(this);
            isCmd.setExecutor(executor);
            isCmd.setTabCompleter(executor);
        }

        PluginCommand adminCmd = getCommand("sbadmin");
        if (adminCmd != null) {
            SkyAdminCommand executor = new SkyAdminCommand(this);
            adminCmd.setExecutor(executor);
            adminCmd.setTabCompleter(executor);
        }
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new IslandProtectionListener(islandManager, teamManager), this);
        Bukkit.getPluginManager().registerEvents(new GeneratorListener(islandManager, generatorManager, boosterManager), this);
        Bukkit.getPluginManager().registerEvents(new MissionTrackingListener(islandManager, missionManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this, islandManager), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(), this);
        Bukkit.getPluginManager().registerEvents(new VoidFallListener(islandManager, coreConfig), this);
        Bukkit.getPluginManager().registerEvents(new PortalListener(islandManager, coreConfig), this);
        Bukkit.getPluginManager().registerEvents(new IslandFlyListener(islandManager, boosterManager, coreConfig), this);
        Bukkit.getPluginManager().registerEvents(new EntityLimitListener(islandManager, upgradeManager), this);

        this.autosellListener = new AutosellListener(islandManager, shopManager, economyProvider);
        Bukkit.getPluginManager().registerEvents(autosellListener, this);
        Bukkit.getPluginManager().registerEvents(new SpawnerStackListener(this, islandManager), this);

        this.borderVisualListener = new BorderVisualListener(this, islandManager, coreConfig);
        this.borderVisualListener.start();

        me.reil.skybound.core.integration.PlaceholderExpansion papi = new me.reil.skybound.core.integration.PlaceholderExpansion(this);
        papi.register();

        // VoidRift integration
        me.reil.skybound.core.integration.VoidRiftHook voidRiftHook = new me.reil.skybound.core.integration.VoidRiftHook(this);
        voidRiftHook.register();

        // FlexAchievements integration
        new me.reil.skybound.core.integration.FlexAchievementsHook(this);
    }

    private void startAutosave() {
        long ticks = Math.max(20L, coreConfig.getAutosaveSeconds() * 20L);
        this.autosaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                storageManager.saveAll();
            }
        }, ticks, ticks);
    }

    private void startBoosterTick() {
        this.boosterTickTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                boosterManager.tick();
            }
        }, 20L, 20L);
    }

    private void startValueRecalculation() {
        // Recalculate island values every 5 minutes
        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                for (Island island : islandManager.getAllIslands()) {
                    islandManager.recalculateValue((me.reil.skybound.core.island.IslandImpl) island);
                }
                leaderboardManager.recalculate();
            }
        }, 6000L, 6000L);
    }

    // Getters for internal use by commands/listeners
    public CoreConfig getCoreConfig() { return coreConfig; }
    public StorageManager getStorageManager() { return storageManager; }
    public IslandManager getIslandManager() { return islandManager; }
    public VaultEconomyProvider getEconomyProvider() { return economyProvider; }
    public TeamManager getTeamManager() { return teamManager; }
    public MissionManager getMissionManager() { return missionManager; }
    public UpgradeManager getUpgradeManager() { return upgradeManager; }
    public GeneratorManager getGeneratorManager() { return generatorManager; }
    public ShopManager getShopManager() { return shopManager; }
    public BankManager getBankManager() { return bankManager; }
    public BoosterManager getBoosterManager() { return boosterManager; }
    public LeaderboardManager getLeaderboardManager() { return leaderboardManager; }
    public SimpleAddonRegistry getAddonRegistry() { return addonRegistry; }
    public SchematicService getSchematicService() { return schematicService; }
    public SopLibIntegration getSopLib() { return sopLib; }
    public LangManager getLangManager() { return langManager; }
    public IslandLogManager getIslandLogManager() { return islandLogManager; }
    public IslandChestManager getIslandChestManager() { return islandChestManager; }
    public ConfirmationManager getConfirmationManager() { return confirmationManager; }
    public BiomeService getBiomeService() { return biomeService; }
    public AutosellListener getAutosellListener() { return autosellListener; }
    public PrestigeManager getPrestigeManager() { return prestigeManager; }
    public OfflineGeneratorManager getOfflineGeneratorManager() { return offlineGeneratorManager; }
    public IslandPermissionManager getIslandPermissionManager() { return islandPermissionManager; }
    public SeasonManager getSeasonManager() { return seasonManager; }
    public TradeManager getTradeManager() { return tradeManager; }
    public VisitManager getVisitManager() { return visitManager; }
    public RecipeManager getRecipeManager() { return recipeManager; }
}
