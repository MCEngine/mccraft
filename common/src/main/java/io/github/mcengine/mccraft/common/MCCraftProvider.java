package io.github.mcengine.mccraft.common;

import io.github.mcengine.mccraft.api.database.IMCCraftDB;
import io.github.mcengine.mccraft.common.cache.RecipeCache;
import io.github.mcengine.mccraft.common.command.MCCraftCommandManager;
import io.github.mcengine.mccraft.common.listener.MCCraftListenerManager;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Central provider / singleton for the MCCraft system.
 * Wraps all database operations in async futures and holds references
 * to the command and listener managers.
 */
public class MCCraftProvider {

    private static MCCraftProvider instance;
    private final IMCCraftDB db;
    private final Executor asyncExecutor;
    private final MCCraftCommandManager commandManager;
    private final MCCraftListenerManager listenerManager;

    public MCCraftProvider(IMCCraftDB db, Executor asyncExecutor, MCCraftCommandManager commandManager, MCCraftListenerManager listenerManager) {
        this.db = db;
        this.asyncExecutor = asyncExecutor;
        this.commandManager = commandManager;
        this.listenerManager = listenerManager;
        instance = this;
    }

    /**
     * Populates the in-memory cache from the database.
     * Should be called once after construction, on an async thread.
     */
    public CompletableFuture<Void> populateCache() {
        return runAsync(() -> {
            try {
                RecipeCache cache = RecipeCache.getInstance();
                cache.loadRecipes(db.getAllItems());
                cache.loadTypes(db.getAllTypesWithHeadItems());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    public static MCCraftProvider getInstance() {
        return instance;
    }

    private <T> CompletableFuture<T> runAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, asyncExecutor);
    }

    public MCCraftCommandManager getCommandManager() {
        return this.commandManager;
    }

    public MCCraftListenerManager getListenerManager() {
        return this.listenerManager;
    }

    public IMCCraftDB getDb() {
        return this.db;
    }

    // --- Async Database Wrappers ---

    public CompletableFuture<Void> saveItem(String id, String type, String contents) {
        final String normalizedType = normalizeType(type);
        return runAsync(() -> {
            try {
                db.upsertItem(id, normalizedType, contents);
                RecipeCache.getInstance().putRecipe(id, normalizedType, contents);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    public CompletableFuture<Map<String, String>> getItem(String id) {
        return runAsync(() -> {
            try {
                return db.getItem(id);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<List<Map<String, String>>> getItemsByType(String type) {
        final String normalizedType = normalizeType(type);
        return runAsync(() -> {
            try {
                return db.getItemsByType(normalizedType);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<List<String>> getTypes() {
        return runAsync(() -> {
            try {
                return db.getTypes();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> deleteItem(String id) {
        return runAsync(() -> {
            try {
                db.deleteItem(id);
                RecipeCache.getInstance().removeRecipe(id);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    // --- Async Type Table Wrappers ---

    public CompletableFuture<Void> insertType(String type, String headItemBase64) {
        final String normalizedType = normalizeType(type);
        return runAsync(() -> {
            try {
                db.insertType(normalizedType, headItemBase64);
                RecipeCache.getInstance().putType(normalizedType, headItemBase64);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    public CompletableFuture<Boolean> typeExists(String type) {
        final String normalizedType = normalizeType(type);
        return runAsync(() -> {
            try {
                return db.typeExists(normalizedType);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<String> getTypeHeadItem(String type) {
        final String normalizedType = normalizeType(type);
        return runAsync(() -> {
            try {
                return db.getTypeHeadItem(normalizedType);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<List<String>> getAllTypes() {
        return runAsync(() -> {
            try {
                return db.getAllTypes();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String normalizeType(String type) {
        return type == null ? null : type.toLowerCase(Locale.ROOT);
    }

    public void shutdown() {
        if (db != null) db.close();
        instance = null;
    }
}
