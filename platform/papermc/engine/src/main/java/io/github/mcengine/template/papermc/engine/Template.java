package io.github.mcengine.template.papermc.engine;

import io.github.mcengine.mcextension.common.MCExtensionManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.Executor;

/**
 * Main class for the Template plugin.
 * <p>
 * This class handles the initialization of the MCExtensionManager and 
 * sets up the appropriate task executor for the platform (Bukkit or Folia).
 */
public class Template extends JavaPlugin {

    /**
     * The manager handling loading and lifecycle of MCExtensions.
     */
    private MCExtensionManager extensionManager;

    /**
     * The executor used for running extension-related tasks.
     */
    private Executor executor;

    /**
     * Called when the plugin is enabled.
     * Initializes configuration, core components, services, and registers handlers.
     */
    @Override
    public void onEnable() {
        // 1. Initialize Configuration
        saveDefaultConfig();

        this.executor = setupExecutor();

        // Initialize Extension Manager
        this.extensionManager = new MCExtensionManager();

        Bukkit.getServicesManager().register(MCExtensionManager.class, extensionManager, this, ServicePriority.Normal);

        // 5. Load Extensions
        extensionManager.loadAllExtensions(this, this.executor);

        getLogger().info("Template Engine has been enabled!");
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

        getLogger().info("Template Engine has been disabled!");
    }
}
