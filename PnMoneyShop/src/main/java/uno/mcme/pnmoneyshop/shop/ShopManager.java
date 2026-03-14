package uno.mcme.pnmoneyshop.shop;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import uno.mcme.pnmoneyshop.PnMoneyShopPlugin;
import uno.mcme.pnmoneyshop.util.NumberParser;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ShopManager {

    private final PnMoneyShopPlugin plugin;
    private final Map<String, ShopEntry> entries = new LinkedHashMap<>();
    private boolean enabled;

    public ShopManager(PnMoneyShopPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        entries.clear();

        File file = new File(plugin.getDataFolder(), "shop.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        enabled = config.getBoolean("use", true);

        for (String key : config.getKeys(false)) {
            if ("use".equalsIgnoreCase(key)) {
                continue;
            }

            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            BigDecimal price = NumberParser.parseFlexibleDecimal(String.valueOf(section.get("price", "0")));
            if (price == null) {
                price = BigDecimal.ZERO;
            }

            ShopEntry.Action action = ShopEntry.Action.fromString(section.getString("action", "deduct"));
            List<String> commands = new ArrayList<>(section.getStringList("commands"));

            entries.put(key.toLowerCase(Locale.ROOT), new ShopEntry(key, price, action, commands));
        }
    }

    public Set<String> getIds() {
        return entries.keySet();
    }

    public ShopResult buy(Player player, String id) {
        if (!enabled) {
            return ShopResult.of(ShopResult.Status.DISABLED, "shop-disabled");
        }

        ShopEntry entry = entries.get(id.toLowerCase(Locale.ROOT));
        if (entry == null) {
            return ShopResult.of(ShopResult.Status.ITEM_NOT_FOUND, "item-not-found");
        }

        BigDecimal price = normalize(entry.price().abs());
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            return ShopResult.of(ShopResult.Status.INVALID_PRICE, "invalid-price");
        }

        if (entry.action() == ShopEntry.Action.DEDUCT) {
            if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                return ShopResult.of(ShopResult.Status.PLACEHOLDER_MISSING, "placeholder-missing");
            }

            BigDecimal balance = resolveBalance(player);
            if (balance == null) {
                return ShopResult.of(ShopResult.Status.BALANCE_PARSE_FAILED, price, null, "balance-parse-failed");
            }

            if (balance.compareTo(price) < 0) {
                return ShopResult.of(ShopResult.Status.NOT_ENOUGH, price, balance, "not-enough");
            }

            if (!runMoneyCommands(player, price, true)) {
                return ShopResult.of(ShopResult.Status.MONEY_COMMAND_FAILED, "command-failed");
            }
        } else {
            if (!runMoneyCommands(player, price, false)) {
                return ShopResult.of(ShopResult.Status.MONEY_COMMAND_FAILED, "command-failed");
            }
        }

        for (String command : entry.commands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), applyTokens(command, player, price, null));
        }

        return ShopResult.of(ShopResult.Status.SUCCESS, price, null, "buy-success");
    }

    private BigDecimal resolveBalance(Player player) {
        String placeholder = plugin.getConfig().getString("money.balance-placeholder", "%vault_eco_balance%");
        String parsed = PlaceholderAPI.setPlaceholders(player, placeholder);
        BigDecimal value = NumberParser.parseFlexibleDecimal(parsed);
        if (value == null) {
            plugin.getLogger().warning("Could not parse player balance from placeholder: " + parsed);
            return null;
        }
        return normalize(value);
    }

    private boolean runMoneyCommands(Player player, BigDecimal amount, boolean deduct) {
        List<String> list = plugin.getConfig().getStringList(deduct ? "money.deduct-commands" : "money.add-commands");
        if (list.isEmpty()) {
            return false;
        }

        for (String command : list) {
            String rendered = applyTokens(command, player, amount, null);
            boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rendered);
            if (!ok) {
                plugin.getLogger().warning("Money command failed: " + rendered);
                return false;
            }
        }
        return true;
    }

    public String applyTokens(String raw, Player player, BigDecimal price, BigDecimal balance) {
        String priceText = formatAmount(price);
        String balanceText = balance == null ? "0" : formatAmount(balance);
        return raw
                .replace("%player%", player.getName())
                .replace("%price%", priceText)
                .replace("%amount%", priceText)
                .replace("%balance%", balanceText);
    }

    public String formatAmount(BigDecimal amount) {
        return normalize(amount).stripTrailingZeros().toPlainString();
    }

    private BigDecimal normalize(BigDecimal value) {
        int scale = Math.max(0, plugin.getConfig().getInt("money.scale", 2));
        return value.setScale(scale, RoundingMode.DOWN);
    }
}
