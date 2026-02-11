package io.github.mcengine.mccraft.common.command.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.github.mcengine.mccraft.api.command.ICraftCommandHandle;
import io.github.mcengine.mccraft.common.MCCraftProvider;
import io.github.mcengine.mccraft.common.command.MCCraftCommandManager;
import io.github.mcengine.mccraft.common.util.ItemSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * Handles /craft type create {text} {head_item_base64}
 * Registers a new station type with duplicate prevention.
 */
public class HandleType implements ICraftCommandHandle {

    @Override
    public void invoke(CommandSender sender, String[] args) {
        // args: ["create", "{type}", "{base64}"]
        if (args.length < 3 || !"create".equalsIgnoreCase(args[0])) {
            MCCraftCommandManager.send(sender, Component.translatable("mcengine.mccraft.msg.type.usage")
                    .color(NamedTextColor.RED));
            return;
        }

        String type = args[1];
        String headItemBase64 = args[2];
        MCCraftProvider provider = MCCraftProvider.getInstance();

        // Accept either a fully serialized ItemStack Base64 (our format) or an HDB-style texture Base64.
        ItemStack headItem = ItemSerializer.fromBase64(headItemBase64);
        if (headItem == null) {
            headItem = createHeadFromTexture(headItemBase64);
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
     */
    private ItemStack createHeadFromTexture(String textureBase64) {
        try {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", textureBase64));

            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
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
