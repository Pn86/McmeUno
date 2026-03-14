package uno.mcme.pnmoney.data;

import uno.mcme.pnmoney.PnMoneyPlugin;

import java.io.File;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {

    private final PnMoneyPlugin plugin;
    private Connection connection;
    private StorageType storageType;

    public DatabaseManager(PnMoneyPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        connectByConfig();
    }

    public void reload() {
        close();
        connectByConfig();
    }

    private void connectByConfig() {
        String configured = plugin.getConfig().getString("storage.type", "sqlite").toLowerCase();
        StorageType targetType = "mysql".equals(configured) ? StorageType.MYSQL : StorageType.SQLITE;

        try {
            if (targetType == StorageType.MYSQL) {
                connectMysql();
                ensureTable();
                File sqliteFile = new File(plugin.getDataFolder(), "pnmoney.db");
                if (sqliteFile.exists()) {
                    migrateFromSqlite(sqliteFile);
                }
            } else {
                connectSqlite();
                ensureTable();
            }
            storageType = targetType;
            plugin.getLogger().info("PnMoney using storage: " + storageType);
        } catch (Exception ex) {
            plugin.getLogger().severe("Database init failed: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    private void connectSqlite() throws Exception {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        Class.forName("org.sqlite.JDBC");
        File dbFile = new File(plugin.getDataFolder(), "pnmoney.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
    }

    private void connectMysql() throws Exception {
        Class.forName("org.mariadb.jdbc.Driver");
        String host = plugin.getConfig().getString("storage.mysql.host", "127.0.0.1");
        int port = plugin.getConfig().getInt("storage.mysql.port", 3306);
        String database = plugin.getConfig().getString("storage.mysql.database", "minecraft");
        String user = plugin.getConfig().getString("storage.mysql.username", "root");
        String password = plugin.getConfig().getString("storage.mysql.password", "");
        String url = "jdbc:mariadb://" + host + ":" + port + "/" + database + "?useUnicode=true&characterEncoding=utf8";
        connection = DriverManager.getConnection(url, user, password);
    }

    private void ensureTable() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS pnmoney_balances (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "name VARCHAR(64) NOT NULL," +
                    "balance DECIMAL(32, 8) NOT NULL" +
                    ")");
        }
    }

    private void migrateFromSqlite(File sqliteFile) {
        plugin.getLogger().info("Trying to migrate data from SQLite to MySQL...");
        String sqliteUrl = "jdbc:sqlite:" + sqliteFile.getAbsolutePath();
        int migrated = 0;
        try (Connection sqliteConn = DriverManager.getConnection(sqliteUrl);
             PreparedStatement select = sqliteConn.prepareStatement("SELECT uuid, name, balance FROM pnmoney_balances");
             ResultSet rs = select.executeQuery()) {
            while (rs.next()) {
                String uuid = rs.getString("uuid");
                String name = rs.getString("name");
                BigDecimal balance = rs.getBigDecimal("balance");
                setBalance(UUID.fromString(uuid), name, balance, 8);
                migrated++;
            }
            plugin.getLogger().info("SQLite -> MySQL migration complete, total records: " + migrated);
        } catch (Exception ex) {
            plugin.getLogger().warning("Migration skipped/failed: " + ex.getMessage());
        }
    }

    public synchronized BigDecimal getOrCreate(UUID uuid, String name, BigDecimal defaultBalance, int scale) {
        BigDecimal current = getBalance(uuid);
        if (current != null) {
            if (name != null) {
                updateName(uuid, name);
            }
            return current.setScale(scale, BigDecimal.ROUND_DOWN);
        }
        setBalance(uuid, name, defaultBalance, scale);
        return defaultBalance;
    }

    public synchronized BigDecimal getBalance(UUID uuid) {
        String sql = "SELECT balance FROM pnmoney_balances WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("balance");
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Query balance failed: " + ex.getMessage());
        }
        return null;
    }

    private synchronized void updateName(UUID uuid, String name) {
        String sql = "UPDATE pnmoney_balances SET name = ? WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name == null ? "unknown" : name);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().warning("Update name failed: " + ex.getMessage());
        }
    }

    public synchronized boolean setBalance(UUID uuid, String name, BigDecimal balance, int scale) {
        String sql = "INSERT INTO pnmoney_balances(uuid, name, balance) VALUES(?,?,?) " +
                "ON DUPLICATE KEY UPDATE name=VALUES(name), balance=VALUES(balance)";

        if (storageType == StorageType.SQLITE) {
            sql = "INSERT INTO pnmoney_balances(uuid, name, balance) VALUES(?,?,?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET name=excluded.name, balance=excluded.balance";
        }

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name == null ? "unknown" : name);
            statement.setBigDecimal(3, balance.setScale(scale, BigDecimal.ROUND_DOWN));
            statement.executeUpdate();
            return true;
        } catch (SQLException ex) {
            plugin.getLogger().warning("Set balance failed: " + ex.getMessage());
            return false;
        }
    }

    public synchronized boolean transfer(UUID fromId, String fromName, UUID toId, String toName, BigDecimal amount,
                                         BigDecimal defaultBalance, boolean allowNegative,
                                         BigDecimal maxBalance, int scale) {
        try {
            connection.setAutoCommit(false);

            BigDecimal fromBal = getOrCreate(fromId, fromName, defaultBalance, scale);
            BigDecimal toBal = getOrCreate(toId, toName, defaultBalance, scale);

            BigDecimal fromNext = fromBal.subtract(amount);
            BigDecimal toNext = toBal.add(amount);

            if (!allowNegative && fromNext.compareTo(BigDecimal.ZERO) < 0) {
                connection.rollback();
                connection.setAutoCommit(true);
                return false;
            }
            if (toNext.compareTo(maxBalance) > 0) {
                connection.rollback();
                connection.setAutoCommit(true);
                return false;
            }

            setBalance(fromId, fromName, fromNext, scale);
            setBalance(toId, toName, toNext, scale);
            connection.commit();
            connection.setAutoCommit(true);
            return true;
        } catch (Exception ex) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
            plugin.getLogger().warning("Transfer failed: " + ex.getMessage());
            return false;
        }
    }

    public synchronized List<PlayerBalance> getTop(int limit) {
        List<PlayerBalance> list = new ArrayList<>();
        String sql = "SELECT name, balance FROM pnmoney_balances ORDER BY balance DESC LIMIT ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    list.add(new PlayerBalance(rs.getString("name"), rs.getBigDecimal("balance")));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Top query failed: " + ex.getMessage());
        }
        return list;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
