package uno.mcme.pnmoneyshop;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import uno.mcme.pnmoneyshop.command.PnMoneyShopCommand;
import uno.mcme.pnmoneyshop.shop.ShopManager;

public class PnMoneyShopPlugin extends JavaPlugin {

    private ShopManager shopManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("shop.yml", false);

        shopManager = new ShopManager(this);
        shopManager.reload();

        PnMoneyShopCommand command = new PnMoneyShopCommand(this, shopManager);
        PluginCommand pluginCommand = getCommand("pnms");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
    }

    public void reloadEverything() {
        reloadConfig();
        shopManager.reload();
    }
}
