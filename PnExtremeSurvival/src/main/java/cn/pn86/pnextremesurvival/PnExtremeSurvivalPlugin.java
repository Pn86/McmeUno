package cn.pn86.pnextremesurvival;

import cn.pn86.pnextremesurvival.command.PnExtremeSurvivalCommand;
import cn.pn86.pnextremesurvival.data.PlayerDataRepository;
import cn.pn86.pnextremesurvival.listener.PlayerConnectionListener;
import cn.pn86.pnextremesurvival.listener.PlayerDeathListener;
import cn.pn86.pnextremesurvival.service.LimitedLifeService;
import cn.pn86.pnextremesurvival.service.LootChestService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class PnExtremeSurvivalPlugin extends JavaPlugin {

    private PlayerDataRepository repository;
    private LimitedLifeService limitedLifeService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.repository = new PlayerDataRepository(this);
        try {
            this.repository.init();
        } catch (IllegalStateException ex) {
            getLogger().severe("Database initialization failed: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.limitedLifeService = new LimitedLifeService(this, repository);
        LootChestService lootChestService = new LootChestService(this);

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(limitedLifeService), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this, limitedLifeService, lootChestService), this);

        PluginCommand command = getCommand("pnes");
        if (command != null) {
            PnExtremeSurvivalCommand handler = new PnExtremeSurvivalCommand(this, limitedLifeService);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }

        limitedLifeService.loadOnlinePlayers();
    }

    @Override
    public void onDisable() {
        if (repository != null) {
            repository.close();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
    }
}
