package com.pn86.pngoldprospecting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class PnGpCommand implements CommandExecutor, TabCompleter {
    private final PnGoldProspectingPlugin plugin;
    private final DataManager dataManager;

    public PnGpCommand(PnGoldProspectingPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pngp.admin")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, 1);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> {
                int page = 1;
                if (args.length >= 2) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {
                        page = 1;
                    }
                }
                sendHelp(sender, page);
            }
            case "creat", "create" -> handleCreate(sender, args);
            case "move" -> handleMove(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "skin" -> handleSkin(sender, args);
            case "additem" -> handleAddItem(sender, args);
            case "listitem" -> handleListItem(sender, args);
            case "removeitem" -> handleRemoveItem(sender, args);
            case "resettime" -> handleResetTime(sender, args);
            case "list" -> handleList(sender);
            case "look" -> handleLook(sender, args);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender, 1);
        }
        return true;
    }

    private void sendHelp(CommandSender sender, int page) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("§6==== PnGoldProspecting 帮助页 (%page%/%max%) ===="));
        lines.add(Component.text("§e/pngp creat [ID] §7- 创建淘金方块"));
        lines.add(Component.text("§e/pngp move [ID] §7- 移动淘金方块"));
        lines.add(Component.text("§e/pngp delete [ID] §7- 删除淘金方块"));
        lines.add(Component.text("§e/pngp skin [ID] [gravel/sand] §7- 设置外观"));
        lines.add(Component.text("§e/pngp additem [ID] [自定义物品ID] [权重可选] [指令可选] §7- 添加战利品/命令"));
        lines.add(Component.text("§e/pngp listitem [ID] §7- 查看战利品(含唯一键、显示名、权重、命令)"));
        lines.add(Component.text("§e/pngp removeitem [ID] [物品ID或唯一键] §7- 删除战利品"));
        lines.add(Component.text("§e/pngp resettime [ID] [秒] §7- 设置自动重置时间"));
        lines.add(Component.text("§e/pngp list §7- 查看全部淘金方块"));
        lines.add(Component.text("§e/pngp look [ID] §7- 查看方块详情"));
        lines.add(Component.text("§e/pngp reload §7- 重载配置与数据并重置状态"));

        int pageSize = 6;
        int maxPage = Math.max(1, (int) Math.ceil((lines.size() - 1) / (double) pageSize));
        int normalized = Math.min(Math.max(1, page), maxPage);

        sender.sendMessage(Component.text("§6==== PnGoldProspecting 帮助页 (" + normalized + "/" + maxPage + ") ===="));
        int start = 1 + (normalized - 1) * pageSize;
        int end = Math.min(lines.size(), start + pageSize);
        for (int i = start; i < end; i++) {
            sender.sendMessage(lines.get(i));
        }
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("player-only"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.msg("usage-create"));
            return;
        }

        String id = args[1];
        Location loc = getTargetBlockAbove(player);
        if (loc == null) {
            sender.sendMessage(plugin.msg("invalid-target"));
            return;
        }
        if (dataManager.exists(id)) {
            sender.sendMessage(plugin.msg("id-exists"));
            return;
        }
        if (!loc.getBlock().getType().isAir()) {
            sender.sendMessage(plugin.msg("target-not-air"));
            return;
        }

        Material skin = dataManager.parseSkin(plugin.getConfig().getString("defaults.skin", "sand")).orElse(Material.SUSPICIOUS_SAND);
        int resetTime = plugin.getConfig().getInt("defaults.reset-time-seconds", 300);
        boolean ok = dataManager.createBlock(id, loc, skin, resetTime);
        sender.sendMessage(ok ? plugin.msg("create-success") : plugin.msg("create-fail"));
    }

    private void handleMove(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("player-only"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.msg("usage-move"));
            return;
        }
        Location loc = getTargetBlockAbove(player);
        if (loc == null || !loc.getBlock().getType().isAir()) {
            sender.sendMessage(plugin.msg("invalid-target"));
            return;
        }
        boolean ok = dataManager.moveBlock(args[1], loc);
        sender.sendMessage(ok ? plugin.msg("move-success") : plugin.msg("id-not-found"));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.msg("usage-delete"));
            return;
        }
        boolean ok = dataManager.deleteBlock(args[1]);
        sender.sendMessage(ok ? plugin.msg("delete-success") : plugin.msg("id-not-found"));
    }

    private void handleSkin(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.msg("usage-skin"));
            return;
        }
        var skin = dataManager.parseSkin(args[2]);
        if (skin.isEmpty()) {
            sender.sendMessage(plugin.msg("invalid-skin"));
            return;
        }
        boolean ok = dataManager.setSkin(args[1], skin.get());
        sender.sendMessage(ok ? plugin.msg("skin-success") : plugin.msg("id-not-found"));
    }

    private void handleAddItem(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("player-only"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.msg("usage-additem"));
            return;
        }

        String blockId = args[1];
        String lootId = args[2];

        int weight = 0;
        String commandText = null;
        if (args.length >= 4) {
            try {
                weight = Integer.parseInt(args[3]);
                if (args.length >= 5) {
                    commandText = String.join(" ", List.of(args).subList(4, args.length));
                }
            } catch (NumberFormatException ignored) {
                commandText = String.join(" ", List.of(args).subList(3, args.length));
            }
        }

        ItemStack hand = null;
        if (commandText == null || commandText.isBlank()) {
            hand = player.getInventory().getItemInMainHand();
            if (hand.getType().isAir()) {
                sender.sendMessage(plugin.msg("hand-empty"));
                return;
            }
        }

        boolean ok = dataManager.addLoot(blockId, lootId, hand, weight, commandText);
        sender.sendMessage(ok ? plugin.msg("additem-success") : plugin.msg("additem-fail"));
    }

    private void handleListItem(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.msg("usage-listitem"));
            return;
        }
        ProspectingBlock block = dataManager.getBlock(args[1]);
        if (block == null) {
            sender.sendMessage(plugin.msg("id-not-found"));
            return;
        }

        sender.sendMessage(Component.text("----- " + block.getId() + " loot -----"));
        for (Map.Entry<String, LootEntry> kv : block.getLoots().entrySet()) {
            LootEntry entry = kv.getValue();
            String displayName = getItemDisplayName(entry);
            String commandState = entry.isCommandLoot() ? " | command=" + entry.command() : "";
            sender.sendMessage(Component.text("key=" + kv.getKey() + " | id=" + entry.itemId() + " | name=" + displayName + " | weight=" + entry.weight() + commandState));
        }
    }

    private String getItemDisplayName(LootEntry entry) {
        if (entry.itemStack() == null || entry.itemStack().getType().isAir()) {
            return "命令战利品";
        }

        ItemMeta meta = entry.itemStack().getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
            return PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        }

        return entry.itemStack().getType().name();
    }

    private void handleRemoveItem(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.msg("usage-removeitem"));
            return;
        }
        boolean ok = dataManager.removeLoot(args[1], args[2]);
        sender.sendMessage(ok ? plugin.msg("removeitem-success") : plugin.msg("removeitem-fail"));
    }

    private void handleResetTime(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.msg("usage-resettime"));
            return;
        }
        int seconds;
        try {
            seconds = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.msg("invalid-number"));
            return;
        }
        boolean ok = dataManager.setResetTime(args[1], seconds);
        sender.sendMessage(ok ? plugin.msg("resettime-success") : plugin.msg("id-not-found"));
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage(Component.text("----- Prospecting Blocks -----"));
        for (Map.Entry<String, ProspectingBlock> entry : dataManager.getBlocks().entrySet()) {
            Location loc = entry.getValue().getLocation();
            sender.sendMessage(Component.text(entry.getKey() + " -> " + loc.getWorld().getName() + ":" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()));
        }
    }

    private void handleLook(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.msg("usage-look"));
            return;
        }
        ProspectingBlock block = dataManager.getBlock(args[1]);
        if (block == null) {
            sender.sendMessage(plugin.msg("id-not-found"));
            return;
        }
        Location loc = block.getLocation();
        sender.sendMessage(Component.text("ID: " + block.getId()));
        sender.sendMessage(Component.text("Loc: " + loc.getWorld().getName() + ", " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
        sender.sendMessage(Component.text("Skin: " + block.getSkin()));
        sender.sendMessage(Component.text("Reset: " + block.getResetTimeSeconds() + "s"));
        sender.sendMessage(Component.text("Opened: " + block.isOpened()));
        sender.sendMessage(Component.text("Loot count: " + block.getLoots().size()));
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadPlugin();
        sender.sendMessage(plugin.msg("reload-success"));
    }

    private Location getTargetBlockAbove(Player player) {
        var target = player.getTargetBlockExact(8);
        if (target == null) {
            return null;
        }
        return target.getLocation().add(0, 1, 0);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(args[0], List.of("help", "creat", "move", "delete", "skin", "additem", "listitem", "removeitem", "resettime", "list", "look", "reload"));
        }
        if (args.length == 2 && List.of("move", "delete", "skin", "additem", "listitem", "removeitem", "resettime", "look").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(args[1], new ArrayList<>(dataManager.getBlocks().keySet()));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("skin")) {
            return filter(args[2], List.of("sand", "gravel"));
        }
        return Collections.emptyList();
    }

    private List<String> filter(String token, List<String> source) {
        return source.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(token.toLowerCase(Locale.ROOT))).collect(Collectors.toList());
    }
}
