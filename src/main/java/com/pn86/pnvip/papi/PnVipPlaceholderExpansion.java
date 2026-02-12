package com.pn86.pnvip.papi;

import com.pn86.pnvip.PnVipPlugin;
import com.pn86.pnvip.model.PlayerVipRecord;
import com.pn86.pnvip.model.VipDefinition;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.Comparator;
import java.util.Map;

public class PnVipPlaceholderExpansion extends PlaceholderExpansion {
    private final PnVipPlugin plugin;

    public PnVipPlaceholderExpansion(PnVipPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "pnvip";
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
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || player.getUniqueId() == null || params == null) {
            return "";
        }

        PlayerVipRecord record = plugin.getDataStore().getRecord(player.getUniqueId());
        if (record == null || record.getVipExpireAt().isEmpty()) {
            return "";
        }

        Map.Entry<String, Long> current = record.getVipExpireAt().entrySet().stream()
                .filter(e -> e.getValue() <= 0 || e.getValue() > System.currentTimeMillis())
                .max(Comparator.comparingLong(e -> e.getValue() <= 0 ? Long.MAX_VALUE : e.getValue()))
                .orElse(null);

        if (current == null) {
            return "";
        }

        VipDefinition definition = plugin.getVipManager().getDefinition(current.getKey());
        return switch (params.toLowerCase()) {
            case "name" -> definition == null ? "" : plugin.getVipManager().color(definition.displayName());
            case "key" -> current.getKey();
            case "expire" -> plugin.getVipManager().formatExpireAt(current.getValue());
            default -> "";
        };
    }
}
