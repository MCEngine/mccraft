package io.github.mcengine.mccraft.common.gui;

import io.github.mcengine.mccraft.common.util.GUIConstants;
import io.github.mcengine.mccraft.common.util.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

/**
 * Builds and manages the virtual 54-slot crafting GUI.
 */
public final class CraftingGUI {

    private CraftingGUI() {}

    /**
     * Opens a blank crafting editor GUI for defining a new recipe.
     *
     * @param player   the player to open the GUI for
     * @param type     the station type
     * @param recipeId the recipe identifier
     */
    public static void openEditor(Player player, String type, String recipeId) {
        String title = GUIConstants.CRAFTING_GUI_TITLE + " - " + type + " [" + recipeId + "]";
        Inventory inv = Bukkit.createInventory(null, GUIConstants.GUI_SIZE, title);
        fillFiller(inv);
        player.openInventory(inv);
    }

    /**
     * Opens a crafting editor GUI pre-populated with an existing recipe.
     *
     * @param player   the player to open the GUI for
     * @param type     the station type
     * @param recipeId the recipe identifier
     * @param row      the database row containing the recipe contents
     */
    public static void openEditorWithData(Player player, String type, String recipeId, Map<String, String> row) {
        String title = GUIConstants.CRAFTING_GUI_TITLE + " - " + type + " [" + recipeId + "]";
        Inventory inv = Bukkit.createInventory(null, GUIConstants.GUI_SIZE, title);
        fillFiller(inv);

        String contents = row.get("contents");
        if (contents != null && !contents.isEmpty()) {
            ItemStack[] decoded = ItemSerializer.arrayFromBase64(contents);
            if (decoded != null) {
                // First 9 items are the recipe grid, 10th is the result
                int[] recipeSlots = GUIConstants.RECIPE_SLOTS;
                for (int i = 0; i < recipeSlots.length && i < decoded.length; i++) {
                    if (decoded[i] != null) {
                        inv.setItem(recipeSlots[i], decoded[i]);
                    }
                }
                if (decoded.length > 9 && decoded[9] != null) {
                    inv.setItem(GUIConstants.RESULT_SLOT, decoded[9]);
                }
            }
        }

        player.openInventory(inv);
    }

    /**
     * Opens the crafting GUI for a player to use a recipe (not edit).
     *
     * @param player   the player
     * @param type     the station type
     * @param recipeId the recipe identifier
     * @param row      the database row
     */
    public static void openCraftingView(Player player, String type, String recipeId, Map<String, String> row) {
        String title = GUIConstants.CRAFTING_GUI_TITLE + " - " + type;
        Inventory inv = Bukkit.createInventory(null, GUIConstants.GUI_SIZE, title);
        fillFiller(inv);

        String contents = row.get("contents");
        if (contents != null && !contents.isEmpty()) {
            ItemStack[] decoded = ItemSerializer.arrayFromBase64(contents);
            if (decoded != null) {
                int[] recipeSlots = GUIConstants.RECIPE_SLOTS;
                for (int i = 0; i < recipeSlots.length && i < decoded.length; i++) {
                    if (decoded[i] != null) {
                        inv.setItem(recipeSlots[i], decoded[i]);
                    }
                }
                if (decoded.length > 9 && decoded[9] != null) {
                    inv.setItem(GUIConstants.RESULT_SLOT, decoded[9]);
                }
            }
        }

        player.openInventory(inv);
    }

    /**
     * Fills all non-functional slots with red glass pane filler items.
     *
     * @param inv the inventory to fill
     */
    private static void fillFiller(Inventory inv) {
        ItemStack filler = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text(" "));
            filler.setItemMeta(meta);
        }
        for (int i = 0; i < GUIConstants.GUI_SIZE; i++) {
            if (!GUIConstants.isRecipeSlot(i) && i != GUIConstants.RESULT_SLOT) {
                inv.setItem(i, filler);
            }
        }
    }

    /**
     * Extracts the recipe grid and result from a GUI inventory and serializes to Base64.
     *
     * @param inv the inventory
     * @return the Base64-encoded contents (9 recipe slots + 1 result = 10 items)
     */
    public static String serializeFromGUI(Inventory inv) {
        ItemStack[] items = new ItemStack[10];
        int[] recipeSlots = GUIConstants.RECIPE_SLOTS;
        for (int i = 0; i < recipeSlots.length; i++) {
            ItemStack slot = inv.getItem(recipeSlots[i]);
            items[i] = isFillerOrAir(slot) ? null : slot;
        }
        ItemStack result = inv.getItem(GUIConstants.RESULT_SLOT);
        items[9] = isFillerOrAir(result) ? null : result;
        return ItemSerializer.arrayToBase64(items);
    }

    /**
     * Checks if an item is air or the red glass pane filler.
     *
     * @param item the item to check
     * @return true if it is filler or air
     */
    private static boolean isFillerOrAir(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return true;
        return item.getType() == Material.RED_STAINED_GLASS_PANE;
    }
}
