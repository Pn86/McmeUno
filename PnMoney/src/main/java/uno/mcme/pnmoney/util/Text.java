package uno.mcme.pnmoney.util;

import org.bukkit.ChatColor;

public final class Text {

    private Text() {}

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
