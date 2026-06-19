package uno.mcme.pnplayertask.task;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import uno.mcme.pnplayertask.util.Text;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerTaskManager {
    private static final Pattern CONDITION = Pattern.compile("^\\[(\\w+)]\\s+(.+?)\\s*(>=|<=|!=|=|>|<)\\s*(.+)$");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final JavaPlugin plugin;
    private final Map<String, PlayerTask> tasks = new LinkedHashMap<>();
    private File playerFile;
    private FileConfiguration playerConfig;

    public PlayerTaskManager(JavaPlugin plugin) { this.plugin = plugin; }

    public void load() {
        plugin.saveResource("task.yml", false); plugin.saveResource("gui.yml", false); plugin.saveResource("player.yml", false);
        playerFile = new File(plugin.getDataFolder(), "player.yml");
        playerConfig = YamlConfiguration.loadConfiguration(playerFile);
        tasks.clear();
        FileConfiguration taskConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "task.yml"));
        for (String id : taskConfig.getKeys(false)) {
            ConfigurationSection sec = taskConfig.getConfigurationSection(id);
            if (sec == null) continue;
            Material mat = Material.matchMaterial(sec.getString("item", "paper"));
            if (mat == null) mat = Material.PAPER;
            List<TaskCondition> conditions = new ArrayList<>();
            for (String raw : sec.getStringList("if")) {
                TaskCondition c = parseCondition(raw);
                if (c != null) conditions.add(c);
            }
            String actionKey = sec.isList("action") ? "action" : "aciton";
            tasks.put(id, new PlayerTask(
                    id,
                    mat,
                    sec.getString("name", id),
                    sec.getStringList("lore"),
                    conditions,
                    sec.getStringList(actionKey),
                    RefreshRule.parse(sec.getString("retime", "none")),
                    sec.getString("complete-title", ""),
                    sec.getString("complete-subtitle", ""),
                    sec.getString("complete-message", "")
            ));
        }
    }

    private TaskCondition parseCondition(String raw) {
        Matcher m = CONDITION.matcher(raw);
        if (!m.matches()) { plugin.getLogger().warning("无效任务条件: " + raw); return null; }
        try { return new TaskCondition(ConditionType.valueOf(m.group(1).toUpperCase(Locale.ROOT)), m.group(2).trim(), m.group(3), m.group(4).trim()); }
        catch (IllegalArgumentException e) { plugin.getLogger().warning("未知任务条件类型: " + raw); return null; }
    }

    public Collection<PlayerTask> getTasks() { return Collections.unmodifiableCollection(tasks.values()); }
    public PlayerTask getTask(String id) { return tasks.get(id); }

    public boolean isConditionComplete(Player player, String taskId) {
        PlayerTask task = tasks.get(taskId);
        return task != null && task.conditions().stream().allMatch(c -> c.matches(player, this));
    }

    public boolean isComplete(Player player, String taskId) { return isCompleted(player.getUniqueId(), taskId) || isConditionComplete(player, taskId); }
    public boolean isCompleted(UUID uuid, String taskId) { return playerConfig.getBoolean("players." + uuid + ".completed." + taskId, false); }
    public boolean isClaimed(UUID uuid, String taskId) { return playerConfig.getBoolean("players." + uuid + ".claimed." + taskId, false); }

    public void markCompleted(Player player, PlayerTask task) {
        String base = "players." + player.getUniqueId();
        playerConfig.set(base + ".name", player.getName());
        playerConfig.set(base + ".completed." + task.id(), true);
        playerConfig.set(base + ".completed-at." + task.id(), System.currentTimeMillis());
        if (task.refreshRule().refreshes()) playerConfig.set(base + ".refresh-at." + task.id(), task.refreshRule().nextAfter(System.currentTimeMillis(), ZoneId.systemDefault()));
        savePlayers();
    }

    public void setClaimed(UUID uuid, String name, String taskId) {
        playerConfig.set("players." + uuid + ".name", name);
        playerConfig.set("players." + uuid + ".claimed." + taskId, true);
        savePlayers();
    }

    public void addProgress(Player player, String material, String type, int amount) {
        String path = "players." + player.getUniqueId() + ".progress." + type + "." + material.toLowerCase(Locale.ROOT);
        playerConfig.set("players." + player.getUniqueId() + ".name", player.getName());
        playerConfig.set(path, playerConfig.getInt(path) + amount);
        savePlayers();
    }

    public int getProgress(UUID uuid, String material, String type) { return playerConfig.getInt("players." + uuid + ".progress." + type + "." + material.toLowerCase(Locale.ROOT), 0); }

    public boolean reset(String name) {
        OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(name);
        UUID uuid = op == null ? findUuidByName(name) : op.getUniqueId();
        if (uuid == null) return false;
        playerConfig.set("players." + uuid, null);
        savePlayers();
        return true;
    }

    private UUID findUuidByName(String name) {
        ConfigurationSection sec = playerConfig.getConfigurationSection("players");
        if (sec == null) return null;
        for (String key : sec.getKeys(false)) if (name.equalsIgnoreCase(playerConfig.getString("players." + key + ".name"))) return UUID.fromString(key);
        return null;
    }

    public void runActions(Player player, PlayerTask task) {
        for (String command : task.actions()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Text.papi(player, command).replace("%player_name%", player.getName()).replace("%player%", player.getName()));
    }

    public void refreshDueTasks(Player player) {
        long now = System.currentTimeMillis();
        String base = "players." + player.getUniqueId();
        boolean changed = false;
        for (PlayerTask task : tasks.values()) {
            long refreshAt = playerConfig.getLong(base + ".refresh-at." + task.id(), 0L);
            if (refreshAt > 0L && now >= refreshAt) {
                playerConfig.set(base + ".completed." + task.id(), null);
                playerConfig.set(base + ".claimed." + task.id(), null);
                playerConfig.set(base + ".completed-at." + task.id(), null);
                playerConfig.set(base + ".refresh-at." + task.id(), null);
                for (TaskCondition condition : task.conditions()) {
                    if (condition.type() == ConditionType.USE_ITEM || condition.type() == ConditionType.GET_ITEM || condition.type() == ConditionType.DROP_ITEM) {
                        playerConfig.set(base + ".progress." + condition.type().name().toLowerCase(Locale.ROOT) + "." + condition.left().toLowerCase(Locale.ROOT), null);
                    }
                }
                changed = true;
            }
        }
        if (changed) savePlayers();
    }

    public int getCompletedCount(Player player) { int count = 0; for (PlayerTask task : tasks.values()) if (isComplete(player, task.id())) count++; return count; }
    public int getTotalCount() { return tasks.size(); }
    public int getIncompleteCount(Player player) { return Math.max(0, getTotalCount() - getCompletedCount(player)); }

    public long getTaskRefreshMillis(Player player, String taskId) {
        if (!tasks.containsKey(taskId)) return 0L;
        return playerConfig.getLong("players." + player.getUniqueId() + ".refresh-at." + taskId, 0L);
    }

    public String getTaskRefreshRemaining(Player player, String taskId) {
        long refreshAt = getTaskRefreshMillis(player, taskId);
        if (refreshAt <= 0L) return "none";
        return String.valueOf(Math.max(0L, (refreshAt - System.currentTimeMillis() + 999L) / 1000L));
    }

    public String getTaskRefreshDate(Player player, String taskId) {
        long refreshAt = getTaskRefreshMillis(player, taskId);
        if (refreshAt <= 0L) return "none";
        return DATE_FORMAT.format(Instant.ofEpochMilli(refreshAt).atZone(ZoneId.systemDefault()));
    }

    public void savePlayers() { try { playerConfig.save(playerFile); } catch (IOException e) { plugin.getLogger().severe("保存 player.yml 失败: " + e.getMessage()); } }
}
