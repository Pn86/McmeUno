package cn.pn86.pnskill.command;

import cn.pn86.pnskill.PnSkillPlugin;
import cn.pn86.pnskill.config.MessageService;
import cn.pn86.pnskill.config.SkillConfigLoader;
import cn.pn86.pnskill.service.SkillCastService;
import cn.pn86.pnskill.util.SkillItemTag;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PnSkillCommand implements CommandExecutor, TabCompleter {
    private final PnSkillPlugin plugin;
    private final MessageService messageService;
    private final SkillConfigLoader skillConfigLoader;
    private final SkillCastService skillCastService;
    private final SkillItemTag skillItemTag;

    public PnSkillCommand(
            PnSkillPlugin plugin,
            MessageService messageService,
            SkillConfigLoader skillConfigLoader,
            SkillCastService skillCastService
    ) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.skillConfigLoader = skillConfigLoader;
        this.skillCastService = skillCastService;
        this.skillItemTag = skillCastService.getSkillItemTag();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            messageService.send(sender, "usage-main");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "bind" -> handleBind(sender, args);
            case "see" -> handleSee(sender);
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
            case "skill" -> handleSkill(sender, args);
            default -> messageService.send(sender, "usage-main");
        }
        return true;
    }

    private void handleBind(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageService.send(sender, "player-only");
            return;
        }

        if (args.length < 2) {
            messageService.send(sender, "usage-bind");
            return;
        }

        String skillId = args[1].toLowerCase(Locale.ROOT);
        if (skillConfigLoader.getSkill(skillId) == null) {
            messageService.send(player, "bind-fail-not-found", Map.of("skill", skillId));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            messageService.send(player, "bind-fail-empty-hand");
            return;
        }

        if (!skillItemTag.bindSkill(item, skillId)) {
            messageService.send(player, "bind-fail-empty-hand");
            return;
        }

        messageService.send(player, "bind-success", Map.of("skill", skillId));
    }

    private void handleSee(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageService.send(sender, "player-only");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        String skillId = skillItemTag.getBoundSkill(item);
        if (skillId == null || skillId.isBlank()) {
            messageService.send(player, "bind-none");
            return;
        }
        messageService.send(player, "bind-seen", Map.of("skill", skillId));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("pnskill.admin")) {
            messageService.send(sender, "no-permission");
            return;
        }
        plugin.reloadEverything();
        messageService.send(sender, "reloaded");
    }

    private void handleList(CommandSender sender) {
        if (skillConfigLoader.getSkillIds().isEmpty()) {
            messageService.send(sender, "list-empty");
            return;
        }
        String joined = String.join(", ", skillConfigLoader.getSkillIds());
        messageService.send(sender, "list-header", Map.of("skills", joined));
    }

    private void handleSkill(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageService.send(sender, "player-only");
            return;
        }
        if (args.length < 3) {
            messageService.send(sender, "usage-skill");
            return;
        }

        String skillId = args[1].toLowerCase(Locale.ROOT);
        String mode = args[2].toLowerCase(Locale.ROOT);
        if (!mode.equals("a") && !mode.equals("b")) {
            messageService.send(sender, "invalid-mode");
            return;
        }
        skillCastService.cast(player, skillId, mode, true);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            return filter(List.of("bind", "see", "reload", "list", "skill"), args[0]);
        }
        if (args.length == 2 && ("bind".equalsIgnoreCase(args[0]) || "skill".equalsIgnoreCase(args[0]))) {
            return filter(new ArrayList<>(skillConfigLoader.getSkillIds()), args[1]);
        }
        if (args.length == 3 && "skill".equalsIgnoreCase(args[0])) {
            return filter(List.of("a", "b"), args[2]);
        }
        return result;
    }

    private List<String> filter(List<String> candidates, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(x -> x.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted()
                .toList();
    }
}
