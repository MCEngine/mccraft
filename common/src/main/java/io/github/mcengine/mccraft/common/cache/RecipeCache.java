package io.github.mcengine.mccraft.common.cache;

import io.github.mcengine.mccraft.common.util.ItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for recipes and type head items.
 * Provides fast access without hitting the database on every interaction.
 */
public final class RecipeCache {

    private static final RecipeCache INSTANCE = new RecipeCache();

    /** type -> list of CachedRecipe */
    private final ConcurrentHashMap<String, List<CachedRecipe>> recipesByType = new ConcurrentHashMap<>();

    /** type -> head item Base64 */
    private final ConcurrentHashMap<String, String> typeHeadItems = new ConcurrentHashMap<>();

    private RecipeCache() {}

    public static RecipeCache getInstance() {
        return INSTANCE;
    }

    /**
     * A cached recipe storing cloned ItemStack grid and result.
     */
    public static class CachedRecipe {
        private final String id;
        private final String type;
        private final ItemStack[] grid;   // 9 recipe slots
        private final ItemStack result;   // result item

        public CachedRecipe(String id, String type, ItemStack[] grid, ItemStack result) {
            this.id = id;
            this.type = type;
            this.grid = grid != null ? grid.clone() : new ItemStack[9];
            this.result = result != null ? result.clone() : null;
        }

        public String getId() { return id; }
        public String getType() { return type; }
        public ItemStack[] getGrid() { return grid; }
        public ItemStack getResult() { return result; }
    }

    // --- Population ---

    /**
     * Loads all recipes from DB rows into the cache.
     * Call this once on startup.
     *
     * @param allItems list of all recipe rows from the database
     */
    public void loadRecipes(List<Map<String, String>> allItems) {
        recipesByType.clear();
        if (allItems == null) return;
        for (Map<String, String> row : allItems) {
            String id = row.get("id");
            String type = row.get("type");
            String contents = row.get("contents");
            if (id == null || type == null || contents == null) continue;
            addRecipeFromBase64(id, type, contents);
        }
    }

    /**
     * Loads all type head items from DB rows into the cache.
     *
     * @param types list of type rows (type, head_item)
     */
    public void loadTypes(List<Map<String, String>> types) {
        typeHeadItems.clear();
        if (types == null) return;
        for (Map<String, String> row : types) {
            String type = row.get("type");
            String headItem = row.get("head_item");
            if (type != null && headItem != null) {
                typeHeadItems.put(type, headItem);
            }
        }
    }

    // --- Mutation ---

    /**
     * Adds or updates a recipe in the cache from a Base64 contents string.
     */
    public void putRecipe(String id, String type, String contentsBase64) {
        removeRecipe(id);
        addRecipeFromBase64(id, type, contentsBase64);
    }

    /**
     * Removes a recipe from the cache by id.
     */
    public void removeRecipe(String id) {
        for (Map.Entry<String, List<CachedRecipe>> entry : recipesByType.entrySet()) {
            entry.getValue().removeIf(r -> r.getId().equals(id));
        }
    }

    /**
     * Adds or updates a type head item in the cache.
     */
    public void putType(String type, String headItemBase64) {
        typeHeadItems.put(type, headItemBase64);
    }

    // --- Queries ---

    /**
     * Gets all cached recipes for a given type.
     */
    public List<CachedRecipe> getRecipes(String type) {
        List<CachedRecipe> list = recipesByType.get(type);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    /**
     * Gets the head item Base64 for a type.
     */
    public String getTypeHeadItem(String type) {
        return typeHeadItems.get(type);
    }

    /**
     * Checks if a type exists in the cache.
     */
    public boolean typeExists(String type) {
        return typeHeadItems.containsKey(type);
    }

    /**
     * Gets all cached type names.
     */
    public List<String> getAllTypes() {
        return new ArrayList<>(typeHeadItems.keySet());
    }

    /**
     * Matches the player's 9-slot grid against all recipes for the given type.
     * Returns the matching CachedRecipe, or null if no match.
     *
     * @param type       the station type
     * @param playerGrid 9 ItemStacks from the player's crafting slots (null = empty)
     * @return the matching recipe, or null
     */
    public CachedRecipe matchRecipe(String type, ItemStack[] playerGrid) {
        if (playerGrid == null || playerGrid.length != 9) return null;
        List<CachedRecipe> recipes = getRecipes(type);
        for (CachedRecipe recipe : recipes) {
            if (gridsMatch(recipe.getGrid(), playerGrid)) {
                return recipe;
            }
        }
        return null;
    }

    // --- Internal ---

    /**
     * Deserializes a Base64 contents string and stores the recipe as ItemStacks.
     */
    private void addRecipeFromBase64(String id, String type, String contentsBase64) {
        ItemStack[] decoded = ItemSerializer.arrayFromBase64(contentsBase64);
        ItemStack[] grid = new ItemStack[9];
        ItemStack result = null;
        if (decoded != null && decoded.length >= 10) {
            System.arraycopy(decoded, 0, grid, 0, 9);
            result = decoded[9];
        }
        recipesByType.computeIfAbsent(type, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new CachedRecipe(id, type, grid, result));
    }

    /**
     * Compares two 9-slot grids. Items match if they are the same material and amount,
     * or both are null/air.
     */
    private boolean gridsMatch(ItemStack[] recipeGrid, ItemStack[] playerGrid) {
        for (int i = 0; i < 9; i++) {
            ItemStack expected = recipeGrid[i];
            ItemStack actual = playerGrid[i];

            boolean expectedEmpty = isEmpty(expected);
            boolean actualEmpty = isEmpty(actual);

            if (expectedEmpty && actualEmpty) continue;
            if (expectedEmpty || actualEmpty) return false;
            if (expected.getType() != actual.getType()) return false;
            if (actual.getAmount() < expected.getAmount()) return false;
        }
        return true;
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }
}
