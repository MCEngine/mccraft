package io.github.mcengine.mccraft.common.database;

import io.github.mcengine.mccraft.api.database.IMCCraftDB;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLite implementation of {@link IMCCraftDB}.
 */
public class MCCraftSQLite implements IMCCraftDB {

    private Connection conn;

    public MCCraftSQLite(Plugin plugin) {
        File dataFolder = new File(plugin.getDataFolder(), "mccraft.db");
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        try {
            Class.forName("org.sqlite.JDBC");
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + dataFolder.getAbsolutePath());
            createTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS mccraft_item ("
                + "id VARCHAR(255) NOT NULL, "
                + "type VARCHAR(255) DEFAULT 'default' NOT NULL, "
                + "contents LONGTEXT, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "PRIMARY KEY (id)"
                + ");";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    @Override
    public void upsertItem(String id, String type, String contents) throws SQLException {
        String sql = "INSERT INTO mccraft_item (id, type, contents) VALUES (?, ?, ?) "
                + "ON CONFLICT(id) DO UPDATE SET type = excluded.type, contents = excluded.contents, updated_at = CURRENT_TIMESTAMP";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, type);
            ps.setString(3, contents);
            ps.executeUpdate();
        }
    }

    @Override
    public Map<String, String> getItem(String id) throws SQLException {
        String sql = "SELECT * FROM mccraft_item WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Map<String, String>> getItemsByType(String type) throws SQLException {
        String sql = "SELECT * FROM mccraft_item WHERE type = ?";
        List<Map<String, String>> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        }
        return results;
    }

    @Override
    public List<String> getTypes() throws SQLException {
        String sql = "SELECT DISTINCT type FROM mccraft_item";
        List<String> types = new ArrayList<>();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                types.add(rs.getString("type"));
            }
        }
        return types;
    }

    @Override
    public void deleteItem(String id) throws SQLException {
        String sql = "DELETE FROM mccraft_item WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> mapRow(ResultSet rs) throws SQLException {
        Map<String, String> row = new HashMap<>();
        row.put("id", rs.getString("id"));
        row.put("type", rs.getString("type"));
        row.put("contents", rs.getString("contents"));
        row.put("created_at", rs.getString("created_at"));
        row.put("updated_at", rs.getString("updated_at"));
        return row;
    }
}
