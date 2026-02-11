package io.github.mcengine.mccraft.common.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.mcengine.mccraft.api.database.IMCCraftDB;
import org.bukkit.plugin.Plugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MySQL implementation of {@link IMCCraftDB} using HikariCP connection pooling.
 */
public class MCCraftMySQL implements IMCCraftDB {

    private final HikariDataSource dataSource;

    public MCCraftMySQL(Plugin plugin) {
        String dbUser = plugin.getConfig().getString("db.mysql.user");
        String dbPassword = plugin.getConfig().getString("db.mysql.password");
        String dbHost = plugin.getConfig().getString("db.mysql.host");
        String dbPort = plugin.getConfig().getString("db.mysql.port");
        String dbName = plugin.getConfig().getString("db.mysql.database");
        String dbSsl = plugin.getConfig().getString("db.mysql.ssl");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName + "?useSSL=" + dbSsl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);

        // Pool Settings optimized for Minecraft
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setLeakDetectionThreshold(10000);

        // Performance properties
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);
        try {
            createTable();
        } catch (SQLException e) {
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
                + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                + "PRIMARY KEY (id)"
                + ");";
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    @Override
    public void upsertItem(String id, String type, String contents) throws SQLException {
        String sql = "INSERT INTO mccraft_item (id, type, contents) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE type = VALUES(type), contents = VALUES(contents), updated_at = CURRENT_TIMESTAMP";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, type);
            ps.setString(3, contents);
            ps.executeUpdate();
        }
    }

    @Override
    public Map<String, String> getItem(String id) throws SQLException {
        String sql = "SELECT * FROM mccraft_item WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
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
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
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
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                types.add(rs.getString("type"));
            }
        }
        return types;
    }

    @Override
    public void deleteItem(String id) throws SQLException {
        String sql = "DELETE FROM mccraft_item WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
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
