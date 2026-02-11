package io.github.mcengine.mccraft.common.command;

import io.github.mcengine.mccraft.api.command.ICraftCommandHandle;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Central command executor for the /craft command.
 * Dispatches to registered {@link ICraftCommandHandle} subcommands.
 */
public class MCCraftCommandManager implements CommandExecutor {

    private final Map<String, ICraftCommandHandle> subcommands = new HashMap<>();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    public void register(String name, ICraftCommandHandle handler) {
        if (subcommands.containsKey(name.toLowerCase())) return;
        subcommands.put(name.toLowerCase(), handler);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String subName = (args.length == 0) ? "help" : args[0].toLowerCase();
        ICraftCommandHandle handle = subcommands.get(subName);

        if (handle != null) {
            String permission = handle.getPermission();
            if (permission != null && !sender.hasPermission(permission)) {
                send(sender, Component.translatable("mcengine.mccraft.msg.permission.denied").color(NamedTextColor.RED));
                return true;
            }
            String[] subArgs = (args.length <= 1) ? new String[0] : Arrays.copyOfRange(args, 1, args.length);
            handle.invoke(sender, subArgs);
        } else {
            send(sender, Component.translatable("mcengine.mccraft.msg.command.unknown")
                    .arguments(Component.text(subName)).color(NamedTextColor.RED));
        }
        return true;
    }

    public Map<String, ICraftCommandHandle> getSubcommands() {
        return subcommands;
    }

    public static void send(CommandSender sender, Component message) {
        if (sender instanceof Audience) {
            ((Audience) sender).sendMessage(message);
        } else {
            sender.sendMessage(LEGACY_SERIALIZER.serialize(message));
        }
    }
}
