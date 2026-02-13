package com.pn86.pntimeworks;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class WorkManager {
    private static final DateTimeFormatter MINUTE_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final PnTimeWorksPlugin plugin;
    private final List<WorkGroup> workGroups = new ArrayList<>();
    private final Set<String> firedKeys = new HashSet<>();

    public WorkManager(PnTimeWorksPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadFromConfig(FileConfiguration worksConfig) {
        workGroups.clear();
        firedKeys.clear();

        boolean changed = false;

        for (String groupId : worksConfig.getKeys(false)) {
            ConfigurationSection section = worksConfig.getConfigurationSection(groupId);
            if (section == null) {
                continue;
            }

            List<String> rawTime = section.getStringList("time");
            List<String> commands = section.getStringList("action");
            List<String> sanitizedTime = new ArrayList<>();
            List<TimeRule> rules = new ArrayList<>();

            for (String raw : rawTime) {
                if ("none".equalsIgnoreCase(raw)) {
                    sanitizedTime.add("none");
                    continue;
                }

                TimeRule.parse(raw).ifPresentOrElse(rule -> {
                    rules.add(rule);
                    sanitizedTime.add(raw.trim());
                }, () -> {
                    sanitizedTime.add("none");
                });
            }

            if (!sanitizedTime.equals(rawTime)) {
                section.set("time", sanitizedTime);
                changed = true;
            }

            workGroups.add(new WorkGroup(groupId, rules, commands));
        }

        if (changed) {
            plugin.saveWorksConfig();
        }
    }

    public void tick() {
        LocalDateTime now = LocalDateTime.now();
        String minuteKey = now.format(MINUTE_KEY_FORMATTER);

        firedKeys.removeIf(key -> !key.startsWith(minuteKey));

        for (WorkGroup group : workGroups) {
            if (group.commands().isEmpty() || group.rules().isEmpty()) {
                continue;
            }

            for (TimeRule rule : group.rules()) {
                if (!rule.matches(now)) {
                    continue;
                }

                String executionKey = minuteKey + ":" + group.id() + ":" + rule.uniqueKey();
                if (firedKeys.contains(executionKey)) {
                    continue;
                }

                fireGroup(group);
                firedKeys.add(executionKey);
            }
        }
    }

    private void fireGroup(WorkGroup group) {
        for (String command : group.commands()) {
            if (command == null || command.isBlank()) {
                continue;
            }
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("group", group.id());
        placeholders.put("count", String.valueOf(group.commands().size()));

        String broadcast = plugin.message("work-executed-broadcast", placeholders);
        if (!broadcast.isBlank()) {
            plugin.getServer().broadcastMessage(broadcast);
        }

        plugin.getLogger().info("Executed work group " + group.id() + " with " + group.commands().size() + " command(s).");
    }

    public List<WorkGroup> getWorkGroups() {
        return workGroups;
    }
}
