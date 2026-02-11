package io.github.mcengine.mccraft.common.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Prevents players from dropping items that contain the mccraft_type
 * PersistentDataContainer key. This protects Head Items from being
 * dropped via 'Q' key, dragging out of inventory, etc.
 */
public class ItemDropProtectionListener implements Listener {

    private static final NamespacedKey MCCRAFT_TYPE_KEY = new NamespacedKey("mccraft", "mccraft_type");

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(MCCRAFT_TYPE_KEY, PersistentDataType.STRING)) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.sendMessage(Component.translatable("mcengine.mccraft.msg.drop.protected")
                    .color(NamedTextColor.RED));
        }
    }
}
