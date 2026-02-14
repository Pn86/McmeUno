package cn.pn86.pnlevel;

import cn.pn86.pnlevel.command.PlayerCommands;
import cn.pn86.pnlevel.command.PnLevelCommand;
import cn.pn86.pnlevel.data.PlayerDataManager;
import cn.pn86.pnlevel.data.PlayerLevelData;
import cn.pn86.pnlevel.exp.ExpManager;
import cn.pn86.pnlevel.gift.GiftManager;
import cn.pn86.pnlevel.gui.GuiManager;
import cn.pn86.pnlevel.papi.PnLevelExpansion;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class PnLevelPlugin extends JavaPlugin {
    private FileConfiguration messageConfig;
    private FileConfiguration expConfig;
    private FileConfiguration giftConfig;
    private FileConfiguration guiConfig;

    private PlayerDataManager playerDataManager;
    private LevelManager levelManager;
    private ExpManager expManager;
    private GiftManager giftManager;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("message.yml", false);
        saveResource("exp.yml", false);
        saveResource("gift.yml", false);
        saveResource("gui.yml", false);
        reloadAll();

        this.playerDataManager = new PlayerDataManager(this);
        playerDataManager.load();
        this.levelManager = new LevelManager(this);
        this.giftManager = new GiftManager(this);
        this.expManager = new ExpManager(this);
        expManager.loadRules();
        this.guiManager = new GuiManager(this);

        PnLevelCommand admin = new PnLevelCommand(this);
        getCommand("pnlv").setExecutor(admin);
        getCommand("pnlv").setTabCompleter(admin);
        PlayerCommands pc = new PlayerCommands(this);
        getCommand("level").setExecutor(pc);
        getCommand("leveltop").setExecutor(pc);
        getCommand("levelgift").setExecutor(pc);

        getServer().getPluginManager().registerEvents(new PnLevelListener(this), this);
        Bukkit.getScheduler().runTaskTimer(this, () -> expManager.tickTimeRules(), 20L, 20L);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PnLevelExpansion(this).register();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            expManager.onJoin(player);
            playerDataManager.getOrCreate(player.getUniqueId(), player.getName()).setLastName(player.getName());
        }
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.save();
        }
    }

    public void reloadAll() {
        reloadConfig();
        this.messageConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "message.yml"));
        this.expConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "exp.yml"));
        this.giftConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "gift.yml"));
        this.guiConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "gui.yml"));
        if (expManager != null) {
            expManager.loadRules();
        }
    }

    public PlayerLevelData findPlayerData(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return playerDataManager.getOrCreate(online.getUniqueId(), online.getName());
        }
        return playerDataManager.getByName(name);
    }

    public String msg(String key) {
        return messageConfig.getString("messages." + key, "");
    }

    public int getMaxLevel() {
        return getConfig().getInt("max-level", 100);
    }

    public int getInitialLevel() {
        return getConfig().getInt("initial-level", 1);
    }

    public int getInitialExp() {
        return Math.min(999, getConfig().getInt("initial-exp", 0));
    }

    public FileConfiguration getMessageConfig() { return messageConfig; }

    public FileConfiguration getExpConfig() { return expConfig; }

    public FileConfiguration getGiftConfig() { return giftConfig; }

    public FileConfiguration getGuiConfig() { return guiConfig; }

    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }

    public LevelManager getLevelManager() { return levelManager; }

    public ExpManager getExpManager() { return expManager; }

    public GiftManager getGiftManager() { return giftManager; }

    public GuiManager getGuiManager() { return guiManager; }
}
