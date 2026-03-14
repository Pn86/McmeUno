package uno.mcme.pnmoneyshop.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

public final class TextUtil {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private TextUtil() {
    }

    public static String color(String raw) {
        return ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
    }

    public static Component asComponent(String raw) {
        String colored = color(raw);
        return MiniMessage.miniMessage().deserialize(LEGACY.serialize(LEGACY.deserialize(colored)));
    }
}
