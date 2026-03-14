package uno.mcme.pnmoney.api;

import org.bukkit.OfflinePlayer;
import uno.mcme.pnmoney.MoneyManager;

import java.math.BigDecimal;

public class MoneyServiceImpl implements MoneyService {

    private final MoneyManager moneyManager;

    public MoneyServiceImpl(MoneyManager moneyManager) {
        this.moneyManager = moneyManager;
    }

    @Override
    public BigDecimal getBalance(OfflinePlayer player) {
        return moneyManager.getBalance(player);
    }

    @Override
    public boolean setBalance(OfflinePlayer player, BigDecimal amount) {
        return moneyManager.setBalance(player, amount);
    }

    @Override
    public boolean addBalance(OfflinePlayer player, BigDecimal amount) {
        return moneyManager.addBalance(player, amount);
    }

    @Override
    public boolean takeBalance(OfflinePlayer player, BigDecimal amount) {
        return moneyManager.takeBalance(player, amount);
    }
}
