package cn.pn86.pnwarp.placeholder;

import cn.pn86.pnwarp.service.WarpService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class PnWarpPlaceholderExpansion extends PlaceholderExpansion {
    private final WarpService warpService;

    public PnWarpPlaceholderExpansion(WarpService warpService) {
        this.warpService = warpService;
    }

    @Override
    public String getIdentifier() {
        return "pnwarp";
    }

    @Override
    public String getAuthor() {
        return "Pn86";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params.equalsIgnoreCase("total")) {
            return String.valueOf(warpService.allWarpsSorted().size());
        }
        if (params.equalsIgnoreCase("max") || params.equalsIgnoreCase("max_per_player")) {
            return String.valueOf(warpService.maxWarpsPerPlayer());
        }
        if (params.equalsIgnoreCase("owned") && player != null && player.getUniqueId() != null) {
            return String.valueOf(warpService.warpCountOf(player.getUniqueId()));
        }
        return "";
    }
}
