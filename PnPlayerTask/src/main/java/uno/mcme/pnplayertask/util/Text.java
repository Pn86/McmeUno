package uno.mcme.pnplayertask.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class Text {
    private Text() {}
    public static String color(String text) { return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text); }
    public static List<String> color(List<String> lines) { List<String> out = new ArrayList<>(); if (lines != null) for (String line : lines) out.add(color(line)); return out; }
    public static String papi(Player player, String text) { return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") && player != null ? PlaceholderAPI.setPlaceholders(player, text) : text; }
    public static List<String> papi(Player player, List<String> lines) { List<String> out = new ArrayList<>(); if (lines != null) for (String line : lines) out.add(papi(player, line)); return out; }
}
