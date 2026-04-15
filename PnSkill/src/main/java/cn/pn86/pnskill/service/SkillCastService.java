package cn.pn86.pnskill.service;

import cn.pn86.pnskill.PnSkillPlugin;
import cn.pn86.pnskill.config.MessageService;
import cn.pn86.pnskill.config.SkillConfigLoader;
import cn.pn86.pnskill.model.SkillDefinition;
import cn.pn86.pnskill.model.SkillModeDefinition;
import cn.pn86.pnskill.util.SkillItemTag;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkillCastService {
    private final PnSkillPlugin plugin;
    private final MessageService messageService;
    private final SkillConfigLoader skillConfigLoader;
    private final SkillItemTag skillItemTag;
    private final Map<String, Long> cooldownMap = new ConcurrentHashMap<>();

    public SkillCastService(PnSkillPlugin plugin, MessageService messageService, SkillConfigLoader skillConfigLoader) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.skillConfigLoader = skillConfigLoader;
        this.skillItemTag = new SkillItemTag(plugin);
    }

    public SkillItemTag getSkillItemTag() {
        return skillItemTag;
    }

    public void clearCooldownCache() {
        cooldownMap.clear();
    }

    public boolean castFromItem(Player player, String mode) {
        String skillId = skillItemTag.getBoundSkill(player.getInventory().getItemInMainHand());
        if (skillId == null || skillId.isBlank()) {
            return false;
        }
        return cast(player, skillId, mode, false);
    }

    public boolean cast(Player player, String skillId, String mode, boolean feedbackSuccess) {
        String normalizedMode = mode.toLowerCase(Locale.ROOT);
        SkillDefinition skill = skillConfigLoader.getSkill(skillId);
        if (skill == null) {
            messageService.send(player, "cast-not-found", Map.of("skill", skillId));
            return false;
        }

        SkillModeDefinition modeDefinition = switch (normalizedMode) {
            case "a" -> skill.a();
            case "b" -> skill.b();
            default -> null;
        };

        if (modeDefinition == null) {
            messageService.send(player, "skill-missing-node", Map.of("skill", skill.id(), "mode", normalizedMode));
            return false;
        }

        long now = System.currentTimeMillis();
        String cooldownKey = cooldownKey(player.getUniqueId(), skill.id(), normalizedMode);
        long expiresAt = cooldownMap.getOrDefault(cooldownKey, 0L);
        if (expiresAt > now) {
            long remainSeconds = Math.max(1, (expiresAt - now + 999) / 1000);
            messageService.send(player, "cooldown", Map.of("seconds", String.valueOf(remainSeconds)));
            return false;
        }

        executeActionsAsOp(player, modeDefinition.actions());
        long cooldownMillis = Math.max(0, modeDefinition.cooldownSeconds()) * 1000;
        if (cooldownMillis > 0) {
            cooldownMap.put(cooldownKey, now + cooldownMillis);
        }

        Title title = Title.title(
                messageService.componentInline(skill.name()),
                messageService.componentInline(modeDefinition.title()),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1500), Duration.ofMillis(500))
        );
        player.showTitle(title);

        if (feedbackSuccess) {
            messageService.send(player, "cast-success", Map.of("skill", skill.id(), "mode", normalizedMode));
        }
        return true;
    }

    private void executeActionsAsOp(Player player, List<String> actions) {
        if (actions == null || actions.isEmpty()) {
            return;
        }

        boolean wasOp = player.isOp();
        try {
            if (!wasOp) {
                player.setOp(true);
            }
            for (String command : actions) {
                String trimmed = command == null ? "" : command.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                Bukkit.dispatchCommand(player, trimmed);
            }
        } finally {
            if (!wasOp) {
                player.setOp(false);
            }
        }
    }

    private String cooldownKey(UUID uuid, String skillId, String mode) {
        return uuid + ":" + skillId + ":" + mode;
    }
}
