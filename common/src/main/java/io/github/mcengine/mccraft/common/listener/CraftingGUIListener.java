package io.github.mcengine.mccraft.common.listener;

import io.github.mcengine.mccraft.common.MCCraftProvider;
import io.github.mcengine.mccraft.common.cache.RecipeCache;
import io.github.mcengine.mccraft.common.gui.CraftingGUI;
import io.github.mcengine.mccraft.common.util.GUIConstants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
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
 * For crafting view GUIs, players place ingredients in recipe slots;
 * the result slot is dynamically populated when the grid matches a cached recipe.
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
        boolean isEditor = titleText.contains("[");

        // Prevent Q-drop and cursor-drop while in crafting GUI
        if (event.getAction() == InventoryAction.DROP_ALL_SLOT
                || event.getAction() == InventoryAction.DROP_ONE_SLOT
                || event.getAction() == InventoryAction.DROP_ALL_CURSOR
                || event.getAction() == InventoryAction.DROP_ONE_CURSOR) {
            event.setCancelled(true);
            return;
        }

        // Clicking inside the top inventory
        if (slot >= 0 && slot < GUIConstants.GUI_SIZE) {
            if (GUIConstants.isRecipeSlot(slot)) {
                // Recipe slots: allow interaction in both editor and crafting view
                // After the click resolves, update the result slot in crafting view
                if (!isEditor) {
                    scheduleRecipeCheck(event.getView().getTopInventory(), titleText);
                }
            } else if (slot == GUIConstants.RESULT_SLOT) {
                if (isEditor) {
                    // Editor: allow free interaction with result slot
                } else {
                    // Crafting view: handle taking the result
                    handleCraftResult(event, player, titleText);
                }
            } else {
                // Filler slots — always cancel
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Component dragTitle = event.getView().title();
        if (dragTitle == null) return;
        String dragTitleText = PlainTextComponentSerializer.plainText().serialize(dragTitle);
        if (!dragTitleText.startsWith(GUIConstants.CRAFTING_GUI_TITLE)) return;

        boolean isEditor = dragTitleText.contains("[");

        // Cancel drag into filler slots
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= 0 && rawSlot < GUIConstants.GUI_SIZE
                    && !GUIConstants.isRecipeSlot(rawSlot) && rawSlot != GUIConstants.RESULT_SLOT) {
                event.setCancelled(true);
                return;
            }
        }

        // In crafting view, schedule a recipe check after drag
        if (!isEditor) {
            scheduleRecipeCheck(event.getView().getTopInventory(), dragTitleText);
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
     * Schedules a recipe match check on the next tick (after the click/drag resolves).
     */
    private void scheduleRecipeCheck(Inventory inv, String titleText) {
        Bukkit.getScheduler().runTask(
                Bukkit.getPluginManager().getPlugin("MCCraft"),
                () -> updateResultSlot(inv, titleText)
        );
    }

    /**
     * Reads the 9 recipe slots from the inventory, matches against the cache,
     * and sets or clears the result slot accordingly.
     */
    private void updateResultSlot(Inventory inv, String titleText) {
        String type = titleText.substring(titleText.indexOf("-") + 2).trim();

        int[] recipeSlots = GUIConstants.RECIPE_SLOTS;
        ItemStack[] playerGrid = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            ItemStack item = inv.getItem(recipeSlots[i]);
            playerGrid[i] = (item == null || item.getType() == Material.AIR
                    || item.getType() == Material.RED_STAINED_GLASS_PANE) ? null : item;
        }

        RecipeCache.CachedRecipe match = RecipeCache.getInstance().matchRecipe(type, playerGrid);
        if (match != null && match.getResult() != null) {
            inv.setItem(GUIConstants.RESULT_SLOT, match.getResult().clone());
        } else {
            inv.setItem(GUIConstants.RESULT_SLOT, null);
        }
    }

    /**
     * Handles the logic when a player clicks the result slot in a crafting view.
     * Validates the head item requirement, decrements ingredients, and gives the result.
     */
    private void handleCraftResult(InventoryClickEvent event, Player player, String titleText) {
        String type = titleText.substring(titleText.indexOf("-") + 2).trim();
        Inventory inv = event.getView().getTopInventory();

        // Read the current grid
        int[] recipeSlots = GUIConstants.RECIPE_SLOTS;
        ItemStack[] playerGrid = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            ItemStack item = inv.getItem(recipeSlots[i]);
            playerGrid[i] = (item == null || item.getType() == Material.AIR
                    || item.getType() == Material.RED_STAINED_GLASS_PANE) ? null : item;
        }

        RecipeCache.CachedRecipe match = RecipeCache.getInstance().matchRecipe(type, playerGrid);
        if (match == null || match.getResult() == null) {
            event.setCancelled(true);
            return;
        }

        // Check for head item in player's inventory (non-default types)
        if (!"default".equalsIgnoreCase(type)) {
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

        // Decrement ingredients from the grid
        ItemStack[] recipeGrid = match.getGrid();
        for (int i = 0; i < 9; i++) {
            if (recipeGrid[i] != null && !recipeGrid[i].getType().isAir()) {
                ItemStack slotItem = inv.getItem(recipeSlots[i]);
                if (slotItem != null) {
                    int remaining = slotItem.getAmount() - recipeGrid[i].getAmount();
                    if (remaining <= 0) {
                        inv.setItem(recipeSlots[i], null);
                    } else {
                        slotItem.setAmount(remaining);
                    }
                }
            }
        }

        // Give the result to the player, respecting existing cursor item
        event.setCancelled(true);
        ItemStack resultItem = match.getResult().clone();
        ItemStack cursor = event.getCursor();

        if (cursor != null && !cursor.getType().isAir()) {
            // If same item type, try to stack
            if (cursor.isSimilar(resultItem)) {
                int space = cursor.getMaxStackSize() - cursor.getAmount();
                int toAdd = Math.min(space, resultItem.getAmount());
                cursor.setAmount(cursor.getAmount() + toAdd);
                int remaining = resultItem.getAmount() - toAdd;
                if (remaining > 0) {
                    resultItem.setAmount(remaining);
                    Map<Integer, ItemStack> overflow = player.getInventory().addItem(resultItem);
                    overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                }
                player.setItemOnCursor(cursor);
            } else {
                // Different item in cursor — place result into inventory
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(resultItem);
                overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            }
        } else {
            player.setItemOnCursor(resultItem);
        }

        // Re-check recipe after consuming ingredients
        scheduleRecipeCheck(inv, titleText);
    }

    /**
     * Finds the head item with the matching mccraft_type in the player's inventory.
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
