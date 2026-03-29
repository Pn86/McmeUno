package cn.pn86.pnextremesurvival.data;

import cn.pn86.pnextremesurvival.PnExtremeSurvivalPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;

public class PlayerDataRepository {

    private final PnExtremeSurvivalPlugin plugin;
    private Connection connection;

    public PlayerDataRepository(PnExtremeSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        File dbFile = new File(plugin.getDataFolder(), "database.db");
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Failed to create plugin data directory: " + parent.getAbsolutePath());
        }

        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            executePragma("PRAGMA busy_timeout = 5000;");
            executePragma("PRAGMA journal_mode = WAL;");
            executePragma("PRAGMA synchronous = NORMAL;");
            createTable();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize database at " + dbFile.getAbsolutePath(), e);
        }
    }

    private void executePragma(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void createTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS player_data (
                  uuid TEXT PRIMARY KEY,
                  max_health REAL NOT NULL,
                  permanently_dead INTEGER NOT NULL,
                  name TEXT,
                  updated_at INTEGER NOT NULL
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public Optional<PlayerLifeData> load(UUID uuid) {
        if (connection == null) {
            return Optional.empty();
        }

        String sql = "SELECT max_health, permanently_dead FROM player_data WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new PlayerLifeData(
                        resultSet.getDouble("max_health"),
                        resultSet.getInt("permanently_dead") == 1
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load player data: " + e.getMessage());
            return Optional.empty();
        }
    }

    public synchronized void save(UUID uuid, String name, double maxHealth, boolean permanentlyDead) {
        if (connection == null) {
            return;
        }

        String sql = """
                INSERT INTO player_data(uuid, max_health, permanently_dead, name, updated_at)
                VALUES (?, ?, ?, ?, strftime('%s', 'now'))
                ON CONFLICT(uuid) DO UPDATE SET
                  max_health = excluded.max_health,
                  permanently_dead = excluded.permanently_dead,
                  name = excluded.name,
                  updated_at = excluded.updated_at
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setDouble(2, maxHealth);
            statement.setInt(3, permanentlyDead ? 1 : 0);
            statement.setString(4, name);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save player data: " + e.getMessage());
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to close database: " + e.getMessage());
            }
        }
    }
}
