package cn.pn86.pnbooknews.util;

import org.bukkit.ChatColor;

public final class TextUtil {

    private TextUtil() {
    }

    public static String color(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
