package uno.mcme.pnoremine;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import uno.mcme.pnoremine.command.PnOreMineCommand;
import uno.mcme.pnoremine.config.ConfigService;
import uno.mcme.pnoremine.config.ConfigValidationException;
import uno.mcme.pnoremine.listener.MineBreakListener;
import uno.mcme.pnoremine.listener.WorldProtectListener;
import uno.mcme.pnoremine.mine.MineManager;
import uno.mcme.pnoremine.mine.MineRegion;
import uno.mcme.pnoremine.placeholder.PnOreMinePlaceholder;
import uno.mcme.pnoremine.util.ColorUtil;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PnOreMinePlugin extends JavaPlugin {

    private static final Pattern DISPLAY_TAG_PATTERN = Pattern.compile("^(?:\\[(message|actionbar|title|subtitle)])+", Pattern.CASE_INSENSITIVE);

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

    public void resetMineWithSafety(MineRegion mine, boolean broadcast) {
        teleportMineWorldPlayersToSurface(mine.getWorldName());
        mine.reset();
        if (broadcast) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("mine", mine.getName());
            broadcastLocalized("mine-reset-broadcast", placeholders,
                "[message]&e矿场 &6%mine% &e已刷新。");
        }
    }

    private void teleportMineWorldPlayersToSurface(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return;
        }
        for (Player player : world.getPlayers()) {
            Location from = player.getLocation();
            int topY = world.getHighestBlockYAt(from.getBlockX(), from.getBlockZ()) + 1;
            Location to = new Location(world, from.getBlockX() + 0.5, topY, from.getBlockZ() + 0.5, from.getYaw(), from.getPitch());
            player.teleport(to);
        }
    }

    private boolean loadMineConfigWithRecovery() {
        File configFile = new File(getDataFolder(), "config.yml");
        File backupFile = new File(getDataFolder(), "config-lastgood.yml");

        try {
            ConfigService.LoadResult result = configService.load(configFile);
            this.messages = result.configuration();
            mineManager.replaceAll(result.mines());
            for (MineRegion mine : mineManager.getMines()) {
                resetMineWithSafety(mine, false);
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
                    notifyAdmins("config-invalid", Collections.emptyMap());
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
                if (mine.getRemainingSeconds() == 10) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("mine", mine.getName());
                    broadcastLocalized("mine-warn-10", placeholders,
                        "[message]&e矿场 &6%mine% &e将在 &c10秒&e后刷新。请注意离开矿区。");
                }
                mine.tick();
                if (mine.isReadyToReset()) {
                    resetMineWithSafety(mine, true);
                }
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
        return msg(key, Collections.emptyMap(), "");
    }

    public String msg(String key, Map<String, String> placeholders, String fallback) {
        String path = "language." + key;
        String template = messages.getString(path, fallback);
        if (template == null) {
            template = fallback;
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            template = template.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return ColorUtil.color(template);
    }

    public void sendLocalized(CommandSender target, String key, Map<String, String> placeholders, String fallback) {
        deliverLocalized(java.util.List.of(target), key, placeholders, fallback);
    }

    public void notifyAdmins(String key, Map<String, String> placeholders) {
        Bukkit.getConsoleSender().sendMessage(getPrefix() + stripDisplayTags(msg(key, placeholders, "")));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("pnoremine.admin")) {
                deliverLocalized(java.util.List.of(player), key, placeholders, "");
            }
        }
    }

    public void broadcastLocalized(String key, Map<String, String> placeholders, String fallback) {
        deliverLocalized(Bukkit.getOnlinePlayers(), key, placeholders, fallback);
        Bukkit.getConsoleSender().sendMessage(getPrefix() + stripDisplayTags(msg(key, placeholders, fallback)));
    }

    private void deliverLocalized(Collection<? extends CommandSender> targets, String key, Map<String, String> placeholders, String fallback) {
        String raw = msg(key, placeholders, fallback);
        DisplaySpec spec = parseDisplay(raw);

        for (CommandSender target : targets) {
            if (spec.message && !spec.content.isBlank()) {
                target.sendMessage(getPrefix() + spec.content);
            }
            if (target instanceof Player player) {
                if (spec.actionbar && !spec.content.isBlank()) {
                    player.sendActionBar(spec.content);
                }
                if (spec.title || spec.subtitle) {
                    String title = spec.title ? spec.titleText : "";
                    String subtitle = spec.subtitle ? spec.subtitleText : "";
                    player.sendTitle(title, subtitle, 10, 50, 10);
                }
            }
        }
    }

    private DisplaySpec parseDisplay(String input) {
        Matcher matcher = DISPLAY_TAG_PATTERN.matcher(input);
        if (!matcher.find()) {
            return new DisplaySpec(true, false, false, false, input, input, "");
        }

        String prefix = matcher.group();
        String content = input.substring(prefix.length()).trim();
        boolean message = prefix.toLowerCase().contains("[message]");
        boolean actionbar = prefix.toLowerCase().contains("[actionbar]");
        boolean title = prefix.toLowerCase().contains("[title]");
        boolean subtitle = prefix.toLowerCase().contains("[subtitle]");

        if (!message && !actionbar && !title && !subtitle) {
            message = true;
        }

        String titleText = content;
        String subtitleText = "";
        if (content.contains("||")) {
            String[] split = content.split("\\|\\|", 2);
            titleText = split[0];
            subtitleText = split[1];
        }
        return new DisplaySpec(message, actionbar, title, subtitle, content, titleText, subtitleText);
    }

    private String stripDisplayTags(String input) {
        Matcher matcher = DISPLAY_TAG_PATTERN.matcher(input);
        if (matcher.find()) {
            return input.substring(matcher.group().length()).trim();
        }
        return input;
    }

    public String getPrefix() {
        return msg("prefix");
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

    private record DisplaySpec(boolean message, boolean actionbar, boolean title, boolean subtitle,
                               String content, String titleText, String subtitleText) {
    }
}
