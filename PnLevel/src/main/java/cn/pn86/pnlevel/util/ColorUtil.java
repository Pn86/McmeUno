package cn.pn86.pnlevel.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ColorUtil {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private ColorUtil() {}

    public static String color(String text) {
        if (text == null) {
            return "";
        }
        return LEGACY.serialize(LEGACY.deserialize(text));
    }

    public static Component component(String text) {
        String legacy = text == null ? "" : text;
        return LEGACY.deserialize(legacy);
    }

    public static Component miniOrLegacy(String text) {
        if (text == null || text.isBlank()) {
            return Component.empty();
        }
        if (text.contains("<") && text.contains(">")) {
            try {
                return MINI.deserialize(text);
            } catch (Exception ignored) {
            }
        }
        return component(text);
    }
}
