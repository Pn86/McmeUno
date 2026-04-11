package uno.mcme.pnmoney.api;

import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;

public interface MoneyService {

    BigDecimal getBalance(OfflinePlayer player);

    boolean setBalance(OfflinePlayer player, BigDecimal amount);

    boolean addBalance(OfflinePlayer player, BigDecimal amount);

    boolean takeBalance(OfflinePlayer player, BigDecimal amount);
}
