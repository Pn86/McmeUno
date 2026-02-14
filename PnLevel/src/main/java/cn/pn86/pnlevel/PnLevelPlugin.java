package cn.pn86.pnlevel;

import cn.pn86.pnlevel.command.PlayerCommands;
import cn.pn86.pnlevel.command.PnLevelCommand;
import cn.pn86.pnlevel.data.PlayerDataManager;
import cn.pn86.pnlevel.data.PlayerLevelData;
import cn.pn86.pnlevel.exp.ExpManager;
import cn.pn86.pnlevel.gift.GiftManager;
import cn.pn86.pnlevel.gui.GuiManager;
import cn.pn86.pnlevel.papi.PnLevelExpansion;
import cn.pn86.pnlevel.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

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
    private int lastReloadWarningCount;

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
            try {
                new PnLevelExpansion(this).register();
                getLogger().info("PnLevel PlaceholderAPI expansion registered.");
            } catch (Throwable throwable) {
                getLogger().warning("Failed to register PlaceholderAPI expansion: " + throwable.getMessage());
            }
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
        lastReloadWarningCount = 0;
        reloadConfig();

        messageConfig = loadYamlSafely("message.yml", messageConfig);
        expConfig = loadYamlSafely("exp.yml", expConfig);
        giftConfig = loadYamlSafely("gift.yml", giftConfig);
        guiConfig = loadYamlSafely("gui.yml", guiConfig);

        if (expManager != null) {
            expManager.loadRules();
        }
    }

    private FileConfiguration loadYamlSafely(String fileName, FileConfiguration current) {
        File file = new File(getDataFolder(), fileName);
        if (!file.exists()) {
            reportConfigWarning(fileName, "文件不存在，已跳过，继续使用旧配置。");
            return current == null ? new YamlConfiguration() : current;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
            return yaml;
        } catch (IOException | InvalidConfigurationException ex) {
            reportConfigWarning(fileName, "读取失败：" + ex.getMessage() + "；已跳过该文件并继续使用旧配置。");
            return current == null ? new YamlConfiguration() : current;
        } catch (Exception ex) {
            reportConfigWarning(fileName, "未知错误：" + ex.getMessage() + "；已跳过该文件并继续使用旧配置。");
            return current == null ? new YamlConfiguration() : current;
        }
    }

    private void reportConfigWarning(String fileName, String reason) {
        lastReloadWarningCount++;
        String msg = "&c[PnLevel] 配置文件 " + fileName + " 出错: " + reason;
        getLogger().warning("[PnLevel] Config error in " + fileName + ": " + reason);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("pnlevel.admin") || online.isOp()) {
                online.sendMessage(ColorUtil.component(msg));
            }
        }
    }

    public int getLastReloadWarningCount() {
        return lastReloadWarningCount;
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

    public String applyPapi(org.bukkit.OfflinePlayer player, String text) {
        if (text == null || getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return text == null ? "" : text;
        }
        try {
            Class<?> clazz = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            java.lang.reflect.Method method = clazz.getMethod("setPlaceholders", org.bukkit.OfflinePlayer.class, String.class);
            Object result = method.invoke(null, player, text);
            return result == null ? text : result.toString();
        } catch (Exception ex) {
            return text;
        }
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
