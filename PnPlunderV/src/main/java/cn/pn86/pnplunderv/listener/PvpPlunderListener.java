package cn.pn86.pnplunderv.listener;

import cn.pn86.pnplunderv.PnPlunderVPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.text.DecimalFormat;

public class PvpPlunderListener implements Listener {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("0.##");

    private final PnPlunderVPlugin plugin;

    public PvpPlunderListener(PnPlunderVPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) {
            return;
        }

        Economy economy = plugin.getEconomy();
        if (economy == null) {
            return;
        }

        double percent = plugin.getConfig().getDouble("plunder.percent", 10.0D);
        if (percent <= 0) {
            return;
        }

        double victimBalance = economy.getBalance(victim);
        if (victimBalance <= 0) {
            return;
        }

        double amount = victimBalance * (percent / 100D);
        if (amount <= 0) {
            return;
        }

        economy.withdrawPlayer(victim, amount);
        economy.depositPlayer(killer, amount);

        String subtitleTemplate = plugin.getConfig().getString(
                "subtitle.content",
                "&6{killer} &f击杀了 &c{victim}&f，掠夺金币: &e{amount}"
        );
        String subtitle = subtitleTemplate
                .replace("{killer}", killer.getName())
                .replace("{victim}", victim.getName())
                .replace("{amount}", MONEY_FORMAT.format(amount));

        int fadeIn = plugin.getConfig().getInt("subtitle.fade-in", 10);
        int stay = plugin.getConfig().getInt("subtitle.stay", 50);
        int fadeOut = plugin.getConfig().getInt("subtitle.fade-out", 10);

        String coloredSubtitle = ChatColor.translateAlternateColorCodes('&', subtitle);
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendTitle("", coloredSubtitle, fadeIn, stay, fadeOut);
        }
    }
}
