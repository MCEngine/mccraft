package io.github.mcengine.mccraft.common.command.util;

import io.github.mcengine.mccraft.api.command.ICraftCommandHandle;
import io.github.mcengine.mccraft.common.MCCraftProvider;
import io.github.mcengine.mccraft.common.command.MCCraftCommandManager;
import io.github.mcengine.mccraft.common.gui.EditorListGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles /craft editor {type}
 * Opens a GUI listing all registered recipes for the given type.
 */
public class HandleEditor implements ICraftCommandHandle {

    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.player.only")
                    .color(NamedTextColor.RED));
            return;
        }
        if (args.length < 1) {
            MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.editor.usage")
                    .color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) sender;
        String type = args[0];
        MCCraftProvider provider = MCCraftProvider.getInstance();

        provider.getItemsByType(type).thenAccept(items -> {
            player.getServer().getScheduler().runTask(
                    player.getServer().getPluginManager().getPlugin("MCCraft"),
                    () -> EditorListGUI.open(player, type, items)
            );
        }).exceptionally(ex -> {
            MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.error")
                    .arguments(Component.text(ex.getMessage())).color(NamedTextColor.RED));
            return null;
        });
    }

    @Override
    public Component getHelp() {
        return Component.translatable("mcengine.mccraft.msg.editor.help");
    }

    @Override
    public String getPermission() {
        return null;
    }
}
