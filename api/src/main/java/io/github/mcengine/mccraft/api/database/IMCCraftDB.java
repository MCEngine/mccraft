package io.github.mcengine.mccraft.api.database;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Database interface for MCCraft item/recipe storage.
 */
public interface IMCCraftDB {

    /**
     * Creates the mccraft_item table if it does not exist.
     *
     * @throws SQLException if a database access error occurs
     */
    void createTable() throws SQLException;

    /**
     * Inserts or updates an item record.
     *
     * @param id       the unique item identifier
     * @param type     the station type (e.g., "default" or a custom type)
     * @param contents the Base64-encoded recipe contents
     * @throws SQLException if a database access error occurs
     */
    void upsertItem(String id, String type, String contents) throws SQLException;

    /**
     * Retrieves an item record by its id.
     *
     * @param id the unique item identifier
     * @return a map containing column names to values, or null if not found
     * @throws SQLException if a database access error occurs
     */
    Map<String, String> getItem(String id) throws SQLException;

    /**
     * Retrieves all item records for a given type.
     *
     * @param type the station type
     * @return a list of maps, each representing a row
     * @throws SQLException if a database access error occurs
     */
    List<Map<String, String>> getItemsByType(String type) throws SQLException;

    /**
     * Retrieves all distinct types registered in the database.
     *
     * @return a list of type strings
     * @throws SQLException if a database access error occurs
     */
    List<String> getTypes() throws SQLException;

    /**
     * Deletes an item record by its id.
     *
     * @param id the unique item identifier
     * @throws SQLException if a database access error occurs
     */
    void deleteItem(String id) throws SQLException;

    /**
     * Closes the database connection or pool.
     */
    void close();
}
