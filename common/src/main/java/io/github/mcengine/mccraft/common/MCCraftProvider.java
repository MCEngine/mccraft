package io.github.mcengine.mccraft.common;

import io.github.mcengine.mccraft.api.database.IMCCraftDB;
import io.github.mcengine.mccraft.common.command.MCCraftCommandManager;
import io.github.mcengine.mccraft.common.listener.MCCraftListenerManager;

import java.sql.SQLException;
import java.util.List;
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
        return runAsync(() -> {
            try {
                db.upsertItem(id, type, contents);
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
        return runAsync(() -> {
            try {
                return db.getItemsByType(type);
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
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    // --- Async Type Table Wrappers ---

    public CompletableFuture<Void> insertType(String type, String headItemBase64) {
        return runAsync(() -> {
            try {
                db.insertType(type, headItemBase64);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    public CompletableFuture<Boolean> typeExists(String type) {
        return runAsync(() -> {
            try {
                return db.typeExists(type);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<String> getTypeHeadItem(String type) {
        return runAsync(() -> {
            try {
                return db.getTypeHeadItem(type);
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

    public void shutdown() {
        if (db != null) db.close();
        instance = null;
    }
}
