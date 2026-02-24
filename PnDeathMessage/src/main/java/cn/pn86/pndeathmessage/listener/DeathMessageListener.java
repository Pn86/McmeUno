package cn.pn86.pndeathmessage.listener;

import cn.pn86.pndeathmessage.PnDeathMessagePlugin;
import cn.pn86.pndeathmessage.config.NameConfigManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DeathMessageListener implements Listener {

    private final PnDeathMessagePlugin plugin;

    public DeathMessageListener(PnDeathMessagePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.deathMessage(null);

        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("broadcast.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        EntityDamageEvent lastDamage = player.getLastDamageCause();
        String causeKey = lastDamage != null ? lastDamage.getCause().name() : "UNKNOWN";

        ConfigurationSection section = config.getConfigurationSection("deathmessage." + causeKey);
        if (section == null) {
            section = config.getConfigurationSection("deathmessage.DEFAULT");
            if (section == null) {
                return;
            }
        }

        String attackName = resolveAttackName(player, event);
        String itemName = resolveItemName(event);

        String message = apply(resolveRandomText(section, "message", "&e%player%&f 死亡了"), player, attackName, itemName);
        String title = apply(resolveRandomText(section, "title", "none"), player, attackName, itemName);
        String subtitle = apply(resolveRandomText(section, "subtitle", "none"), player, attackName, itemName);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!"none".equalsIgnoreCase(message)) {
                online.sendMessage(color(message));
            }
            if (!"none".equalsIgnoreCase(title) || !"none".equalsIgnoreCase(subtitle)) {
                String finalTitle = "none".equalsIgnoreCase(title) ? "" : color(title);
                String finalSubtitle = "none".equalsIgnoreCase(subtitle) ? "" : color(subtitle);
                online.sendTitle(finalTitle, finalSubtitle, 10, 50, 20);
            }
        }
    }

    private String resolveAttackName(Player player, PlayerDeathEvent event) {
        NameConfigManager manager = plugin.getNameConfigManager();
        Entity killer = player.getKiller();

        if (killer == null && event.getDamageSource() != null) {
            killer = event.getDamageSource().getCausingEntity();
        }
        if (killer == null && event.getDamageSource() != null) {
            killer = event.getDamageSource().getDirectEntity();
        }

        if (killer == null) {
            return manager.getAttackNone();
        }

        // 玩家永远显示玩家名，不使用 attack.yml 映射
        if (killer instanceof Player killerPlayer) {
            return killerPlayer.getName();
        }

        if (killer.customName() != null) {
            return PlainTextComponentSerializer.plainText().serialize(killer.customName());
        }

        return manager.getAttackName(killer.getType());
    }

    private String resolveItemName(PlayerDeathEvent event) {
        NameConfigManager manager = plugin.getNameConfigManager();

        Entity source = event.getDamageSource() != null ? event.getDamageSource().getCausingEntity() : null;
        if (!(source instanceof LivingEntity livingEntity)) {
            return manager.getItemNone();
        }

        ItemStack item = livingEntity.getEquipment() != null ? livingEntity.getEquipment().getItemInMainHand() : null;
        if (item == null || item.getType() == Material.AIR) {
            return manager.getItemNone();
        }

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }

        return manager.getItemName(item.getType());
    }

    private String resolveRandomText(ConfigurationSection section, String path, String defaultValue) {
        Object raw = section.get(path);
        if (raw instanceof List<?> list && !list.isEmpty()) {
            int index = ThreadLocalRandom.current().nextInt(list.size());
            Object value = list.get(index);
            return value == null ? defaultValue : value.toString();
        }

        String single = section.getString(path);
        return single == null ? defaultValue : single;
    }

    private String apply(String text, Player player, String attack, String item) {
        return text
                .replace("%player%", player.getName())
                .replace("%attack%", attack)
                .replace("%item%", item);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
