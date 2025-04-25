package net.fliuxx.aFKGuard.managers;

import net.fliuxx.aFKGuard.AFKGuard;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {

    private final AFKGuard plugin;
    private Connection connection;
    private final String dbPath;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DatabaseManager(AFKGuard plugin) {
        this.plugin = plugin;
        this.dbPath = plugin.getDataFolder() + File.separator + "afk_logs.db";
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            try (Statement statement = connection.createStatement()) {
                // Tabella per i giocatori messi in AFK automaticamente
                statement.execute(
                        "CREATE TABLE IF NOT EXISTS afk_auto_logs (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "player_uuid VARCHAR(36) NOT NULL, " +
                                "player_name VARCHAR(16) NOT NULL, " +
                                "timestamp DATETIME NOT NULL, " +
                                "inactive_time INTEGER NOT NULL, " +
                                "auto_afk BOOLEAN NOT NULL DEFAULT 1" +
                                ")"
                );

                // Tabella per i giocatori kickati per inattività
                statement.execute(
                        "CREATE TABLE IF NOT EXISTS afk_kick_logs (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "player_uuid VARCHAR(36) NOT NULL, " +
                                "player_name VARCHAR(16) NOT NULL, " +
                                "timestamp DATETIME NOT NULL, " +
                                "total_afk_time INTEGER NOT NULL" +
                                ")"
                );

                // Tabella per i giocatori teleportati in zone AFK
                statement.execute(
                        "CREATE TABLE IF NOT EXISTS afk_teleport_logs (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "player_uuid VARCHAR(36) NOT NULL, " +
                                "player_name VARCHAR(16) NOT NULL, " +
                                "timestamp DATETIME NOT NULL, " +
                                "world VARCHAR(64) NOT NULL, " +
                                "x DOUBLE NOT NULL, " +
                                "y DOUBLE NOT NULL, " +
                                "z DOUBLE NOT NULL" +
                                ")"
                );
            }

            plugin.getLogger().info("Database SQLite inizializzato con successo.");
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().severe("Errore durante l'inizializzazione del database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void logAutoAFK(Player player, int inactiveTime) {
        try {
            String query = "INSERT INTO afk_auto_logs (player_uuid, player_name, timestamp, inactive_time, auto_afk) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, player.getName());
                statement.setString(3, LocalDateTime.now().format(dateFormatter));
                statement.setInt(4, inactiveTime);
                statement.setBoolean(5, true);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Errore durante il salvataggio del log AFK automatico: " + e.getMessage());
        }
    }

    public void logAFKKick(Player player, int totalAfkTime) {
        try {
            String query = "INSERT INTO afk_kick_logs (player_uuid, player_name, timestamp, total_afk_time) VALUES (?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, player.getName());
                statement.setString(3, LocalDateTime.now().format(dateFormatter));
                statement.setInt(4, totalAfkTime);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Errore durante il salvataggio del log di kick AFK: " + e.getMessage());
        }
    }

    public void logAFKTeleport(Player player, String world, double x, double y, double z) {
        try {
            String query = "INSERT INTO afk_teleport_logs (player_uuid, player_name, timestamp, world, x, y, z) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, player.getName());
                statement.setString(3, LocalDateTime.now().format(dateFormatter));
                statement.setString(4, world);
                statement.setDouble(5, x);
                statement.setDouble(6, y);
                statement.setDouble(7, z);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Errore durante il salvataggio del log di teleport AFK: " + e.getMessage());
        }
    }

    public List<String> getPlayerAFKHistory(UUID playerUUID, int limit) {
        List<String> history = new ArrayList<>();

        try {
            String query = "SELECT 'AUTO_AFK' as type, timestamp, inactive_time, NULL as total_afk_time, NULL as world, NULL as x, NULL as y, NULL as z " +
                    "FROM afk_auto_logs WHERE player_uuid = ? " +
                    "UNION ALL " +
                    "SELECT 'KICK' as type, timestamp, NULL as inactive_time, total_afk_time, NULL as world, NULL as x, NULL as y, NULL as z " +
                    "FROM afk_kick_logs WHERE player_uuid = ? " +
                    "UNION ALL " +
                    "SELECT 'TELEPORT' as type, timestamp, NULL as inactive_time, NULL as total_afk_time, world, x, y, z " +
                    "FROM afk_teleport_logs WHERE player_uuid = ? " +
                    "ORDER BY timestamp DESC LIMIT ?";

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, playerUUID.toString());
                statement.setString(2, playerUUID.toString());
                statement.setString(3, playerUUID.toString());
                statement.setInt(4, limit);

                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        String type = results.getString("type");
                        String timestamp = results.getString("timestamp");

                        switch (type) {
                            case "AUTO_AFK":
                                int inactiveTime = results.getInt("inactive_time");
                                history.add(String.format("[%s] Impostato AFK automaticamente dopo %d secondi di inattività", timestamp, inactiveTime));
                                break;
                            case "KICK":
                                int totalAfkTime = results.getInt("total_afk_time");
                                history.add(String.format("[%s] Kickato per inattività dopo %d secondi in AFK", timestamp, totalAfkTime));
                                break;
                            case "TELEPORT":
                                String world = results.getString("world");
                                double x = results.getDouble("x");
                                double y = results.getDouble("y");
                                double z = results.getDouble("z");
                                history.add(String.format("[%s] Teleportato in area AFK (%s: %.2f, %.2f, %.2f)", timestamp, world, x, y, z));
                                break;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Errore durante il recupero della cronologia AFK: " + e.getMessage());
        }

        return history;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Connessione al database chiusa correttamente.");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Errore durante la chiusura della connessione al database: " + e.getMessage());
        }
    }
}