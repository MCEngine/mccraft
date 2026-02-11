package io.github.mcengine.mccraft.common.command.util;

import io.github.mcengine.mccraft.api.command.ICraftCommandHandle;
import io.github.mcengine.mccraft.common.MCCraftProvider;
import io.github.mcengine.mccraft.common.command.MCCraftCommandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/**
 * Handles /craft type create {text} {head_item_base64}
 * Registers a new station type with duplicate prevention.
 */
public class HandleType implements ICraftCommandHandle {

    @Override
    public void invoke(CommandSender sender, String[] args) {
        // args: ["create", "{type}", "{base64}"]
        if (args.length < 3 || !"create".equalsIgnoreCase(args[0])) {
            MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.type.usage")
                    .color(NamedTextColor.RED));
            return;
        }

        String type = args[1];
        String headItemBase64 = args[2];
        MCCraftProvider provider = MCCraftProvider.getInstance();

        provider.typeExists(type).thenAccept(exists -> {
            if (exists) {
                MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.type.exists")
                        .arguments(Component.text(type)).color(NamedTextColor.RED));
                return;
            }

            provider.insertType(type, headItemBase64).thenRun(() ->
                    MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.type.success")
                            .arguments(Component.text(type)).color(NamedTextColor.GREEN))
            ).exceptionally(ex -> {
                MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.error")
                        .arguments(Component.text(ex.getMessage())).color(NamedTextColor.RED));
                return null;
            });
        }).exceptionally(ex -> {
            MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.error")
                    .arguments(Component.text(ex.getMessage())).color(NamedTextColor.RED));
            return null;
        });
    }

    @Override
    public Component getHelp() {
        return Component.translatable("mcengine.mccraft.msg.type.help");
    }

    @Override
    public String getPermission() {
        return "mcengine.mccraft.type";
    }
}
