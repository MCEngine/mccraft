package io.github.mcengine.mccraft.common.command.util;

import io.github.mcengine.mccraft.api.command.ICraftCommandHandle;
import io.github.mcengine.mccraft.common.MCCraftProvider;
import io.github.mcengine.mccraft.common.command.MCCraftCommandManager;
import io.github.mcengine.mccraft.common.util.ItemSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

/**
 * Handles /craft get {type}
 * Gives the player the Head Item for the specified station type.
 */
public class HandleGet implements ICraftCommandHandle {

    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.player.only")
                    .color(NamedTextColor.RED));
            return;
        }
        if (args.length < 1) {
            MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.get.usage")
                    .color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) sender;
        String type = args[0];
        MCCraftProvider provider = MCCraftProvider.getInstance();
        String headId = type + "/__head__";

        provider.getItem(headId).thenAccept(row -> {
            if (row == null) {
                MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.get.not.found")
                        .arguments(Component.text(type)).color(NamedTextColor.RED));
                return;
            }

            String base64 = row.get("contents");
            ItemStack headItem = ItemSerializer.fromBase64(base64);
            if (headItem == null) {
                MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.error")
                        .arguments(Component.text("Failed to deserialize head item")).color(NamedTextColor.RED));
                return;
            }

            // Stamp the PersistentDataContainer with mccraft_type
            ItemMeta meta = headItem.getItemMeta();
            if (meta != null) {
                NamespacedKey key = new NamespacedKey("mccraft", "mccraft_type");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, type);
                headItem.setItemMeta(meta);
            }

            // Give item on main thread
            player.getServer().getScheduler().runTask(
                    player.getServer().getPluginManager().getPlugin("MCCraft"),
                    () -> {
                        Map<Integer, ItemStack> overflow = player.getInventory().addItem(headItem);
                        if (!overflow.isEmpty()) {
                            overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                        }
                        MCCraftCommandManager.send(player, Component.translatable("mcengine.mccraft.msg.get.success")
                                .arguments(Component.text(type)).color(NamedTextColor.GREEN));
                    }
            );
        }).exceptionally(ex -> {
            MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.error")
                    .arguments(Component.text(ex.getMessage())).color(NamedTextColor.RED));
            return null;
        });
    }

    @Override
    public Component getHelp() {
        return Component.translatable("mcengine.mccraft.msg.get.help");
    }

    @Override
    public String getPermission() {
        return null;
    }
}
