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
 * Handles /craft create {type} {head_item_base64 | id}
 * <p>
 * Two modes:
 * <ul>
 *   <li>If the type does not yet exist in the DB, registers a new station type with the given head item (Base64).</li>
 *   <li>If the type already exists, the third arg is treated as a recipe id and opens the in-game editor.</li>
 * </ul>
 */
public class HandleCreate implements ICraftCommandHandle {

    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.create.usage")
                    .color(NamedTextColor.RED));
            return;
        }

        String type = args[0];
        String secondArg = args[1];
        MCCraftProvider provider = MCCraftProvider.getInstance();

        // Determine mode: if the type already has a registered head item, treat as recipe creation
        provider.getItemsByType(type).thenAccept(items -> {
            boolean typeExists = items.stream().anyMatch(row -> "__head__".equals(row.get("id")));

            if (!typeExists) {
                // Mode 1: Register new station type — secondArg is head_item_base64
                String headId = type + "/__head__";
                provider.saveItem(headId, type, secondArg).thenRun(() ->
                        MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.create.type.success")
                                .arguments(Component.text(type)).color(NamedTextColor.GREEN))
                ).exceptionally(ex -> {
                    MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.error")
                            .arguments(Component.text(ex.getMessage())).color(NamedTextColor.RED));
                    return null;
                });
            } else {
                // Mode 2: Create/edit a recipe — secondArg is recipe id
                if (!(sender instanceof Player)) {
                    MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.player.only")
                            .color(NamedTextColor.RED));
                    return;
                }
                Player player = (Player) sender;
                String recipeId = type + "/" + secondArg;
                // Open the crafting editor GUI on the main thread
                player.getServer().getScheduler().runTask(
                        player.getServer().getPluginManager().getPlugins()[0],
                        () -> CraftingGUI.openEditor(player, type, recipeId)
                );
            }
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
        // Single gate handled by MCCraftCommandManager; covers both type registration and recipe editing paths
        return "mcengine.mccraft.create";
    }
}
