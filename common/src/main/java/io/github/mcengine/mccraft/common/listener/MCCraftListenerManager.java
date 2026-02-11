package io.github.mcengine.mccraft.common.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages registration of all MCCraft event listeners.
 */
public class MCCraftListenerManager {

    private final Plugin plugin;
    private final List<Listener> listeners = new ArrayList<>();

    public MCCraftListenerManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register(Listener listener) {
        listeners.add(listener);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    public List<Listener> getListeners() {
        return listeners;
    }
}
