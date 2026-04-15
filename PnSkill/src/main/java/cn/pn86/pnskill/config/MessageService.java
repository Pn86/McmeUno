package cn.pn86.pnskill.config;

import cn.pn86.pnskill.PnSkillPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class MessageService {
    private final PnSkillPlugin plugin;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    public MessageService(PnSkillPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        // no-op: read values on demand.
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(component(key, placeholders));
    }

    public Component component(String key) {
        return component(key, Map.of());
    }

    public Component component(String key, Map<String, String> placeholders) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String raw = plugin.getConfig().getString("messages." + key, key);
        return serializer.deserialize(applyPlaceholders(prefix + raw, placeholders));
    }

    public Component componentInline(String raw) {
        return serializer.deserialize(raw == null ? "" : raw);
    }

    private String applyPlaceholders(String text, Map<String, String> placeholders) {
        String out = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            out = out.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return out;
    }
}
