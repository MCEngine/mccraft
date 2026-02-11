package io.github.mcengine.mccraft.api.command;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

/**
 * Interface for MCCraft subcommand handlers.
 */
public interface ICraftCommandHandle {

    /**
     * Invokes the subcommand logic.
     *
     * @param sender the command sender
     * @param args   the remaining arguments after the subcommand name
     */
    void invoke(CommandSender sender, String[] args);

    /**
     * Returns a help description component for this subcommand.
     *
     * @return the help component
     */
    Component getHelp();

    /**
     * Returns the permission node required to use this subcommand, or null if none.
     *
     * @return the permission string, or null
     */
    String getPermission();
}
