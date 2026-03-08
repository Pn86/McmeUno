package cn.pn86.pnwarp.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public final class TextUtil {
    private TextUtil() {
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    public static List<String> color(List<String> lines) {
        List<String> output = new ArrayList<>();
        for (String line : lines) {
            output.add(color(line));
        }
        return output;
    }

    public static String stripColors(String input) {
        if (input == null) {
            return "";
        }
        String withSectionColors = ChatColor.translateAlternateColorCodes('&', input);
        return ChatColor.stripColor(withSectionColors) == null
                ? withSectionColors
                : ChatColor.stripColor(withSectionColors);
    }
}
