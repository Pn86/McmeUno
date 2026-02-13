package cn.pn86.pnvipcommand;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class PnVIPCommandV1Plugin extends JavaPlugin {

    private CooldownManager cooldownManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadPluginConfig();
        registerCommands();
        getLogger().info("PnVIPCommandV1 已启用");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        this.cooldownManager = new CooldownManager(getConfig());
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    public void sendConfigMessage(org.bukkit.command.CommandSender sender, String key) {
        sender.sendMessage(color(getConfig().getString("messages." + key, "&c未配置消息: " + key)));
    }

    public void sendConfigMessage(org.bukkit.command.CommandSender sender, String key, String placeholder, String value) {
        String msg = getConfig().getString("messages." + key, "&c未配置消息: " + key);
        msg = msg.replace(placeholder, value);
        sender.sendMessage(color(msg));
    }

    public List<String> getStringList(String path) {
        return getConfig().getStringList(path);
    }

    private void registerCommands() {
        PnVipCommandExecutor executor = new PnVipCommandExecutor(this);
        tabExecutor("pnvc", executor);
        tabExecutor("ptp", executor);
        tabExecutor("pgive", executor);
        tabExecutor("ptime", executor);
        tabExecutor("pweather", executor);
        tabExecutor("pexp", executor);
    }

    private void tabExecutor(String commandName, PnVipCommandExecutor executor) {
        PluginCommand cmd = Bukkit.getPluginCommand(commandName);
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        } else {
            getLogger().warning("命令未在 plugin.yml 中注册: " + commandName);
        }
    }
}
