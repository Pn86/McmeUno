package cn.pn86.pnextremesurvival.listener;

import cn.pn86.pnextremesurvival.PnExtremeSurvivalPlugin;
import cn.pn86.pnextremesurvival.service.LimitedLifeService;
import cn.pn86.pnextremesurvival.service.LootChestService;
import cn.pn86.pnextremesurvival.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.ArrayList;

public class PlayerDeathListener implements Listener {

    private final PnExtremeSurvivalPlugin plugin;
    private final LimitedLifeService limitedLifeService;
    private final LootChestService lootChestService;

    public PlayerDeathListener(PnExtremeSurvivalPlugin plugin,
                               LimitedLifeService limitedLifeService,
                               LootChestService lootChestService) {
        this.plugin = plugin;
        this.limitedLifeService = limitedLifeService;
        this.lootChestService = lootChestService;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Player killer = player.getKiller();

        if (lootChestService.isEnabled()) {
            var drops = new ArrayList<>(event.getDrops());
            event.getDrops().clear();
            lootChestService.createLootChest(player, player.getLocation(), drops);
        }

        if (limitedLifeService.shouldPenalizeThisDeath(player, killer)) {
            double penalty = plugin.getConfig().getDouble("limited-life.death-penalty-health", 2.0);
            limitedLifeService.removeHealth(player, penalty);

            if (limitedLifeService.isPermanentDead(player)) {
                MessageUtil.send(plugin, player, "perma-dead");
            }
        }

        if (killer != null && killer != player) {
            limitedLifeService.handleKillReward(killer);
        }
    }
}
