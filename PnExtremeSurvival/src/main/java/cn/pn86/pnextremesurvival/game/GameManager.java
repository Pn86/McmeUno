package cn.pn86.pnextremesurvival.game;

import cn.pn86.pnextremesurvival.PnExtremeSurvivalPlugin;
import cn.pn86.pnextremesurvival.config.MessageManager;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class GameManager {

    private final PnExtremeSurvivalPlugin plugin;
    private final MessageManager messageManager;
    private final Set<UUID> optedOut = new HashSet<>();
    private final Random random = new Random();

    private GameState state = GameState.WAITING;
    private String mode = "solo";
    private int stateTimer = 0;
    private int shrinkElapsed = 0;
    private BukkitTask tickTask;
    private BukkitTask hudTask;
    private Scoreboard scoreboard;
    private Objective objective;

    private int minPlayers;
    private int maxPlayers;
    private int countdownSeconds;
    private int fullCountdownSeconds;
    private int invincibleSeconds;
    private int shrinkSeconds;
    private int endResetSeconds;
    private int mapSize;
    private int finalMapSize;
    private double maxHealth;
    private int teamSize;
    private World gameWorld;

    public GameManager(PnExtremeSurvivalPlugin plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        reload();
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfig();
        this.minPlayers = cfg.getInt("game.waiting.min-players", 8);
        this.maxPlayers = cfg.getInt("game.waiting.max-players", 32);
        this.countdownSeconds = cfg.getInt("game.waiting.countdown", 60);
        this.fullCountdownSeconds = cfg.getInt("game.waiting.full-countdown", 15);
        this.invincibleSeconds = cfg.getInt("game.start.invincible-seconds", 120);
        this.shrinkSeconds = cfg.getInt("game.border.shrink-seconds", 1800);
        this.endResetSeconds = cfg.getInt("game.end.auto-reset-seconds", 30);
        this.mapSize = cfg.getInt("game.border.start-size", 1000);
        this.finalMapSize = cfg.getInt("game.border.end-size", 16);
        this.maxHealth = cfg.getDouble("game.start.max-health", 40.0);
        this.teamSize = cfg.getInt("game.multiplayer.team-size", 3);
        this.mode = cfg.getString("game.default-mode", "solo").toLowerCase();
        this.gameWorld = Bukkit.getWorld(cfg.getString("game.world", "world"));
        if (gameWorld == null) {
            gameWorld = Bukkit.getWorlds().getFirst();
        }
        setupBoard();
    }

    private void setupBoard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        scoreboard = manager.getNewScoreboard();
        objective = scoreboard.registerNewObjective("pnes", Criteria.DUMMY,
                messageManager.text("scoreboard.title", "<gold>PnExtremeSurvival"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public void startLoops() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        hudTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateHud, 20L, 20L);
    }

    public void shutdown() {
        if (tickTask != null) tickTask.cancel();
        if (hudTask != null) hudTask.cancel();
    }

    public void setOptOut(Player player, boolean value) {
        if (value) {
            optedOut.add(player.getUniqueId());
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(messageManager.text("player.leave", "<red>你已退出本局，当前为旁观者。"));
        } else {
            optedOut.remove(player.getUniqueId());
            if (state == GameState.WAITING || state == GameState.COUNTDOWN) {
                player.setGameMode(GameMode.SPECTATOR);
            }
            player.sendMessage(messageManager.text("player.join", "<green>你已加入本局。"));
        }
    }

    public boolean isParticipating(Player p) {
        return !optedOut.contains(p.getUniqueId());
    }

    public void onJoin(Player player) {
        if (state == GameState.WAITING || state == GameState.COUNTDOWN) {
            if (isParticipating(player)) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        } else {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    private void tick() {
        switch (state) {
            case WAITING -> tickWaiting();
            case COUNTDOWN -> tickCountdown();
            case INVINCIBLE -> tickInvincible();
            case SHRINKING -> tickShrinking();
            case ENDING -> tickEnding();
        }
        checkWinCondition();
    }

    private void tickWaiting() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(scoreboard);
            if (isParticipating(p)) p.setGameMode(GameMode.SPECTATOR);
        }
        if (getAliveParticipants().size() >= minPlayers) {
            state = GameState.COUNTDOWN;
            stateTimer = getAliveParticipants().size() >= maxPlayers ? fullCountdownSeconds : countdownSeconds;
            broadcast("game.countdown.start", "<yellow>倒计时开始：{seconds}秒", Map.of("seconds", String.valueOf(stateTimer)));
        }
    }

    private void tickCountdown() {
        int alive = getAliveParticipants().size();
        if (alive < minPlayers) {
            state = GameState.WAITING;
            broadcast("game.countdown.cancel", "<red>人数不足，倒计时取消。", Map.of());
            return;
        }
        if (alive >= maxPlayers && stateTimer > fullCountdownSeconds) stateTimer = fullCountdownSeconds;
        if (stateTimer <= 0) {
            startGame();
            return;
        }
        if (stateTimer <= 10 || stateTimer % 10 == 0) {
            broadcast("game.countdown.ticking", "<gold>游戏将在 {seconds} 秒后开始", Map.of("seconds", String.valueOf(stateTimer)));
        }
        stateTimer--;
    }

    private void startGame() {
        state = GameState.INVINCIBLE;
        stateTimer = invincibleSeconds;
        shrinkElapsed = 0;
        WorldBorder border = gameWorld.getWorldBorder();
        border.setCenter(0, 0);
        border.setSize(mapSize);
        border.setWarningDistance(2);
        border.setWarningTime(15);
        border.setDamageAmount(1.0);
        border.setDamageBuffer(0);

        List<Player> participants = getAliveParticipants();
        if ("multiplayer".equalsIgnoreCase(mode)) assignTeams(participants);

        for (Player p : participants) {
            p.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
            p.setHealth(maxHealth);
            p.setFoodLevel(20);
            p.setSaturation(20f);
            p.getInventory().clear();
            p.getInventory().addItem(new ItemStack(Material.STONE_AXE));
            p.getInventory().addItem(new ItemStack(Material.STONE_PICKAXE));
            p.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 32));
            p.setGameMode(GameMode.SURVIVAL);
            p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, invincibleSeconds * 20, 10, false, false, true));
            teleportSafe(p, participants);
            p.sendTitle(messageManager.text("title.start", "<green><bold>生存开始！"),
                    messageManager.text("title.invincible", "<yellow>无敌时间 {seconds} 秒", Map.of("seconds", String.valueOf(invincibleSeconds))),
                    10, 60, 20);
            messageManager.playSound(p, "sounds.game-start", Sound.EVENT_RAID_HORN, 1f, 1f);
        }
        broadcast("game.start", "<green>游戏开始！", Map.of());
    }

    private void assignTeams(List<Player> participants) {
        Team existing;
        for (Team team : scoreboard.getTeams()) {
            team.unregister();
        }
        int idx = 1;
        Team current = null;
        for (int i = 0; i < participants.size(); i++) {
            if (i % teamSize == 0) {
                existing = scoreboard.registerNewTeam("team_" + idx++);
                existing.setAllowFriendlyFire(false);
                existing.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OWN_TEAM);
                current = existing;
            }
            current.addEntry(participants.get(i).getName());
        }
    }

    private void teleportSafe(Player player, List<Player> participants) {
        if ("multiplayer".equalsIgnoreCase(mode)) {
            Team team = scoreboard.getEntryTeam(player.getName());
            if (team != null) {
                List<Player> mates = participants.stream().filter(p -> team.hasEntry(p.getName())).toList();
                Player leader = mates.getFirst();
                if (!leader.equals(player)) {
                    player.teleport(leader.getLocation());
                    return;
                }
            }
        }
        for (int i = 0; i < 30; i++) {
            double half = mapSize / 2.0 - 5;
            double x = (random.nextDouble() * 2 - 1) * half;
            double z = (random.nextDouble() * 2 - 1) * half;
            int y = gameWorld.getHighestBlockYAt((int) x, (int) z) + 2;
            Location loc = new Location(gameWorld, x, y, z);
            if (loc.getBlock().isPassable() && loc.clone().add(0, -1, 0).getBlock().getType().isSolid()) {
                player.teleport(loc);
                return;
            }
        }
        player.teleport(new Location(gameWorld, 0, gameWorld.getHighestBlockYAt(0, 0) + 2, 0));
    }

    private void tickInvincible() {
        if (stateTimer <= 0) {
            state = GameState.SHRINKING;
            gameWorld.getWorldBorder().setSize(finalMapSize, shrinkSeconds);
            broadcast("game.shrink.start", "<red>无敌结束，开始缩圈！", Map.of());
            return;
        }
        if (stateTimer <= 10 || stateTimer % 30 == 0) {
            broadcast("game.invincible.ticking", "<aqua>无敌剩余 {seconds} 秒", Map.of("seconds", String.valueOf(stateTimer)));
        }
        stateTimer--;
    }

    private void tickShrinking() {
        shrinkElapsed++;
        if (shrinkElapsed >= shrinkSeconds) {
            state = GameState.SHRINKING;
        }
    }

    private void tickEnding() {
        if (stateTimer <= 0) {
            resetWorldAndShutdown();
            return;
        }
        stateTimer--;
    }

    private void checkWinCondition() {
        if (!(state == GameState.INVINCIBLE || state == GameState.SHRINKING)) return;
        List<Player> alive = getAliveParticipants();
        if (alive.isEmpty()) {
            finishGame(Collections.emptyList());
            return;
        }
        if ("multiplayer".equalsIgnoreCase(mode)) {
            Set<Team> teamsAlive = alive.stream()
                    .map(p -> scoreboard.getEntryTeam(p.getName()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (teamsAlive.size() <= 1) finishGame(alive);
        } else {
            if (alive.size() <= 1) finishGame(alive);
        }
    }

    private void finishGame(List<Player> winners) {
        state = GameState.ENDING;
        stateTimer = endResetSeconds;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setGameMode(GameMode.SPECTATOR);
            p.teleport(new Location(gameWorld, 0, gameWorld.getHighestBlockYAt(0, 0) + 2, 0));
        }
        if (winners.isEmpty()) {
            broadcast("game.end.no-winner", "<gray>本局无人获胜。", Map.of());
            return;
        }
        String winnerNames = winners.stream().map(Player::getName).collect(Collectors.joining(", "));
        broadcast("game.end.winner", "<gold>胜利者: {winner}", Map.of("winner", winnerNames));
        for (Player winner : winners) {
            Firework fw = (Firework) winner.getWorld().spawnEntity(winner.getLocation(), EntityType.FIREWORK_ROCKET);
            FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().withColor(Color.FUCHSIA, Color.RED).flicker(true).trail(true).build());
            meta.setPower(1);
            fw.setFireworkMeta(meta);
        }
    }

    public void forceStart() {
        if (state == GameState.WAITING || state == GameState.COUNTDOWN) {
            startGame();
        }
    }

    public void forceStop() {
        state = GameState.WAITING;
        WorldBorder border = gameWorld.getWorldBorder();
        border.reset();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20.0);
            p.setHealth(Math.min(20.0, p.getHealth()));
            p.getActivePotionEffects().forEach(effect -> p.removePotionEffect(effect.getType()));
            p.setGameMode(GameMode.SPECTATOR);
            p.getInventory().clear();
        }
        broadcast("game.force-stop", "<red>游戏已强制结束并恢复。", Map.of());
    }

    public void resetWorldAndShutdown() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.kick(messageManager.text("game.reset.kick", "<red>地图重置中，服务器即将关闭。"));
        }
        File worldFolder = gameWorld.getWorldFolder();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> wipeWorld(worldFolder)));
        Bukkit.shutdown();
    }

    private void wipeWorld(File worldFolder) {
        File[] files = worldFolder.listFiles();
        if (files == null) return;
        for (File child : files) {
            if (child.getName().equalsIgnoreCase("datapacks")) continue;
            try {
                Path path = child.toPath();
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {
                            }
                        });
            } catch (IOException ignored) {
            }
        }
    }

    public void setMode(String mode) {
        this.mode = mode;
        plugin.getConfig().set("game.default-mode", mode);
        plugin.saveConfig();
    }

    public String getMode() {
        return mode;
    }

    private void updateHud() {
        if (scoreboard == null || objective == null) return;
        scoreboard.getEntries().forEach(scoreboard::resetScores);

        List<String> lines = messageManager.list("scoreboard.lines");
        Map<String, String> vars = new HashMap<>();
        vars.put("state", state.name());
        vars.put("alive", String.valueOf(getAliveParticipants().size()));
        vars.put("players", String.valueOf(getParticipants().size()));
        vars.put("mode", mode);
        vars.put("timer", String.valueOf(stateTimer));
        vars.put("border", String.format("%.1f", gameWorld.getWorldBorder().getSize()));
        vars.put("progress", String.valueOf((int) ((Math.min(shrinkElapsed, shrinkSeconds) / (double) Math.max(1, shrinkSeconds)) * 100)));

        int score = lines.size();
        for (String raw : lines) {
            String line = raw;
            for (Map.Entry<String, String> e : vars.entrySet()) line = line.replace("{" + e.getKey() + "}", e.getValue());
            objective.getScore(ChatColor.translateAlternateColorCodes('&', line)).setScore(score--);
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(scoreboard);
            Location l = p.getLocation();
            double dist = Math.hypot(l.getX(), l.getZ());
            double limit = gameWorld.getWorldBorder().getSize() / 2.0;
            String danger = dist >= limit - 10 ? "危险" : "安全";
            Map<String, String> ph = Map.of(
                    "x", String.valueOf((int) l.getX()),
                    "y", String.valueOf((int) l.getY()),
                    "z", String.valueOf((int) l.getZ()),
                    "distance", String.format("%.1f", dist),
                    "danger", danger
            );
            Component action = messageManager.text("actionbar.status", "<gray>X:{x} Y:{y} Z:{z} <red>| 距中心:{distance} <yellow>{danger}", ph);
            p.sendActionBar(action);
        }
    }

    public List<Player> getParticipants() {
        return Bukkit.getOnlinePlayers().stream().filter(this::isParticipating).toList();
    }

    public List<Player> getAliveParticipants() {
        return Bukkit.getOnlinePlayers().stream().filter(this::isParticipating)
                .filter(p -> !p.isDead())
                .filter(p -> p.getGameMode() != GameMode.SPECTATOR)
                .toList();
    }

    private void broadcast(String path, String def, Map<String, String> placeholders) {
        Component msg = messageManager.text(path, def, placeholders);
        Bukkit.broadcast(msg);
    }
}
