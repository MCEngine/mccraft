package io.github.mcengine.mccraft.common.command.util;

import io.github.mcengine.mccraft.api.command.ICraftCommandHandle;
import io.github.mcengine.mccraft.common.MCCraftProvider;
import io.github.mcengine.mccraft.common.command.MCCraftCommandManager;
import io.github.mcengine.mccraft.common.gui.CraftingGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles /craft create {type} {id}
 * <p>
 * Creates a new recipe for an existing station type and opens the GUI editor.
 * The type must already be registered via /craft type create.
 */
public class HandleCreate implements ICraftCommandHandle {

    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.player.only")
                    .color(NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.create.usage")
                    .color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) sender;
        String type = args[0];
        String id = args[1];
        String recipeId = type + "/" + id;
        MCCraftProvider provider = MCCraftProvider.getInstance();

        // Verify the type exists in the mccraft_type table
        provider.typeExists(type).thenAccept(exists -> {
            if (!exists) {
                MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.create.type.not.found")
                        .arguments(Component.text(type)).color(NamedTextColor.RED));
                return;
            }

            // Open the crafting editor GUI on the main thread
            player.getServer().getScheduler().runTask(
                    player.getServer().getPluginManager().getPlugin("MCCraft"),
                    () -> CraftingGUI.openEditor(player, type, recipeId)
            );
        }).exceptionally(ex -> {
            MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.error")
                    .arguments(Component.text(ex.getMessage())).color(NamedTextColor.RED));
            return null;
        });
    }

    @Override
    public Component getHelp() {
        return Component.translatable("mcengine.mccraft.msg.create.help");
    }

    @Override
    public String getPermission() {
        return "mcengine.mccraft.create";
    }
}
