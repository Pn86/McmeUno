package cn.pn86.pnextremesurvival.service;

import cn.pn86.pnextremesurvival.PnExtremeSurvivalPlugin;
import cn.pn86.pnextremesurvival.data.PlayerDataRepository;
import cn.pn86.pnextremesurvival.data.PlayerLifeData;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.Optional;

public class LimitedLifeService {

    private final PnExtremeSurvivalPlugin plugin;
    private final PlayerDataRepository repository;

    public LimitedLifeService(PnExtremeSurvivalPlugin plugin, PlayerDataRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public void loadOnlinePlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            loadPlayer(player);
        }
    }

    public void loadPlayer(Player player) {
        if (!isLimitedLifeEnabled()) {
            return;
        }

        Optional<PlayerLifeData> loaded = repository.load(player.getUniqueId());
        if (loaded.isPresent()) {
            PlayerLifeData data = loaded.get();
            applyPlayerState(player, data.maxHealth(), data.permanentlyDead());
        } else {
            double defaultHealth = plugin.getConfig().getDouble("limited-life.default-health", 20.0);
            double maxHealth = clampHealth(defaultHealth);
            repository.save(player.getUniqueId(), player.getName(), maxHealth, false);
            applyPlayerState(player, maxHealth, false);
        }
    }

    public double getPlayerMaxHealth(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        return attribute == null ? 20.0 : attribute.getBaseValue();
    }

    public void addHealth(Player player, double amount) {
        if (!isLimitedLifeEnabled()) {
            return;
        }
        double finalHealth = clampHealth(getPlayerMaxHealth(player) + amount);
        boolean dead = isPermanentDead(player) && finalHealth <= 0.0;
        if (isPermanentDead(player) && finalHealth > 0.0) {
            dead = false;
            player.setGameMode(GameMode.SURVIVAL);
        }
        applyPlayerState(player, finalHealth, dead);
        repository.save(player.getUniqueId(), player.getName(), finalHealth, dead);
    }

    public void removeHealth(Player player, double amount) {
        if (!isLimitedLifeEnabled() || isExempt(player)) {
            return;
        }
        double next = getPlayerMaxHealth(player) - amount;
        processHealthAfterDeath(player, next);
    }

    public void handleKillReward(Player killer) {
        if (!isLimitedLifeEnabled()) {
            return;
        }
        double reward = plugin.getConfig().getDouble("limited-life.kill-reward-health", 2.0);
        addHealth(killer, reward);
    }

    public void processHealthAfterDeath(Player player, double rawHealth) {
        double min = plugin.getConfig().getDouble("limited-life.min-health", 0.0);
        boolean spectatorOnZero = plugin.getConfig().getBoolean("limited-life.enable-permanent-spectator-on-zero", true);
        double next = clampToRange(rawHealth, min, plugin.getConfig().getDouble("limited-life.max-health", 40.0));

        boolean permanentlyDead = false;
        if (spectatorOnZero && min <= 0.0 && rawHealth <= 0.0) {
            permanentlyDead = true;
            next = 0.0;
            player.getInventory().clear();
            player.setGameMode(GameMode.SPECTATOR);
        }

        applyPlayerState(player, next, permanentlyDead);
        repository.save(player.getUniqueId(), player.getName(), next, permanentlyDead);
    }

    public boolean isPermanentDead(Player player) {
        return repository.load(player.getUniqueId()).map(PlayerLifeData::permanentlyDead).orElse(false);
    }

    public void reviveToOneHeart(Player player) {
        double revivedHealth = 2.0;
        applyPlayerState(player, revivedHealth, false);
        repository.save(player.getUniqueId(), player.getName(), revivedHealth, false);
        player.setGameMode(GameMode.SURVIVAL);
        if (player.isDead()) {
            player.spigot().respawn();
        }
    }

    private void applyPlayerState(Player player, double maxHealth, boolean permanentlyDead) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(Math.max(0.0001, maxHealth));
        }
        if (maxHealth > 0) {
            player.setHealth(Math.min(player.getHealth(), maxHealth));
        }
        if (permanentlyDead) {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    public boolean isLimitedLifeEnabled() {
        return plugin.getConfig().getBoolean("features.limited-life", true);
    }

    public boolean shouldPenalizeThisDeath(Player player, Player killer) {
        if (!isLimitedLifeEnabled() || isExempt(player)) {
            return false;
        }
        boolean onlyPlayerKill = plugin.getConfig().getBoolean("limited-life.only-player-kill-death-penalty", false);
        return !onlyPlayerKill || killer != null;
    }

    private boolean isExempt(Player player) {
        return plugin.getConfig().getBoolean("limited-life.exempt-creative", true)
                && player.getGameMode() == GameMode.CREATIVE;
    }

    private double clampHealth(double health) {
        double max = plugin.getConfig().getDouble("limited-life.max-health", 40.0);
        double min = plugin.getConfig().getDouble("limited-life.min-health", 0.0);
        return clampToRange(health, min, max);
    }

    private double clampToRange(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
