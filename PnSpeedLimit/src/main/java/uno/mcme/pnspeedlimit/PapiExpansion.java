package uno.mcme.pnspeedlimit;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uno.mcme.pnspeedlimit.model.SpeedType;

import java.util.Locale;

public class PapiExpansion extends PlaceholderExpansion {

    private final PnSpeedLimitPlugin plugin;
    private final SpeedLimitManager manager;

    public PapiExpansion(PnSpeedLimitPlugin plugin, SpeedLimitManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "pnsl";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Pn86";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        double speed = manager.getLastSpeed(player);
        SpeedType type = manager.getLastType(player);
        if (params.equalsIgnoreCase("speed")) {
            return manager.formatSpeed(speed, true);
        }
        if (params.equalsIgnoreCase("speednum")) {
            return manager.formatSpeed(speed, false);
        }
        if (params.equalsIgnoreCase("limit")) {
            return manager.formatSpeed(manager.getSpeedLimit(type), false);
        }
        if (params.equalsIgnoreCase("type")) {
            return type.name().toLowerCase(Locale.ROOT);
        }
        return null;
    }
}
