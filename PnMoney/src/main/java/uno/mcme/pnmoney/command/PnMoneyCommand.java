package uno.mcme.pnmoney.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uno.mcme.pnmoney.MoneyManager;
import uno.mcme.pnmoney.PnMoneyPlugin;
import uno.mcme.pnmoney.data.PlayerBalance;
import uno.mcme.pnmoney.shop.ShopManager;
import uno.mcme.pnmoney.util.Text;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PnMoneyCommand implements CommandExecutor, TabCompleter {

    private final PnMoneyPlugin plugin;
    private final MoneyManager money;
    private final ShopManager shop;

    public PnMoneyCommand(PnMoneyPlugin plugin, MoneyManager money, ShopManager shop) {
        this.plugin = plugin;
        this.money = money;
        this.shop = shop;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            send(sender, "messages.help");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload" -> {
                if (!sender.hasPermission("pnmoney.admin")) {
                    send(sender, "messages.no-permission");
                    return true;
                }
                plugin.reloadEverything();
                send(sender, "messages.reload");
                return true;
            }
            case "give", "take", "set" -> {
                if (!sender.hasPermission("pnmoney.admin")) {
                    send(sender, "messages.no-permission");
                    return true;
                }
                return handleAdminAmount(sender, sub, args);
            }
            case "reset" -> {
                if (!sender.hasPermission("pnmoney.admin")) {
                    send(sender, "messages.no-permission");
                    return true;
                }
                return handleReset(sender, args);
            }
            case "pay" -> {
                if (!sender.hasPermission("pnmoney.admin")) {
                    send(sender, "messages.no-permission");
                    return false;
                }
                return handlePay(sender, args);
            }
            case "list" -> {
                if (!sender.hasPermission("pnmoney.player")
                        && !sender.hasPermission("pnmoney.use")
                        && !sender.hasPermission("pnmoney.admin")) {
                    send(sender, "messages.no-permission");
                    return false;
                }
                return handleList(sender, args);
            }
            case "top" -> {
                if (!sender.hasPermission("pnmoney.player")
                        && !sender.hasPermission("pnmoney.use")
                        && !sender.hasPermission("pnmoney.admin")) {
                    send(sender, "messages.no-permission");
                    return false;
                }
                return handleTop(sender);
            }
            case "shop" -> {
                if (!sender.hasPermission("pnmoney.player")
                        && !sender.hasPermission("pnmoney.use")
                        && !sender.hasPermission("pnmoney.admin")) {
                    send(sender, "messages.no-permission");
                    return false;
                }
                return handleShop(sender, args);
            }
            default -> {
                send(sender, "messages.help");
                return true;
            }
        }
    }

    private boolean handleAdminAmount(CommandSender sender, String sub, String[] args) {
        if (args.length < 3) {
            send(sender, "messages.invalid-usage");
            return false;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        BigDecimal amount = parseAmount(sender, args[2]);
        if (amount == null) {
            return false;
        }

        boolean success = switch (sub) {
            case "give" -> money.addBalance(target, amount);
            case "take" -> money.takeBalance(target, amount);
            default -> money.setBalance(target, amount);
        };

        if (!success) {
            send(sender, "messages.operation-failed");
            return false;
        }

        String msg = plugin.getConfig().getString("messages.admin-success", "&a操作成功: %target% -> %bal% %money%");
        msg = msg.replace("%target%", target.getName() == null ? target.getUniqueId().toString() : target.getName())
                .replace("%bal%", money.getBalance(target).stripTrailingZeros().toPlainString())
                .replace("%money%", money.getCurrencyName());
        sender.sendMessage(Text.color(msg));
        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, "messages.invalid-usage");
            return false;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (money.resetBalance(target)) {
            send(sender, "messages.reset-success");
            return true;
        } else {
            send(sender, "messages.operation-failed");
            return false;
        }
    }

    private boolean handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "messages.player-only");
            return false;
        }
        if (args.length < 3) {
            send(sender, "messages.invalid-usage");
            return false;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getUniqueId().equals(player.getUniqueId())) {
            send(sender, "messages.pay-self");
            return false;
        }

        BigDecimal amount = parseAmount(sender, args[2]);
        if (amount == null) {
            return false;
        }

        boolean success = money.transfer(player, target, amount);
        if (!success) {
            send(sender, "messages.not-enough");
            return false;
        }

        String senderMsg = plugin.getConfig().getString("messages.pay-success", "&a支付成功: %target% +%amount% %money%");
        senderMsg = senderMsg.replace("%target%", target.getName() == null ? "unknown" : target.getName())
                .replace("%amount%", amount.stripTrailingZeros().toPlainString())
                .replace("%money%", money.getCurrencyName());
        player.sendMessage(Text.color(senderMsg));

        Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
        if (onlineTarget != null) {
            String receiveMsg = plugin.getConfig().getString("messages.receive", "&a你收到了 %from% 的 %amount% %money%");
            receiveMsg = receiveMsg.replace("%from%", player.getName())
                    .replace("%amount%", amount.stripTrailingZeros().toPlainString())
                    .replace("%money%", money.getCurrencyName());
            onlineTarget.sendMessage(Text.color(receiveMsg));
        }
        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        OfflinePlayer target;
        boolean isAdmin = sender.hasPermission("pnmoney.admin");
        if (args.length >= 2) {
            if (!isAdmin) {
                send(sender, "messages.no-permission");
                return false;
            }
            target = Bukkit.getOfflinePlayer(args[1]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            send(sender, "messages.invalid-usage");
            return false;
        }

        String msg = plugin.getConfig().getString("messages.balance", "&e%target% 余额: %bal% %money%");
        msg = msg.replace("%target%", target.getName() == null ? "unknown" : target.getName())
                .replace("%bal%", money.getBalance(target).stripTrailingZeros().toPlainString())
                .replace("%money%", money.getCurrencyName());
        sender.sendMessage(Text.color(msg));
        return true;
    }

    private boolean handleTop(CommandSender sender) {
        List<PlayerBalance> top = money.getTop(10);
        sender.sendMessage(Text.color(plugin.getConfig().getString("messages.top-title", "&6货币排行榜")));
        int i = 1;
        for (PlayerBalance balance : top) {
            String line = plugin.getConfig().getString("messages.top-line", "&e#%index% &f%player% &7- &a%bal% %money%")
                    .replace("%index%", String.valueOf(i++))
                    .replace("%player%", balance.playerName())
                    .replace("%bal%", balance.balance().stripTrailingZeros().toPlainString())
                    .replace("%money%", money.getCurrencyName());
            sender.sendMessage(Text.color(line));
        }
        return true;
    }

    private boolean handleShop(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "messages.player-only");
            return false;
        }
        if (args.length < 2) {
            send(sender, "messages.invalid-usage");
            return false;
        }

        var result = shop.purchase(player, args[1]);
        switch (result.status()) {
            case DISABLED -> {
                send(sender, "messages.shop-disabled");
                return false;
            }
            case NOT_FOUND -> {
                send(sender, "messages.shop-not-found");
                return false;
            }
            case INVALID_PRICE -> {
                send(sender, "messages.shop-invalid-price");
                return false;
            }
            case NOT_ENOUGH -> {
                send(sender, "messages.not-enough");
                return false;
            }
            case BALANCE_READ_FAILED, DEDUCT_FAILED -> {
                send(sender, "messages.operation-failed");
                return false;
            }
            case SUCCESS -> {
                String message = plugin.getConfig().getString("messages.shop-buy", "&a购买成功，花费 %amount% %money%")
                        .replace("%amount%", result.price().stripTrailingZeros().toPlainString())
                        .replace("%money%", money.getCurrencyName());
                player.sendMessage(Text.color(message));
                return true;
            }
        }
        return false;
    }

    private BigDecimal parseAmount(CommandSender sender, String raw) {
        try {
            BigDecimal amount = new BigDecimal(raw);
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                send(sender, "messages.invalid-number");
                return null;
            }
            return money.normalize(amount);
        } catch (Exception ex) {
            send(sender, "messages.invalid-number");
            return null;
        }
    }

    private void send(CommandSender sender, String key) {
        sender.sendMessage(Text.color(plugin.getConfig().getString(key, "&cMissing message: " + key)));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList("reload", "give", "take", "reset", "set", "pay", "list", "top", "shop");
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && Arrays.asList("give", "take", "reset", "set", "pay", "list").contains(args[0].toLowerCase())) {
            if ("list".equalsIgnoreCase(args[0]) && !sender.hasPermission("pnmoney.admin")) {
                return List.of();
            }
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names;
        }
        if (args.length == 2 && "shop".equalsIgnoreCase(args[0])) {
            return shop.getIds().stream().filter(id -> id.startsWith(args[1])).collect(Collectors.toList());
        }
        return List.of();
    }
}
