package io.github.mcengine.mccraft.common.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Utility class for serializing and deserializing {@link ItemStack} objects to/from Base64 strings.
 */
public final class ItemSerializer {

    private ItemSerializer() {}

    /**
     * Serializes an ItemStack to a Base64-encoded string.
     *
     * @param item the item to serialize
     * @return the Base64 string, or null if serialization fails
     */
    public static String toBase64(ItemStack item) {
        if (item == null) return null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos)) {
            boos.writeObject(item);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deserializes an ItemStack from a Base64-encoded string.
     *
     * @param base64 the Base64 string
     * @return the deserialized ItemStack, or null if deserialization fails
     */
    public static ItemStack fromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            byte[] data = Base64.getDecoder().decode(base64);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                 BukkitObjectInputStream bois = new BukkitObjectInputStream(bais)) {
                return (ItemStack) bois.readObject();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Serializes an array of ItemStacks (recipe grid) to a Base64-encoded string.
     *
     * @param items the items array to serialize
     * @return the Base64 string, or null if serialization fails
     */
    public static String arrayToBase64(ItemStack[] items) {
        if (items == null) return null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos)) {
            boos.writeInt(items.length);
            for (ItemStack item : items) {
                boos.writeObject(item);
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deserializes an array of ItemStacks from a Base64-encoded string.
     *
     * @param base64 the Base64 string
     * @return the deserialized ItemStack array, or null if deserialization fails
     */
    public static ItemStack[] arrayFromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            byte[] data = Base64.getDecoder().decode(base64);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                 BukkitObjectInputStream bois = new BukkitObjectInputStream(bais)) {
                int length = bois.readInt();
                ItemStack[] items = new ItemStack[length];
                for (int i = 0; i < length; i++) {
                    items[i] = (ItemStack) bois.readObject();
                }
                return items;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
