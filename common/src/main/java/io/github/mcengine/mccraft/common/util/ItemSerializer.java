package io.github.mcengine.mccraft.common.util;

import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Base64;

/**
 * Utility class for serializing and deserializing {@link ItemStack} objects to/from Base64 strings.
 * Uses Paper's NBT-based {@code serializeAsBytes}/{@code deserializeBytes} API (no deprecated streams).
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
        try {
            byte[] data = item.serializeAsBytes();
            return Base64.getEncoder().encodeToString(data);
        } catch (Exception e) {
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
            return ItemStack.deserializeBytes(data);
        } catch (Exception e) {
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
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Write length as 4-byte int
            baos.write(ByteBuffer.allocate(4).putInt(items.length).array());
            for (ItemStack item : items) {
                if (item == null) {
                    baos.write(ByteBuffer.allocate(4).putInt(0).array());
                } else {
                    byte[] itemBytes = item.serializeAsBytes();
                    baos.write(ByteBuffer.allocate(4).putInt(itemBytes.length).array());
                    baos.write(itemBytes);
                }
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
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
            ByteBuffer buf = ByteBuffer.wrap(data);
            int length = buf.getInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                int itemLen = buf.getInt();
                if (itemLen == 0) {
                    items[i] = null;
                } else {
                    byte[] itemBytes = new byte[itemLen];
                    buf.get(itemBytes);
                    items[i] = ItemStack.deserializeBytes(itemBytes);
                }
            }
            return items;
        } catch (Exception e) {
            return null;
        }
    }
}
