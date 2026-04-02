package uno.mcme.pnoremine.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uno.mcme.pnoremine.PnOreMinePlugin;
import uno.mcme.pnoremine.mine.MineRegion;

public class PnOreMinePlaceholder extends PlaceholderExpansion {

    private final PnOreMinePlugin plugin;

    public PnOreMinePlaceholder(PnOreMinePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "pnoremine";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Pn86";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.startsWith("time_")) {
            String mineName = params.substring("time_".length());
            MineRegion mine = plugin.getMineManager().findMine(mineName);
            return mine == null ? "0" : String.valueOf(mine.getRemainingSeconds());
        }
        return null;
    }
}
