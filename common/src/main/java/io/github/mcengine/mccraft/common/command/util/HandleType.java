package io.github.mcengine.mccraft.common.command.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.github.mcengine.mccraft.api.command.ICraftCommandHandle;
import io.github.mcengine.mccraft.common.MCCraftProvider;
import io.github.mcengine.mccraft.common.command.MCCraftCommandManager;
import io.github.mcengine.mccraft.common.util.ItemSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Locale;
import java.util.UUID;

/**
 * Handles /craft type create {text} {head_item_base64 | hand}
 * <p>
 * Accepts three input modes for the head item:
 * <ul>
 *   <li>{@code hand} — uses the item the player is currently holding</li>
 *   <li>A serialized ItemStack Base64 (our format)</li>
 *   <li>An HDB-style texture Base64 (JSON with skin URL)</li>
 * </ul>
 * Registers a new station type with duplicate prevention.
 */
public class HandleType implements ICraftCommandHandle {

    @Override
    public void invoke(CommandSender sender, String[] args) {
        // args: ["create", "{type}", "{base64|hand}"]
        if (args.length < 3 || !"create".equalsIgnoreCase(args[0])) {
            MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.type.usage")
                    .color(NamedTextColor.RED));
            return;
        }

        String type = args[1].toLowerCase(Locale.ROOT);
        String thirdArg = args[2];
        MCCraftProvider provider = MCCraftProvider.getInstance();

        ItemStack headItem = null;

        // Mode 1: "hand" — use the item the player is holding
        if ("hand".equalsIgnoreCase(thirdArg)) {
            if (!(sender instanceof Player)) {
                MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.player.only")
                        .color(NamedTextColor.RED));
                return;
            }
            Player player = (Player) sender;
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType() == Material.AIR) {
                MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.type.hand.empty")
                        .color(NamedTextColor.RED));
                return;
            }
            headItem = held.clone();
        }

        // Mode 2: Try our serialized ItemStack Base64
        if (headItem == null) {
            headItem = ItemSerializer.fromBase64(thirdArg);
        }

        // Mode 3: Try HDB-style texture Base64 (JSON containing skin URL)
        if (headItem == null) {
            headItem = createHeadFromTexture(thirdArg);
        }

        if (headItem == null) {
            MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.type.invalid.base64")
                    .arguments(Component.text(type)).color(NamedTextColor.RED));
            return;
        }

        // Serialize the validated ItemStack using our serializer before saving
        String serialized = ItemSerializer.toBase64(headItem);
        if (serialized == null) {
            MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.type.invalid.base64")
                    .arguments(Component.text(type)).color(NamedTextColor.RED));
            return;
        }

        provider.typeExists(type).thenAccept(exists -> {
            if (exists) {
                MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.type.exists")
                        .arguments(Component.text(type)).color(NamedTextColor.RED));
                return;
            }

            provider.insertType(type, serialized).thenRun(() ->
                    MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.type.success")
                            .arguments(Component.text(type)).color(NamedTextColor.GREEN))
            ).exceptionally(ex -> {
                MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.error")
                        .arguments(Component.text(ex.getMessage())).color(NamedTextColor.RED));
                return null;
            });
        }).exceptionally(ex -> {
            MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.error")
                    .arguments(Component.text(ex.getMessage())).color(NamedTextColor.RED));
            return null;
        });
    }

    /**
     * Builds a player head ItemStack from an HDB-style Base64 texture string.
     * Uses Paper's PlayerProfile API with ProfileProperty to set the texture directly.
     */
    private ItemStack createHeadFromTexture(String textureBase64) {
        try {
            // Validate it is valid Base64 that contains texture JSON
            byte[] decoded = java.util.Base64.getDecoder().decode(textureBase64);
            String json = new String(decoded);
            if (!json.contains("textures")) return null;

            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "");
            profile.setProperty(new ProfileProperty("textures", textureBase64));

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setPlayerProfile(profile);
            skull.setItemMeta(meta);
            return skull;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public Component getHelp() {
        return Component.translatable("mcengine.mccraft.msg.type.help");
    }

    @Override
    public String getPermission() {
        return "mcengine.mccraft.type";
    }
}
