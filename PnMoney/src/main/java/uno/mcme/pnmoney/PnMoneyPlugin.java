package uno.mcme.pnmoney;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import uno.mcme.pnmoney.api.MoneyService;
import uno.mcme.pnmoney.api.MoneyServiceImpl;
import uno.mcme.pnmoney.command.PnMoneyCommand;
import uno.mcme.pnmoney.data.DatabaseManager;
import uno.mcme.pnmoney.shop.ShopManager;

public class PnMoneyPlugin extends JavaPlugin {

    private DatabaseManager databaseManager;
    private MoneyManager moneyManager;
    private ShopManager shopManager;
    private MoneyService moneyService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("shop.yml", false);

        databaseManager = new DatabaseManager(this);
        databaseManager.start();

        moneyManager = new MoneyManager(this, databaseManager);
        shopManager = new ShopManager(this);
        moneyService = new MoneyServiceImpl(moneyManager);

        PnMoneyCommand command = new PnMoneyCommand(this, moneyManager, shopManager);
        PluginCommand pnmy = getCommand("pnmy");
        if (pnmy != null) {
            pnmy.setExecutor(command);
            pnmy.setTabCompleter(command);
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PnMoneyPlaceholder(this, moneyManager).register();
            getLogger().info("PlaceholderAPI detected, placeholders registered.");
        }
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    public void reloadEverything() {
        reloadConfig();
        shopManager.reload();
        databaseManager.reload();
    }

    public MoneyService getMoneyService() {
        return moneyService;
    }
}
