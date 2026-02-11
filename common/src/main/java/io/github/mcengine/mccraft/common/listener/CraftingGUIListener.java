package io.github.mcengine.mccraft.common.listener;

import io.github.mcengine.mccraft.common.MCCraftProvider;
import io.github.mcengine.mccraft.common.gui.CraftingGUI;
import io.github.mcengine.mccraft.common.util.GUIConstants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles interactions within the MCCraft crafting GUI.
 * <p>
 * For editor GUIs (title contains "["), closing saves the recipe.
 * For crafting view GUIs, clicking the result slot validates the head item
 * and decrements it.
 */
public class CraftingGUIListener implements Listener {

    private static final NamespacedKey MCCRAFT_TYPE_KEY = new NamespacedKey("mccraft", "mccraft_type");

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Component title = event.getView().title();
        if (title == null) return;
        String titleText = PlainTextComponentSerializer.plainText().serialize(title);
        if (!titleText.startsWith(GUIConstants.CRAFTING_GUI_TITLE)) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        // If clicking inside the top inventory
        if (slot >= 0 && slot < GUIConstants.GUI_SIZE) {
            boolean isEditor = titleText.contains("[");

            // Allow interaction only on recipe slots and result slot
            if (GUIConstants.isRecipeSlot(slot) || slot == GUIConstants.RESULT_SLOT) {
                if (!isEditor && slot == GUIConstants.RESULT_SLOT) {
                    // Crafting view: player is trying to take the result
                    handleCraftResult(event, player, titleText);
                }
                // In editor mode, allow free interaction with recipe/result slots
                // In crafting view, allow interaction with recipe slots (they show items but are locked)
                if (!isEditor && GUIConstants.isRecipeSlot(slot)) {
                    event.setCancelled(true);
                }
            } else {
                // Filler slots — always cancel
                event.setCancelled(true);
            }
        }
        // Prevent Q-drop and cursor-drop while in crafting GUI
        if (event.getAction() == InventoryAction.DROP_ALL_SLOT
                || event.getAction() == InventoryAction.DROP_ONE_SLOT
                || event.getAction() == InventoryAction.DROP_ALL_CURSOR
                || event.getAction() == InventoryAction.DROP_ONE_CURSOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Component dragTitle = event.getView().title();
        if (dragTitle == null) return;
        String dragTitleText = PlainTextComponentSerializer.plainText().serialize(dragTitle);
        if (!dragTitleText.startsWith(GUIConstants.CRAFTING_GUI_TITLE)) return;

        // Cancel drag into filler slots
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= 0 && rawSlot < GUIConstants.GUI_SIZE
                    && !GUIConstants.isRecipeSlot(rawSlot) && rawSlot != GUIConstants.RESULT_SLOT) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Component closeTitle = event.getView().title();
        if (closeTitle == null) return;
        String title = PlainTextComponentSerializer.plainText().serialize(closeTitle);
        if (!title.startsWith(GUIConstants.CRAFTING_GUI_TITLE)) return;

        // Only save if this is an editor GUI (title contains "[recipeId]")
        if (!title.contains("[")) return;

        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();

        // Parse type and recipeId from title: "MCCraft - {type} [{recipeId}]"
        String afterDash = title.substring(title.indexOf("-") + 2);
        String type = afterDash.substring(0, afterDash.indexOf("[")).trim();
        String recipeId = afterDash.substring(afterDash.indexOf("[") + 1, afterDash.indexOf("]"));

        String contents = CraftingGUI.serializeFromGUI(inv);
        MCCraftProvider provider = MCCraftProvider.getInstance();
        if (provider != null && contents != null) {
            provider.saveItem(recipeId, type, contents).thenRun(() ->
                    player.sendMessage(Component.translatable("mcengine.mccraft.msg.recipe.saved")
                            .arguments(Component.text(recipeId)).color(NamedTextColor.GREEN))
            ).exceptionally(ex -> {
                player.sendMessage(Component.translatable("mcengine.mccraft.msg.error")
                        .arguments(Component.text(ex.getMessage())).color(NamedTextColor.RED));
                return null;
            });
        }
    }

    /**
     * Handles the logic when a player clicks the result slot in a crafting view.
     * Validates the head item requirement and decrements it.
     */
    private void handleCraftResult(InventoryClickEvent event, Player player, String title) {
        // Parse type from title: "MCCraft - {type}"
        String type = title.substring(title.indexOf("-") + 2).trim();

        // Default type doesn't need a head item
        if ("default".equalsIgnoreCase(type)) return;

        // Check for head item in player's inventory
        ItemStack headItem = findHeadItem(player, type);
        if (headItem == null) {
            event.setCancelled(true);
            player.sendMessage(Component.translatable("mcengine.mccraft.msg.craft.no.head")
                    .arguments(Component.text(type)).color(NamedTextColor.RED));
            return;
        }

        // Decrement the head item
        if (headItem.getAmount() > 1) {
            headItem.setAmount(headItem.getAmount() - 1);
        } else {
            player.getInventory().remove(headItem);
        }
    }

    /**
     * Finds the head item with the matching mccraft_type in the player's inventory.
     *
     * @param player the player
     * @param type   the required type value
     * @return the matching ItemStack, or null if not found
     */
    private ItemStack findHeadItem(Player player, String type) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (pdc.has(MCCRAFT_TYPE_KEY, PersistentDataType.STRING)) {
                String value = pdc.get(MCCRAFT_TYPE_KEY, PersistentDataType.STRING);
                if (type.equals(value)) {
                    return item;
                }
            }
        }
        return null;
    }
}
