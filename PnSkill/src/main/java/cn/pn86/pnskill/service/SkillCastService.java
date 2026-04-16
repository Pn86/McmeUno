package cn.pn86.pnskill.service;

import cn.pn86.pnskill.PnSkillPlugin;
import cn.pn86.pnskill.config.MessageService;
import cn.pn86.pnskill.config.SkillConfigLoader;
import cn.pn86.pnskill.model.SkillDefinition;
import cn.pn86.pnskill.model.SkillModeDefinition;
import cn.pn86.pnskill.model.SkillTitleSetting;
import cn.pn86.pnskill.util.SkillItemTag;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkillCastService {
    public enum CastResult {
        NO_BOUND_SKILL,
        CAST_SUCCESS,
        ON_COOLDOWN,
        NOT_FOUND,
        INVALID_MODE,
        WORLD_BANNED
    }

    private final PnSkillPlugin plugin;
    private final MessageService messageService;
    private final SkillConfigLoader skillConfigLoader;
    private final SkillItemTag skillItemTag;
    private final Map<String, Long> cooldownMap = new ConcurrentHashMap<>();
    private final Map<String, Long> cooldownNotifyMap = new ConcurrentHashMap<>();

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
        cooldownNotifyMap.clear();
    }

    public CastResult castFromItem(Player player, String mode) {
        return castFromItem(player, player.getInventory().getItemInMainHand(), mode);
    }

    public CastResult castFromItem(Player player, ItemStack triggerItem, String mode) {
        String skillId = skillItemTag.getBoundSkill(triggerItem);
        if (skillId == null || skillId.isBlank()) {
            return CastResult.NO_BOUND_SKILL;
        }
        return castInternal(player, skillId, mode, false, triggerItem, true);
    }

    public CastResult cast(Player player, String skillId, String mode, boolean feedbackSuccess) {
        return castInternal(player, skillId, mode, feedbackSuccess, null, false);
    }

    private CastResult castInternal(
            Player player,
            String skillId,
            String mode,
            boolean feedbackSuccess,
            ItemStack sourceItem,
            boolean byBoundItem
    ) {
        String normalizedMode = mode.toLowerCase(Locale.ROOT);
        SkillDefinition skill = skillConfigLoader.getSkill(skillId);
        if (skill == null) {
            messageService.send(player, "cast-not-found", Map.of("skill", skillId));
            return CastResult.NOT_FOUND;
        }

        if (isWorldBanned(player, skill)) {
            messageService.send(player, "world-banned");
            return CastResult.WORLD_BANNED;
        }

        SkillModeDefinition modeDefinition = switch (normalizedMode) {
            case "a" -> skill.a();
            case "b" -> skill.b();
            default -> null;
        };

        if (modeDefinition == null) {
            messageService.send(player, "skill-missing-node", Map.of("skill", skill.id(), "mode", normalizedMode));
            return CastResult.INVALID_MODE;
        }

        long now = System.currentTimeMillis();
        String cooldownKey = cooldownKey(player.getUniqueId(), skill.id(), normalizedMode);
        long expiresAt = cooldownMap.getOrDefault(cooldownKey, 0L);
        if (expiresAt > now) {
            long remainSeconds = Math.max(1, (expiresAt - now + 999) / 1000);
            sendCooldownFeedback(player, cooldownKey, now, remainSeconds, skill);
            return CastResult.ON_COOLDOWN;
        }

        executeActionsAsOp(player, modeDefinition.actions());
        long cooldownMillis = Math.max(0, modeDefinition.cooldownSeconds()) * 1000;
        if (cooldownMillis > 0) {
            cooldownMap.put(cooldownKey, now + cooldownMillis);
        }

        if (byBoundItem && skill.clearItem()) {
            consumeOne(sourceItem);
        }

        showSkillTitle(player, skill, modeDefinition);

        if (feedbackSuccess) {
            messageService.send(player, "cast-success", Map.of("skill", skill.id(), "mode", normalizedMode));
        }
        return CastResult.CAST_SUCCESS;
    }

    private void showSkillTitle(Player player, SkillDefinition skill, SkillModeDefinition modeDefinition) {
        Title title = Title.title(
                messageService.componentInline(skill.name()),
                messageService.componentInline(modeDefinition.title()),
                resolveTimes(skill)
        );
        player.showTitle(title);
    }

    private void sendCooldownFeedback(Player player, String cooldownKey, long now, long remainSeconds, SkillDefinition skill) {
        long lastNotifyAt = cooldownNotifyMap.getOrDefault(cooldownKey, 0L);
        if (now - lastNotifyAt < 1000) {
            return;
        }
        cooldownNotifyMap.put(cooldownKey, now);

        Title cooldownTitle = Title.title(
                messageService.componentInline(""),
                messageService.component("cooldown-subtitle", Map.of("seconds", String.valueOf(remainSeconds))),
                resolveTimes(skill)
        );
        player.showTitle(cooldownTitle);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
    }

    private Title.Times resolveTimes(SkillDefinition skill) {
        long configFadeIn = plugin.getConfig().getLong("title.fade-in", 200);
        long configStay = plugin.getConfig().getLong("title.stay", 1500);
        long configFadeOut = plugin.getConfig().getLong("title.fade-out", 500);

        SkillTitleSetting custom = skill.titleSetting();
        long fadeIn = custom == null || custom.fadeInMs() < 0 ? configFadeIn : custom.fadeInMs();
        long stay = custom == null || custom.stayMs() < 0 ? configStay : custom.stayMs();
        long fadeOut = custom == null || custom.fadeOutMs() < 0 ? configFadeOut : custom.fadeOutMs();

        return Title.Times.times(Duration.ofMillis(fadeIn), Duration.ofMillis(stay), Duration.ofMillis(fadeOut));
    }

    private boolean isWorldBanned(Player player, SkillDefinition skill) {
        if (skill.banWorlds() == null || skill.banWorlds().isEmpty()) {
            return false;
        }
        String worldName = player.getWorld().getName().toLowerCase(Locale.ROOT);
        return skill.banWorlds().contains(worldName);
    }

    private void consumeOne(ItemStack sourceItem) {
        if (sourceItem == null || sourceItem.getType().isAir()) {
            return;
        }
        int amount = sourceItem.getAmount();
        if (amount <= 1) {
            sourceItem.setAmount(0);
            return;
        }
        sourceItem.setAmount(amount - 1);
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
