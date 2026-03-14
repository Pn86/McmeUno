package uno.mcme.pnmoney.shop;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import uno.mcme.pnmoney.MoneyManager;
import uno.mcme.pnmoney.PnMoneyPlugin;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;

public class ShopManager {

    private final PnMoneyPlugin plugin;
    private final MoneyManager moneyManager;
    private final Map<String, ShopEntry> entries = new HashMap<>();
    private boolean enabled;

    public ShopManager(PnMoneyPlugin plugin, MoneyManager moneyManager) {
        this.plugin = plugin;
        this.moneyManager = moneyManager;
        reload();
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
            BigDecimal price = readPrice(section);
            List<String> commands = new ArrayList<>(section.getStringList("item"));
            entries.put(key, new ShopEntry(key, price, commands));
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Set<String> getIds() {
        return entries.keySet();
    }

    public ShopPurchaseResult purchase(Player player, String id) {
        if (!enabled) {
            return ShopPurchaseResult.of(ShopPurchaseStatus.DISABLED);
        }

        ShopEntry entry = entries.get(id);
        if (entry == null) {
            return ShopPurchaseResult.of(ShopPurchaseStatus.NOT_FOUND);
        }

        BigDecimal price = moneyManager.normalize(entry.price());
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            return ShopPurchaseResult.of(ShopPurchaseStatus.INVALID_PRICE);
        }

        BigDecimal currentBal = resolveBalanceByPapi(player);
        if (currentBal == null) {
            return ShopPurchaseResult.of(ShopPurchaseStatus.BALANCE_READ_FAILED);
        }

        if (currentBal.compareTo(price) < 0) {
            return ShopPurchaseResult.of(ShopPurchaseStatus.NOT_ENOUGH);
        }

        if (!moneyManager.takeBalance(player, price)) {
            return ShopPurchaseResult.of(ShopPurchaseStatus.DEDUCT_FAILED);
        }

        for (String cmd : entry.commands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
        }
        return ShopPurchaseResult.success(price);
    }

    private BigDecimal resolveBalanceByPapi(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            String parsed = PlaceholderAPI.setPlaceholders(player, "%pnmoney.bal%");
            BigDecimal byPapi = tryParseDecimal(parsed);
            if (byPapi != null) {
                return moneyManager.normalize(byPapi);
            }
            plugin.getLogger().warning("Failed to parse PAPI balance for " + player.getName() + ": " + parsed);
            return null;
        }
        return moneyManager.getBalance(player);
    }

    private BigDecimal readPrice(ConfigurationSection section) {
        Object raw = section.get("int");
        if (raw == null) {
            return BigDecimal.ZERO;
        }

        if (raw instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }

        BigDecimal parsed = tryParseDecimal(String.valueOf(raw));
        return parsed == null ? BigDecimal.ZERO : parsed;
    }

    private BigDecimal tryParseDecimal(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.trim().replace(",", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
