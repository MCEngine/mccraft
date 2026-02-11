package io.github.mcengine.mccraft.common.command.util;

import io.github.mcengine.mccraft.api.command.ICraftCommandHandle;
import io.github.mcengine.mccraft.common.command.MCCraftCommandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

import java.util.Map;

/**
 * Displays help information for all registered /craft subcommands.
 */
public class HandleHelp implements ICraftCommandHandle {

    private final MCCraftCommandManager manager;

    public HandleHelp(MCCraftCommandManager manager) {
        this.manager = manager;
    }

    @Override
    public void invoke(CommandSender sender, String[] args) {
        MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.help.header")
                .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        for (Map.Entry<String, ICraftCommandHandle> entry : manager.getSubcommands().entrySet()) {
            String name = entry.getKey();
            ICraftCommandHandle handle = entry.getValue();
            if (name.equalsIgnoreCase("help")) continue;
            if (handle.getPermission() == null || sender.hasPermission(handle.getPermission())) {
                String fullCommand = "/craft " + name;
                MCCraftCommandManager.send(sender, Component.text()
                        .append(Component.text(fullCommand + " ", NamedTextColor.YELLOW)
                                .clickEvent(ClickEvent.suggestCommand(fullCommand + " "))
                                .hoverEvent(HoverEvent.showText(Component.translatable("mcengine.mccraft.msg.help.hover")
                                        .color(NamedTextColor.GREEN))))
                        .append(handle.getHelp().color(NamedTextColor.GRAY))
                        .build());
            }
        }
    }

    @Override
    public Component getHelp() {
        return Component.translatable("mcengine.mccraft.msg.help.help");
    }

    @Override
    public String getPermission() {
        return null;
    }
}
