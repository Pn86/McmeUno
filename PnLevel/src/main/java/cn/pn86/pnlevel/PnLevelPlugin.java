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
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class PnLevelPlugin extends JavaPlugin {
    private FileConfiguration langConfig;
    private FileConfiguration expConfig;
    private FileConfiguration giftConfig;
    private FileConfiguration guiConfig;

    private PlayerDataManager playerDataManager;
    private LevelManager levelManager;
    private ExpManager expManager;
    private GiftManager giftManager;
    private GuiManager guiManager;
    private int lastReloadWarningCount;
    private PnLevelExpansion expansion;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("lang.yml", false);
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

        logPapiDiagnostics("onEnable");
        Bukkit.getScheduler().runTask(this, this::registerPapiExpansion);
        Bukkit.getScheduler().runTaskLater(this, () -> logPapiDiagnostics("post-register"), 20L);
        for (Player player : Bukkit.getOnlinePlayers()) {
            expManager.onJoin(player);
            PlayerLevelData data = playerDataManager.getOrCreate(player.getUniqueId(), player.getName());
            data.setLastName(player.getName());
            levelManager.processLevelUps(data, player, false);
        }
    }

    @Override
    public void onDisable() {
        if (expansion != null) {
            try {
                expansion.unregister();
            } catch (Exception ignored) {
            }
        }
        if (playerDataManager != null) {
            playerDataManager.save();
        }
    }

    public void reloadAll() {
        lastReloadWarningCount = 0;
        reloadConfig();

        langConfig = loadYamlSafely("lang.yml", langConfig);
        expConfig = loadYamlSafely("exp.yml", expConfig);
        giftConfig = loadYamlSafely("gift.yml", giftConfig);
        guiConfig = loadYamlSafely("gui.yml", guiConfig);

        if (expManager != null) {
            expManager.loadRules();
        }

        Bukkit.getScheduler().runTask(this, this::registerPapiExpansion);
        Bukkit.getScheduler().runTaskLater(this, () -> logPapiDiagnostics("post-reload"), 20L);
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


    private void registerPapiExpansion() {
        org.bukkit.plugin.Plugin papi = getServer().getPluginManager().getPlugin("PlaceholderAPI");
        if (papi == null) {
            getLogger().warning("[PAPI] PlaceholderAPI not found, placeholders are disabled.");
            return;
        }
        getLogger().info("[PAPI] PlaceholderAPI detected: " + papi.getDescription().getVersion());
        try {
            if (expansion != null) {
                expansion.unregister();
                getLogger().info("[PAPI] Unregistered previous expansion instance.");
            }
            expansion = new PnLevelExpansion(this);
            boolean ok = expansion.register();
            if (ok) {
                getLogger().info("[PAPI] PnLevel expansion registered successfully.");
                getLogger().info("[PAPI] Example placeholders: %pnlevel_level%, %pnlevel_exp%, %pnlevel_top.1%");
            } else {
                getLogger().warning("[PAPI] PnLevel expansion register returned false.");
            }
        } catch (Throwable throwable) {
            getLogger().warning("[PAPI] Failed to register expansion: " + throwable.getMessage());
        }
    }

    private void logPapiDiagnostics(String stage) {
        org.bukkit.plugin.Plugin papi = getServer().getPluginManager().getPlugin("PlaceholderAPI");
        String papiState = papi == null ? "missing" : (papi.isEnabled() ? "enabled" : "disabled");
        getLogger().info("[PAPI] Diagnostics(" + stage + "): PlaceholderAPI=" + papiState
                + ", expansionInstance=" + (expansion == null ? "null" : "present")
                + ", sample=" + samplePlaceholderCheck() + ".");
    }

    private String samplePlaceholderCheck() {
        Player player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (player == null) {
            return "no-online-player";
        }
        return applyPapi(player, "%pnlevel_level%");
    }

    public PlayerLevelData findPlayerData(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return playerDataManager.getOrCreate(online.getUniqueId(), online.getName());
        }
        return playerDataManager.getByName(name);
    }

    public String msg(String key) {
        String modern = langConfig.getString("messages." + key + ".message.text");
        if (modern != null) return modern;
        return langConfig.getString("messages." + key, "");
    }

    public void sendLang(CommandSender sender, String key, String... placeholders) {
        ConfigurationSection sec = langConfig.getConfigurationSection("messages." + key);
        if (sec == null) {
            String legacy = langConfig.getString("messages." + key, "");
            legacy = applyPlaceholders(legacy, placeholders);
            if (!legacy.isBlank()) sender.sendMessage(ColorUtil.component(legacy));
            return;
        }

        ConfigurationSection msg = sec.getConfigurationSection("message");
        if (msg == null || msg.getBoolean("enable", true)) {
            String text = msg == null ? "" : msg.getString("text", "");
            text = applyPlaceholders(text, placeholders);
            if (!text.isBlank()) sender.sendMessage(ColorUtil.component(text));
        }

        if (sender instanceof Player player) {
            ConfigurationSection title = sec.getConfigurationSection("title");
            ConfigurationSection subtitle = sec.getConfigurationSection("subtitle");
            boolean titleOn = title != null && title.getBoolean("enable", false);
            boolean subOn = subtitle != null && subtitle.getBoolean("enable", false);
            if (titleOn || subOn) {
                String t = titleOn ? applyPlaceholders(title.getString("text", ""), placeholders) : "";
                String st = subOn ? applyPlaceholders(subtitle.getString("text", ""), placeholders) : "";
                player.showTitle(net.kyori.adventure.title.Title.title(ColorUtil.component(t), ColorUtil.component(st)));
            }
        }
    }

    private String applyPlaceholders(String text, String... placeholders) {
        String out = text == null ? "" : text;
        if (placeholders == null) return out;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            out = out.replace(placeholders[i], placeholders[i + 1]);
        }
        return out;
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

    public FileConfiguration getLangConfig() { return langConfig; }

    public FileConfiguration getMessageConfig() { return langConfig; }

    public FileConfiguration getExpConfig() { return expConfig; }

    public FileConfiguration getGiftConfig() { return giftConfig; }

    public FileConfiguration getGuiConfig() { return guiConfig; }

    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }

    public LevelManager getLevelManager() { return levelManager; }

    public ExpManager getExpManager() { return expManager; }

    public GiftManager getGiftManager() { return giftManager; }

    public GuiManager getGuiManager() { return guiManager; }
}
