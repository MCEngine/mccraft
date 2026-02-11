package io.github.mcengine.mccraft.papermc.engine;

import io.github.mcengine.mccraft.api.database.IMCCraftDB;
import io.github.mcengine.mccraft.common.MCCraftProvider;
import io.github.mcengine.mccraft.common.command.MCCraftCommandManager;
import io.github.mcengine.mccraft.common.command.MCCraftTabCompleter;
import io.github.mcengine.mccraft.common.command.util.HandleCreate;
import io.github.mcengine.mccraft.common.command.util.HandleEditor;
import io.github.mcengine.mccraft.common.command.util.HandleGet;
import io.github.mcengine.mccraft.common.command.util.HandleHelp;
import io.github.mcengine.mccraft.common.command.util.HandleType;
import io.github.mcengine.mccraft.common.database.MCCraftMySQL;
import io.github.mcengine.mccraft.common.database.MCCraftSQLite;
import io.github.mcengine.mccraft.common.listener.CraftingGUIListener;
import io.github.mcengine.mccraft.common.listener.EditorListGUIListener;
import io.github.mcengine.mccraft.common.listener.ItemDropProtectionListener;
import io.github.mcengine.mccraft.common.listener.MCCraftListenerManager;
import io.github.mcengine.mcextension.common.MCExtensionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.Executor;

/**
 * Main class for the MCCraft plugin.
 * <p>
 * This class handles the initialization of the MCExtensionManager and
 * sets up the appropriate task executor for the platform (Bukkit or Folia).
 */
public class MCCraftPlugin extends JavaPlugin {

    /**
     * The manager handling loading and lifecycle of MCExtensions.
     */
    private MCExtensionManager extensionManager;

    /**
     * The executor used for running extension-related tasks.
     */
    private Executor executor;

    /**
     * The central provider for MCCraft systems.
     */
    private MCCraftProvider provider;

    /**
     * Called when the plugin is enabled.
     * Initializes configuration, core components, services, and registers handlers.
     */
    @Override
    public void onEnable() {
        // 1. Initialize Configuration
        saveDefaultConfig();

        this.executor = setupExecutor();

        // 2. Initialize Database
        IMCCraftDB db = setupDatabase();

        // 3. Initialize Command & Listener Managers
        MCCraftCommandManager commandManager = new MCCraftCommandManager();
        MCCraftListenerManager listenerManager = new MCCraftListenerManager(this);

        // 4. Create Provider (singleton)
        this.provider = new MCCraftProvider(db, executor, commandManager, listenerManager);

        // 5. Register Commands
        commandManager.register("help", new HandleHelp(commandManager));
        commandManager.register("type", new HandleType());
        commandManager.register("create", new HandleCreate());
        commandManager.register("get", new HandleGet());
        commandManager.register("editor", new HandleEditor());

        PluginCommand craftCommand = getCommand("craft");
        if (craftCommand != null) {
            craftCommand.setExecutor(commandManager);
            craftCommand.setTabCompleter(new MCCraftTabCompleter());
        }

        // 6. Register Listeners
        listenerManager.register(new CraftingGUIListener());
        listenerManager.register(new EditorListGUIListener());
        listenerManager.register(new ItemDropProtectionListener());

        // 7. Initialize Extension Manager
        this.extensionManager = new MCExtensionManager();
        Bukkit.getServicesManager().register(MCExtensionManager.class, extensionManager, this, ServicePriority.Normal);
        extensionManager.loadAllExtensions(this, this.executor);

        getLogger().info("MCCraft Engine has been enabled!");
    }

    /**
     * Sets up the database based on the config.yml db.type setting.
     *
     * @return the initialized database implementation
     */
    private IMCCraftDB setupDatabase() {
        String dbType = getConfig().getString("db.type", "sqlite");
        if ("mysql".equalsIgnoreCase(dbType)) {
            getLogger().info("Using MySQL database backend.");
            return new MCCraftMySQL(this);
        } else {
            getLogger().info("Using SQLite database backend.");
            return new MCCraftSQLite(this);
        }
    }

    /**
     * Helper to determine the correct Executor for the platform.
     *
     * @return An Executor that runs tasks asynchronously, supporting both Folia and standard Bukkit schedulers.
     */
    private Executor setupExecutor() {
        try {
            // Check if Folia's AsyncScheduler is available (Folia/Paper 1.20+)
            Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            return task -> Bukkit.getAsyncScheduler().runNow(this, scheduledTask -> task.run());
        } catch (ClassNotFoundException e) {
            // Fallback to standard Bukkit Async Scheduler (Spigot/Legacy Paper)
            return task -> Bukkit.getScheduler().runTaskAsynchronously(this, task);
        }
    }

    /**
     * Called when the plugin is disabled.
     * Ensures extensions are unloaded and resources are cleaned up properly.
     */
    @Override
    public void onDisable() {
        // Shutdown extensions first
        if (extensionManager != null) {
            extensionManager.disableAllExtensions(this, this.executor);
        }

        // Shutdown provider (closes DB)
        if (provider != null) {
            provider.shutdown();
        }

        getLogger().info("MCCraft Engine has been disabled!");
    }
}
