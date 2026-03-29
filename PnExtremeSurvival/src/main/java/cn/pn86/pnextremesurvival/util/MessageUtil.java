package cn.pn86.pnextremesurvival.util;

import cn.pn86.pnextremesurvival.PnExtremeSurvivalPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class MessageUtil {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private MessageUtil() {
    }

    public static void send(PnExtremeSurvivalPlugin plugin, CommandSender sender, String key) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String content = plugin.getConfig().getString("messages." + key, key);
        Component component = SERIALIZER.deserialize(prefix + content);
        sender.sendMessage(component);
    }

    public static void sendRaw(CommandSender sender, String text) {
        sender.sendMessage(SERIALIZER.deserialize(text));
    }

    public static String format(PnExtremeSurvivalPlugin plugin, String key) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String content = plugin.getConfig().getString("messages." + key, key);
        return prefix + content;
    }
}
