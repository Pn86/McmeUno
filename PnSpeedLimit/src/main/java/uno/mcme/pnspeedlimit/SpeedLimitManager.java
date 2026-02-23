package uno.mcme.pnspeedlimit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import uno.mcme.pnspeedlimit.model.SpeedType;

import java.util.*;

public class SpeedLimitManager {

    private final PnSpeedLimitPlugin plugin;

    private final Map<UUID, Double> lastSpeed = new HashMap<>();
    private final Map<UUID, SpeedType> lastType = new HashMap<>();
    private final Map<UUID, Long> lastWarnTime = new HashMap<>();

    private final Set<String> whitelist = new HashSet<>();

    private boolean enabled;
    private double limitAll;
    private double limitMove;
    private double limitFly;
    private double limitRepel;

    private double warningPercent;
    private long warningCooldownMillis;
    private List<String> warningMessages;
    private List<String> punishCommands;

    public SpeedLimitManager(PnSpeedLimitPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        enabled = config.getBoolean("settings.enabled", true);
        limitAll = config.getDouble("limits.all", 18.0D);
        limitMove = config.getDouble("limits.move", 10.0D);
        limitFly = config.getDouble("limits.fly", 28.0D);
        limitRepel = config.getDouble("limits.repel", 22.0D);

        warningPercent = Math.max(0.01D, config.getDouble("warning.percent", 0.8D));
        warningCooldownMillis = Math.max(0L, config.getLong("warning.cooldown-ms", 3000L));
        warningMessages = config.getStringList("warning.messages");
        punishCommands = config.getStringList("punishment.commands");

        whitelist.clear();
        for (String name : config.getStringList("whitelist")) {
            whitelist.add(name.toLowerCase(Locale.ROOT));
        }

        clearRuntimeCache();
    }

    public void save() {
        FileConfiguration config = plugin.getConfig();
        config.set("settings.enabled", enabled);
        config.set("limits.all", limitAll);
        config.set("limits.move", limitMove);
        config.set("limits.fly", limitFly);
        config.set("limits.repel", limitRepel);

        List<String> names = new ArrayList<>(whitelist);
        names.sort(String::compareToIgnoreCase);
        config.set("whitelist", names);

        plugin.saveConfig();
    }

    public void clearRuntimeCache() {
        lastSpeed.clear();
        lastType.clear();
        lastWarnTime.clear();
    }

    public boolean isWhitelisted(Player player) {
        return whitelist.contains(player.getName().toLowerCase(Locale.ROOT));
    }

    public boolean addWhitelist(String name) {
        return whitelist.add(name.toLowerCase(Locale.ROOT));
    }

    public boolean removeWhitelist(String name) {
        return whitelist.remove(name.toLowerCase(Locale.ROOT));
    }

    public Set<String> getWhitelist() {
        return Collections.unmodifiableSet(whitelist);
    }

    public double getSpeedLimit(SpeedType type) {
        double specific;
        if (type == SpeedType.MOVE) {
            specific = limitMove;
        } else if (type == SpeedType.FLY) {
            specific = limitFly;
        } else {
            specific = limitRepel;
        }
        return Math.min(limitAll, specific);
    }

    public double getRawLimitAll() {
        return limitAll;
    }

    public double getRawLimitMove() {
        return limitMove;
    }

    public double getRawLimitFly() {
        return limitFly;
    }

    public double getRawLimitRepel() {
        return limitRepel;
    }

    public void setLimitAll(double value) {
        this.limitAll = Math.max(0.1D, value);
    }

    public void setLimitMove(double value) {
        this.limitMove = Math.max(0.1D, value);
    }

    public void setLimitFly(double value) {
        this.limitFly = Math.max(0.1D, value);
    }

    public void setLimitRepel(double value) {
        this.limitRepel = Math.max(0.1D, value);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void updatePlayerSpeed(Player player, double speed, SpeedType type) {
        lastSpeed.put(player.getUniqueId(), speed);
        lastType.put(player.getUniqueId(), type);
    }

    public double getLastSpeed(Player player) {
        return lastSpeed.getOrDefault(player.getUniqueId(), 0.0D);
    }

    public SpeedType getLastType(Player player) {
        return lastType.getOrDefault(player.getUniqueId(), SpeedType.MOVE);
    }

    public void tryWarning(Player player, double speed, SpeedType type) {
        double limit = getSpeedLimit(type);
        if (speed < limit * warningPercent) {
            return;
        }

        long now = System.currentTimeMillis();
        long last = lastWarnTime.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < warningCooldownMillis) {
            return;
        }
        lastWarnTime.put(player.getUniqueId(), now);

        for (String line : warningMessages) {
            player.sendMessage(colorize(applyPlaceholders(line, player, speed, type)));
        }
    }

    public void punish(Player player, double speed, SpeedType type) {
        for (String cmd : punishCommands) {
            String parsed = applyPlaceholders(cmd, player, speed, type);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }

    public String applyPlaceholders(String input, Player player, double speed, SpeedType type) {
        return input
                .replace("%player%", player.getName())
                .replace("%speed%", formatSpeed(speed, true))
                .replace("%speednum%", formatSpeed(speed, false))
                .replace("%limit%", formatSpeed(getSpeedLimit(type), false))
                .replace("%type%", type.name().toLowerCase(Locale.ROOT));
    }

    public String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public String formatSpeed(double speed, boolean withUnit) {
        String formatted = String.format(Locale.US, "%.2f", speed);
        return withUnit ? formatted + "m/s" : formatted;
    }
}
