package uno.mcme.pnoremine;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import uno.mcme.pnoremine.command.PnOreMineCommand;
import uno.mcme.pnoremine.config.ConfigService;
import uno.mcme.pnoremine.config.ConfigValidationException;
import uno.mcme.pnoremine.listener.WorldProtectListener;
import uno.mcme.pnoremine.listener.MineBreakListener;
import uno.mcme.pnoremine.mine.MineManager;
import uno.mcme.pnoremine.mine.MineRegion;
import uno.mcme.pnoremine.placeholder.PnOreMinePlaceholder;
import uno.mcme.pnoremine.util.ColorUtil;

import java.io.File;
import java.io.IOException;

public class PnOreMinePlugin extends JavaPlugin {

    private final MineManager mineManager = new MineManager();
    private final ConfigService configService = new ConfigService();
    private FileConfiguration messages;
    private Economy economy;
    private BukkitTask timerTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!setupEconomy()) {
            getLogger().severe("未找到 Vault 经济服务，插件已禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!loadMineConfigWithRecovery()) {
            getLogger().severe("配置加载失败且无可恢复备份，插件已禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        registerCommand();
        getServer().getPluginManager().registerEvents(new MineBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldProtectListener(this), this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PnOreMinePlaceholder(this).register();
        }

        startTimer();
        getLogger().info("PnOreMine 已启用。");
    }

    @Override
    public void onDisable() {
        if (timerTask != null) {
            timerTask.cancel();
        }
    }

    public boolean reloadMinePlugin() {
        reloadConfig();
        return loadMineConfigWithRecovery();
    }

    private boolean loadMineConfigWithRecovery() {
        File configFile = new File(getDataFolder(), "config.yml");
        File backupFile = new File(getDataFolder(), "config-lastgood.yml");

        try {
            ConfigService.LoadResult result = configService.load(configFile);
            this.messages = result.configuration();
            mineManager.replaceAll(result.mines());
            for (MineRegion mine : mineManager.getMines()) {
                mine.reset();
            }
            configService.backup(configFile, backupFile);
            return true;
        } catch (Exception ex) {
            getLogger().severe("配置错误: " + ex.getMessage());
            if (backupFile.exists()) {
                try {
                    ConfigService.LoadResult backup = configService.load(backupFile);
                    this.messages = backup.configuration();
                    mineManager.replaceAll(backup.mines());
                    notifyAdmins(msg("config-invalid"));
                    getLogger().warning("已恢复为上一份可用配置 config-lastgood.yml");
                    return true;
                } catch (ConfigValidationException backupEx) {
                    getLogger().severe("恢复备份失败: " + backupEx.getMessage());
                    return false;
                }
            }
            return false;
        }
    }

    private void registerCommand() {
        PluginCommand command = getCommand("pnom");
        if (command == null) {
            throw new IllegalStateException("pnom command not found in plugin.yml");
        }
        CommandExecutor executor = new PnOreMineCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter((sender, cmd, label, args) -> {
            if (args.length == 1) {
                return java.util.List.of("reload", "list", "see", "reset");
            }
            if (args.length == 2 && (args[0].equalsIgnoreCase("see") || args[0].equalsIgnoreCase("reset"))) {
                return mineManager.getMines().stream().map(MineRegion::getName).toList();
            }
            return java.util.Collections.emptyList();
        });
    }

    private void startTimer() {
        if (timerTask != null) {
            timerTask.cancel();
        }
        timerTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (MineRegion mine : mineManager.getMines()) {
                mine.tickAndMaybeReset();
            }
        }, 20L, 20L);
    }

    public MineManager getMineManager() {
        return mineManager;
    }

    public Economy getEconomy() {
        return economy;
    }

    public String msg(String key) {
        String path = "language." + key;
        return ColorUtil.color(messages.getString(path, ""));
    }

    public String getPrefix() {
        return msg("prefix");
    }

    public void notifyAdmins(String message) {
        String full = getPrefix() + message;
        Bukkit.getConsoleSender().sendMessage(full);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("pnoremine.admin")) {
                player.sendMessage(full);
            }
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }
}
