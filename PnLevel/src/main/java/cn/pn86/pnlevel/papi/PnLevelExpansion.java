package cn.pn86.pnlevel.papi;

import cn.pn86.pnlevel.PnLevelPlugin;
import cn.pn86.pnlevel.data.PlayerLevelData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.List;

public class PnLevelExpansion extends PlaceholderExpansion {
    private final PnLevelPlugin plugin;

    public PnLevelExpansion(PnLevelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "pnlevel";
    }

    @Override
    public String getAuthor() {
        return "Pn86";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String getRequiredPlugin() {
        return plugin.getName();
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(org.bukkit.entity.Player player, String params) {
        return onRequest(player, params);
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) return "";
        if (params.startsWith("top")) {
            int rank = 1;
            String[] split = params.split("\\.");
            if (split.length >= 2) {
                rank = parseInt(split[1], 1);
            }
            List<PlayerLevelData> top = plugin.getLevelManager().top();
            if (rank <= 0 || rank > top.size()) return "-";
            PlayerLevelData data = top.get(rank - 1);
            return data.getLastName() + " Lv." + data.getLevel();
        }

        if (player == null || player.getUniqueId() == null) return "0";
        PlayerLevelData data = plugin.getPlayerDataManager().getOrCreate(player.getUniqueId(), player.getName() == null ? "Unknown" : player.getName());
        int need = plugin.getLevelManager().getRequiredExpForLevel(data.getLevel());

        return switch (params.toLowerCase()) {
            case "level" -> plugin.getLevelManager().getLevelPrefix(data.getLevel()) + data.getLevel();
            case "levelnum" -> String.valueOf(data.getLevel());
            case "exp" -> String.valueOf(data.getExp());
            case "explast" -> String.valueOf(Math.max(0, need - data.getExp()));
            case "expimg" -> buildBar(data.getExp(), need);
            default -> null;
        };
    }

    int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String buildBar(int exp, int need) {
        int filled = Math.max(0, Math.min(10, (int) Math.floor((exp * 10.0) / Math.max(1, need))));
        return plugin.msg("bar-filled").repeat(filled) + plugin.msg("bar-empty").repeat(10 - filled);
    }
}
