package io.github.mcengine.mccraft.common.listener;

import io.github.mcengine.mccraft.common.MCCraftProvider;
import io.github.mcengine.mccraft.common.gui.CraftingGUI;
import io.github.mcengine.mccraft.common.util.GUIConstants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Handles clicks in the editor list GUI.
 * When a player clicks an item, it opens the crafting editor for that recipe.
 */
public class EditorListGUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Component title = event.getView().title();
        if (title == null) return;
        String titleText = PlainTextComponentSerializer.plainText().serialize(title);
        if (!titleText.startsWith(GUIConstants.EDITOR_LIST_TITLE)) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        // Extract the recipe id from the display name
        Component displayName = meta.displayName();
        if (displayName == null) return;

        // The display name is set as the recipe id (plain text)
        String recipeId = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(displayName);

        // Parse type from title: "MCCraft Editor - {type}"
        String type = titleText.substring(titleText.indexOf("-") + 2).trim();

        MCCraftProvider provider = MCCraftProvider.getInstance();
        if (provider == null) return;

        provider.getItem(recipeId).thenAccept(row -> {
            if (row != null) {
                player.getServer().getScheduler().runTask(
                        player.getServer().getPluginManager().getPlugin("MCCraft"),
                        () -> CraftingGUI.openEditorWithData(player, type, recipeId, row)
                );
            } else {
                player.sendMessage(Component.translatable("mcengine.mccraft.msg.error")
                        .arguments(Component.text("Recipe not found")).color(NamedTextColor.RED));
            }
        }).exceptionally(ex -> {
            player.sendMessage(Component.translatable("mcengine.mccraft.msg.error")
                    .arguments(Component.text(ex.getMessage())).color(NamedTextColor.RED));
            return null;
        });
    }
}
