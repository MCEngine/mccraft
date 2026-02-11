package io.github.mcengine.mccraft.common.gui;

import io.github.mcengine.mccraft.common.util.GUIConstants;
import io.github.mcengine.mccraft.common.util.ItemSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds the editor list GUI that displays all registered recipes for a given type.
 * Clicking an item opens the crafting editor for that specific recipe.
 */
public final class EditorListGUI {

    private EditorListGUI() {}

    /**
     * Opens the editor list GUI for a player.
     *
     * @param player the player
     * @param type   the station type
     * @param items  the list of database rows for this type
     */
    public static void open(Player player, String type, List<Map<String, String>> items) {
        Component title = Component.text(GUIConstants.EDITOR_LIST_TITLE + " - " + type);
        int size = Math.min(54, ((items.size() / 9) + 1) * 9);
        if (size < 9) size = 9;
        Inventory inv = Bukkit.createInventory(null, size, title);

        int slot = 0;
        for (Map<String, String> row : items) {
            String id = row.get("id");
            // Skip the head item entry
            if (id != null && id.endsWith("/__head__")) continue;
            if (slot >= size) break;

            String contents = row.get("contents");
            ItemStack display;

            // Try to show the result item as the display icon
            if (contents != null && !contents.isEmpty()) {
                ItemStack[] decoded = ItemSerializer.arrayFromBase64(contents);
                if (decoded != null && decoded.length > 9 && decoded[9] != null) {
                    display = decoded[9].clone();
                } else {
                    display = new ItemStack(Material.PAPER);
                }
            } else {
                display = new ItemStack(Material.PAPER);
            }

            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(id != null ? id : "Unknown", NamedTextColor.YELLOW));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Type: " + type, NamedTextColor.GRAY));
                lore.add(Component.text("Click to edit", NamedTextColor.GREEN));
                meta.lore(lore);
                display.setItemMeta(meta);
            }

            inv.setItem(slot, display);
            slot++;
        }

        player.openInventory(inv);
    }
}
