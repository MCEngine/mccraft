package io.github.mcengine.mccraft.common.listener;

import io.github.mcengine.mccraft.common.cache.RecipeCache;
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

        // Check cache for recipes of this type
        RecipeCache cache = RecipeCache.getInstance();
        if (cache.getRecipes(type).isEmpty()) {
            player.sendMessage(Component.translatable("mcengine.mccraft.msg.craft.no.recipes")
                    .arguments(Component.text(type)).color(NamedTextColor.RED));
            return;
        }

        // Open an empty crafting view — player places ingredients, result appears dynamically
        CraftingGUI.openCraftingView(player, type);
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
