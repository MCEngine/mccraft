package io.github.mcengine.mccraft.common.listener;

import io.github.mcengine.mccraft.common.MCCraftProvider;
import io.github.mcengine.mccraft.common.gui.CraftingGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles right-click with a head item to open the crafting GUI,
 * and prevents placing head items as blocks.
 */
public class HeadItemInteractListener implements Listener {

    private static final NamespacedKey MCCRAFT_TYPE_KEY = new NamespacedKey("mccraft", "mccraft_type");

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        String type = getMCCraftType(item);
        if (type == null) return;

        // Cancel the interaction (prevent placing the head)
        event.setCancelled(true);

        MCCraftProvider provider = MCCraftProvider.getInstance();
        if (provider == null) return;

        // Load recipes for this type and open the first one as a crafting view
        provider.getItemsByType(type).thenAccept(items -> {
            if (items == null || items.isEmpty()) {
                player.sendMessage(Component.translatable("mcengine.mccraft.msg.craft.no.recipes")
                        .arguments(Component.text(type)).color(NamedTextColor.RED));
                return;
            }

            // Open the first recipe as a crafting view on the main thread
            var firstRow = items.get(0);
            String recipeId = firstRow.get("id");
            player.getServer().getScheduler().runTask(
                    player.getServer().getPluginManager().getPlugin("MCCraft"),
                    () -> CraftingGUI.openCraftingView(player, type, recipeId, firstRow)
            );
        }).exceptionally(ex -> {
            player.sendMessage(Component.translatable("mcengine.mccraft.msg.error")
                    .arguments(Component.text(ex.getMessage())).color(NamedTextColor.RED));
            return null;
        });
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (getMCCraftType(item) != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.translatable("mcengine.mccraft.msg.place.protected")
                    .color(NamedTextColor.RED));
        }
    }

    private String getMCCraftType(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(MCCRAFT_TYPE_KEY, PersistentDataType.STRING)) {
            return pdc.get(MCCRAFT_TYPE_KEY, PersistentDataType.STRING);
        }
        return null;
    }
}
