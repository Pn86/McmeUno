package uno.mcme.pnmoneyshop.command;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uno.mcme.pnmoneyshop.PnMoneyShopPlugin;
import uno.mcme.pnmoneyshop.shop.ShopManager;
import uno.mcme.pnmoneyshop.shop.ShopResult;
import uno.mcme.pnmoneyshop.util.TextUtil;

import java.util.ArrayList;
import java.util.List;

public class PnMoneyShopCommand implements CommandExecutor, TabCompleter {

    private final PnMoneyShopPlugin plugin;
    private final ShopManager shopManager;

    public PnMoneyShopCommand(PnMoneyShopPlugin plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            send(sender, "usage");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("pnmoneyshop.admin")) {
                    send(sender, "no-permission");
                    return true;
                }
                plugin.reloadEverything();
                send(sender, "reload");
            }
            case "buy" -> {
                if (!(sender instanceof Player player)) {
                    send(sender, "player-only");
                    return true;
                }
                if (!player.hasPermission("pnmoneyshop.use")) {
                    send(sender, "no-permission");
                    return true;
                }
                if (args.length < 2) {
                    send(sender, "usage");
                    return true;
                }
                handleBuy(player, args[1]);
            }
            default -> send(sender, "usage");
        }
        return true;
    }

    private void handleBuy(Player player, String id) {
        ShopResult result = shopManager.buy(player, id);

        if (result.status() == ShopResult.Status.SUCCESS) {
            String message = shopManager.applyTokens(msg(result.reason()), player, result.price(), result.balance());
            player.sendMessage(prefix() + TextUtil.color(message));
            playFeedback(player, true, result);
            return;
        }

        String message = shopManager.applyTokens(msg(result.reason()), player, result.price(), result.balance());
        player.sendMessage(prefix() + TextUtil.color(message));
        playFeedback(player, false, result);
    }

    private void playFeedback(Player player, boolean success, ShopResult result) {
        String path = success ? "feedback.success" : "feedback.fail";
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null) {
            return;
        }

        String soundName = section.getString("sound", success ? "ENTITY_PLAYER_LEVELUP" : "ENTITY_VILLAGER_NO");
        float volume = (float) section.getDouble("volume", 1.0D);
        float pitch = (float) section.getDouble("pitch", 1.0D);

        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException ignored) {
            plugin.getLogger().warning("Invalid sound in config: " + soundName);
        }

        String title = section.getString("title", "");
        String subtitle = section.getString("subtitle", "");

        String parsedTitle = shopManager.applyTokens(title, player, result.price(), result.balance());
        String parsedSubtitle = shopManager.applyTokens(subtitle, player, result.price(), result.balance())
                .replace("%reason%", msg(result.reason()));

        player.sendTitle(TextUtil.color(parsedTitle), TextUtil.color(parsedSubtitle), 10, 50, 10);
    }

    private void send(CommandSender sender, String key) {
        sender.sendMessage(prefix() + TextUtil.color(msg(key)));
    }

    private String prefix() {
        return TextUtil.color(plugin.getConfig().getString("messages.prefix", ""));
    }

    private String msg(String key) {
        return plugin.getConfig().getString("messages." + key, key);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(args[0], List.of("reload", "buy"));
        }
        if (args.length == 2 && "buy".equalsIgnoreCase(args[0])) {
            return filter(args[1], new ArrayList<>(shopManager.getIds()));
        }
        return List.of();
    }

    private List<String> filter(String token, List<String> source) {
        String lower = token.toLowerCase();
        return source.stream().filter(s -> s.toLowerCase().startsWith(lower)).toList();
    }
}
