package com.pn86.pnvip;

import com.pn86.pnvip.data.DataStore;
import com.pn86.pnvip.model.PlayerVipRecord;
import com.pn86.pnvip.model.VipDefinition;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class VipManager {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PnVipPlugin plugin;
    private final DataStore dataStore;
    private final Map<String, VipDefinition> definitions = new HashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    private final Map<String, Long> unitSeconds = new HashMap<>();

    public VipManager(PnVipPlugin plugin, DataStore dataStore) {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    public void reloadAll() {
        loadUnitMap();
        loadVipDefinitions();
        cleanInvalidOrExpired();
        dataStore.save();
    }

    private void loadUnitMap() {
        unitSeconds.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("time-units");
        if (section == null) {
            unitSeconds.put("s", 1L);
            unitSeconds.put("m", 60L);
            unitSeconds.put("d", 86400L);
            return;
        }
        for (String key : section.getKeys(false)) {
            unitSeconds.put(key.toLowerCase(Locale.ROOT), section.getLong(key));
        }
    }

    private void loadVipDefinitions() {
        definitions.clear();
        File file = new File(plugin.getDataFolder(), "vip.yml");
        YamlConfiguration vipConfig = YamlConfiguration.loadConfiguration(file);
        for (String key : vipConfig.getKeys(false)) {
            ConfigurationSection section = vipConfig.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            String displayName = section.getString("name", key);
            List<String> permissions = section.getStringList("per");
            List<String> signinCommands = section.getStringList("signin");
            List<String> giftCommands = section.getStringList("gift");
            definitions.put(key.toLowerCase(Locale.ROOT), new VipDefinition(key, displayName, permissions, signinCommands, giftCommands));
        }
    }

    public Collection<VipDefinition> getDefinitions() {
        return definitions.values();
    }

    public VipDefinition getDefinition(String name) {
        return definitions.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean removeExpired(PlayerVipRecord record) {
        boolean changed = false;
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = record.getVipExpireAt().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            String vipName = entry.getKey();
            Long expireAt = entry.getValue();
            if (!definitions.containsKey(vipName.toLowerCase(Locale.ROOT)) || (expireAt > 0 && expireAt <= now)) {
                iterator.remove();
                record.getLastSigninDate().remove(vipName);
                changed = true;
            }
        }
        return changed;
    }

    private void cleanInvalidOrExpired() {
        for (UUID uuid : dataStore.getAllPlayers()) {
            PlayerVipRecord record = dataStore.getRecord(uuid);
            if (record != null) {
                removeExpired(record);
            }
        }
    }

    public long parseDurationSeconds(long amount, String unit) {
        Long mult = unitSeconds.get(unit.toLowerCase(Locale.ROOT));
        if (mult == null || amount <= 0) {
            return -1;
        }
        return amount * mult;
    }

    public Set<String> supportedUnits() {
        return unitSeconds.keySet();
    }

    public long grantVip(UUID uuid, String playerName, String vipName, long durationSeconds) {
        PlayerVipRecord record = dataStore.getOrCreateRecord(uuid, playerName);
        String key = vipName.toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        long current = record.getVipExpireAt().getOrDefault(key, now);
        if (current < now) {
            current = now;
        }
        long newExpireAt = current + (durationSeconds * 1000L);
        boolean isNew = !record.getVipExpireAt().containsKey(key) || record.getVipExpireAt().get(key) <= now;
        record.getVipExpireAt().put(key, newExpireAt);
        dataStore.save();

        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            applyPermissions(online);
            if (isNew) {
                executeGiftCommands(online, key);
            }
        }
        return newExpireAt;
    }

    public boolean removeVip(UUID uuid, String vipName) {
        PlayerVipRecord record = dataStore.getRecord(uuid);
        if (record == null) {
            return false;
        }
        String key = vipName.toLowerCase(Locale.ROOT);
        boolean removed = record.getVipExpireAt().remove(key) != null;
        record.getLastSigninDate().remove(key);
        if (removed) {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) {
                applyPermissions(online);
            }
            dataStore.save();
        }
        return removed;
    }

    public boolean clearVip(UUID uuid) {
        PlayerVipRecord record = dataStore.getRecord(uuid);
        if (record == null || record.getVipExpireAt().isEmpty()) {
            return false;
        }
        record.getVipExpireAt().clear();
        record.getLastSigninDate().clear();
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            applyPermissions(online);
        }
        dataStore.save();
        return true;
    }

    public void applyPermissions(Player player) {
        PlayerVipRecord record = dataStore.getOrCreateRecord(player.getUniqueId(), player.getName());
        removeExpired(record);

        PermissionAttachment old = attachments.remove(player.getUniqueId());
        if (old != null) {
            player.removeAttachment(old);
        }

        PermissionAttachment attachment = player.addAttachment(plugin);
        Set<String> permissions = new HashSet<>();

        for (Map.Entry<String, Long> entry : record.getVipExpireAt().entrySet()) {
            VipDefinition definition = getDefinition(entry.getKey());
            if (definition == null) {
                continue;
            }
            permissions.addAll(definition.permissions());
        }

        for (String permission : permissions) {
            attachment.setPermission(permission, true);
        }

        attachments.put(player.getUniqueId(), attachment);
        player.recalculatePermissions();
    }

    public void clearAttachment(UUID uuid) {
        PermissionAttachment attachment = attachments.remove(uuid);
        if (attachment != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.removeAttachment(attachment);
                player.recalculatePermissions();
            }
        }
    }

    public void clearAttachments() {
        for (UUID uuid : new HashSet<>(attachments.keySet())) {
            clearAttachment(uuid);
        }
    }

    public boolean signin(Player player) {
        PlayerVipRecord record = dataStore.getOrCreateRecord(player.getUniqueId(), player.getName());
        removeExpired(record);
        LocalDate today = LocalDate.now();
        boolean executed = false;
        for (String vipName : record.getVipExpireAt().keySet()) {
            VipDefinition definition = getDefinition(vipName);
            if (definition == null || definition.signinCommands().isEmpty()) {
                continue;
            }
            String last = record.getLastSigninDate().get(vipName);
            if (today.toString().equals(last)) {
                continue;
            }
            for (String command : definition.signinCommands()) {
                dispatchConsoleCommand(command, player.getName());
            }
            record.getLastSigninDate().put(vipName, today.toString());
            executed = true;
        }
        if (executed) {
            dataStore.save();
        }
        return executed;
    }

    public void executeGiftCommands(Player player, String vipName) {
        VipDefinition definition = getDefinition(vipName);
        if (definition == null || definition.giftCommands().isEmpty()) {
            return;
        }
        for (String command : definition.giftCommands()) {
            dispatchConsoleCommand(command, player.getName());
        }
    }

    private void dispatchConsoleCommand(String command, String playerName) {
        String parsed = command.replace("%player%", playerName);
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        Bukkit.dispatchCommand(console, parsed);
    }

    public String formatExpireAt(long expireAt) {
        if (expireAt <= 0) {
            return color(plugin.getConfig().getString("format.permanent", "永久"));
        }
        return Instant.ofEpochMilli(expireAt)
                .atZone(ZoneId.systemDefault())
                .format(DATE_TIME_FORMAT);
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
