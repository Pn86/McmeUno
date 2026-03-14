package uno.mcme.pnmoney.shop;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import uno.mcme.pnmoney.PnMoneyPlugin;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;

public class ShopManager {

    private final PnMoneyPlugin plugin;
    private FileConfiguration config;

    public ShopManager(PnMoneyPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "shop.yml");
        config = YamlConfiguration.loadConfiguration(file);
    }

    public boolean isEnabled() {
        return config.getBoolean("use", true);
    }

    public Optional<ShopEntry> getEntry(String id) {
        ConfigurationSection section = config.getConfigurationSection(id);
        if (section == null) {
            return Optional.empty();
        }
        BigDecimal price = BigDecimal.valueOf(section.getDouble("int", 0.0));
        List<String> commands = section.getStringList("item");
        return Optional.of(new ShopEntry(id, price, commands));
    }

    public Set<String> getIds() {
        Set<String> ids = new HashSet<>();
        for (String key : config.getKeys(false)) {
            if (!"use".equalsIgnoreCase(key)) {
                ids.add(key);
            }
        }
        return ids;
    }
}
