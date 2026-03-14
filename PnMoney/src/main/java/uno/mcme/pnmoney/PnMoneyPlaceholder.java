package uno.mcme.pnmoney;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PnMoneyPlaceholder extends PlaceholderExpansion {

    private final PnMoneyPlugin plugin;
    private final MoneyManager moneyManager;

    public PnMoneyPlaceholder(PnMoneyPlugin plugin, MoneyManager moneyManager) {
        this.plugin = plugin;
        this.moneyManager = moneyManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "pnmoney";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Pn86";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if ("money".equalsIgnoreCase(params)) {
            return moneyManager.getCurrencyName();
        }
        if ("bal".equalsIgnoreCase(params)) {
            if (player == null) {
                return "0";
            }
            return moneyManager.getBalance(player).stripTrailingZeros().toPlainString();
        }
        return null;
    }
}
