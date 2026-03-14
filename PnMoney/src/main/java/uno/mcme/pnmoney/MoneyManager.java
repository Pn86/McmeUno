package uno.mcme.pnmoney;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import uno.mcme.pnmoney.data.DatabaseManager;
import uno.mcme.pnmoney.data.PlayerBalance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

public class MoneyManager {

    private final PnMoneyPlugin plugin;
    private final DatabaseManager databaseManager;

    public MoneyManager(PnMoneyPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public String getCurrencyName() {
        return plugin.getConfig().getString("currency.name", "金币");
    }

    public BigDecimal getDefaultBalance() {
        return normalize(BigDecimal.valueOf(plugin.getConfig().getDouble("money.default", 0.0)));
    }

    public BigDecimal getMaxBalance() {
        return normalize(BigDecimal.valueOf(plugin.getConfig().getDouble("money.max", 999999999.0)));
    }

    public boolean allowNegative() {
        return plugin.getConfig().getBoolean("money.allow-negative", false);
    }

    public int getScale() {
        return plugin.getConfig().getInt("money.max-decimal", 2);
    }

    public BigDecimal normalize(BigDecimal value) {
        return value.setScale(getScale(), RoundingMode.DOWN);
    }

    public BigDecimal clamp(BigDecimal value) {
        BigDecimal normalized = normalize(value);
        if (!allowNegative() && normalized.compareTo(BigDecimal.ZERO) < 0) {
            normalized = BigDecimal.ZERO;
        }
        if (normalized.compareTo(getMaxBalance()) > 0) {
            normalized = getMaxBalance();
        }
        return normalized;
    }

    public BigDecimal getBalance(OfflinePlayer player) {
        return databaseManager.getOrCreate(player.getUniqueId(), player.getName(), getDefaultBalance(), getScale());
    }

    public boolean setBalance(OfflinePlayer player, BigDecimal amount) {
        BigDecimal target = clamp(amount);
        return databaseManager.setBalance(player.getUniqueId(), player.getName(), target, getScale());
    }

    public boolean addBalance(OfflinePlayer player, BigDecimal amount) {
        BigDecimal now = getBalance(player);
        return setBalance(player, now.add(amount));
    }

    public boolean takeBalance(OfflinePlayer player, BigDecimal amount) {
        BigDecimal now = getBalance(player);
        BigDecimal target = now.subtract(amount);
        if (!allowNegative() && target.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        return setBalance(player, target);
    }

    public boolean hasEnough(OfflinePlayer player, BigDecimal amount) {
        BigDecimal normalized = normalize(amount);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return getBalance(player).compareTo(normalized) >= 0;
    }

    public boolean resetBalance(OfflinePlayer player) {
        return setBalance(player, getDefaultBalance());
    }

    public boolean transfer(OfflinePlayer from, OfflinePlayer to, BigDecimal amount) {
        BigDecimal normalized = normalize(amount);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        UUID fromId = from.getUniqueId();
        UUID toId = to.getUniqueId();
        return databaseManager.transfer(fromId, from.getName(), toId, to.getName(), normalized,
                getDefaultBalance(), allowNegative(), getMaxBalance(), getScale());
    }

    public List<PlayerBalance> getTop(int limit) {
        return databaseManager.getTop(limit);
    }

    public FileConfiguration getMessages() {
        return plugin.getConfig();
    }
}
