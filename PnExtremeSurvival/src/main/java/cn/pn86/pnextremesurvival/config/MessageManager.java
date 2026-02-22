package cn.pn86.pnextremesurvival.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MessageManager {

    private final JavaPlugin plugin;
    private FileConfiguration messages;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "message.yml");
        this.messages = YamlConfiguration.loadConfiguration(file);
    }

    public String raw(String path, String def) {
        return messages.getString(path, def);
    }

    public Component text(String path, String def, Map<String, String> placeholders) {
        String msg = raw(path, def);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return miniMessage.deserialize(msg);
    }

    public Component text(String path, String def) {
        return text(path, def, Collections.emptyMap());
    }

    public List<String> list(String path) {
        return messages.getStringList(path);
    }

    public void playSound(Player player, String path, Sound fallback, float volume, float pitch) {
        String name = raw(path, fallback.name());
        try {
            player.playSound(player.getLocation(), Sound.valueOf(name.toUpperCase()), volume, pitch);
        } catch (IllegalArgumentException ignored) {
            player.playSound(player.getLocation(), fallback, volume, pitch);
        }
    }
}
