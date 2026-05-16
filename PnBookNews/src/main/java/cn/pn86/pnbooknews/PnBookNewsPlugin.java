package cn.pn86.pnbooknews;

import cn.pn86.pnbooknews.command.PnBookNewsCommand;
import cn.pn86.pnbooknews.listener.AuthMeBookListener;
import cn.pn86.pnbooknews.listener.JoinBookListener;
import cn.pn86.pnbooknews.util.TextUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PnBookNewsPlugin extends JavaPlugin {

    private boolean authMeHooked;
    private boolean placeholderApiHooked;
    private AuthMeBookListener authMeBookListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        registerCommand();
        getServer().getPluginManager().registerEvents(new JoinBookListener(this), this);
        reloadBookNews();
        getLogger().info("PnBookNews 已启用。");
    }

    @Override
    public void onDisable() {
        unregisterAuthMeListener();
    }

    public boolean reloadBookNews() {
        try {
            reloadConfig();
            unregisterAuthMeListener();
            this.placeholderApiHooked = isPlaceholderApiUsable();
            this.authMeHooked = false;

            if (isAuthMeCompatEnabled()) {
                this.authMeBookListener = AuthMeBookListener.register(this);
                this.authMeHooked = this.authMeBookListener != null;
            }

            logHookStatus();
            return true;
        } catch (Exception ex) {
            getLogger().severe("重载配置失败: " + ex.getMessage());
            return false;
        }
    }

    public void openNewsBookLater(Player player) {
        if (!getConfig().getBoolean("settings.enabled", true)) {
            return;
        }
        long delay = Math.max(0L, getConfig().getLong("settings.open-delay-ticks", 20L));
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                openNewsBook(player);
            }
        }, delay);
    }

    public void openNewsBook(Player player) {
        ItemStack book = createBook(player);
        player.openBook(book);
    }

    private ItemStack createBook(Player player) {
        FileConfiguration config = getConfig();
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) {
            return book;
        }

        meta.setTitle(formatForPlayer(player, config.getString("book.title", "PnBookNews")));
        meta.setAuthor(formatForPlayer(player, config.getString("book.author", "Pn86")));

        List<String> pages = config.getStringList("book.pages");
        if (pages.isEmpty()) {
            pages = List.of("&c公告书没有配置任何页面。\n&7请编辑 config.yml 的 book.pages。");
        }
        for (String page : pages) {
            meta.addPage(formatForPlayer(player, page));
        }

        book.setItemMeta(meta);
        return book;
    }

    public String msg(String key) {
        return TextUtil.color(getConfig().getString("language." + key, ""));
    }

    public String getPrefix() {
        return msg("prefix");
    }

    public String formatForPlayer(Player player, String text) {
        String result = text == null ? "" : text;
        if (placeholderApiHooked) {
            result = PlaceholderAPI.setPlaceholders(player, result);
        }
        return TextUtil.color(result);
    }

    public boolean shouldShowOnJoin() {
        return getConfig().getBoolean("settings.show-on-every-join", true);
    }

    public boolean isAuthMeCompatEnabled() {
        return getConfig().getBoolean("settings.authme-compat", true);
    }

    public boolean isAuthMeHooked() {
        return authMeHooked;
    }

    private void unregisterAuthMeListener() {
        if (authMeBookListener != null) {
            HandlerList.unregisterAll(authMeBookListener);
            authMeBookListener = null;
        }
    }

    private boolean isPlaceholderApiUsable() {
        return getConfig().getBoolean("settings.placeholderapi", true)
            && getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    private void registerCommand() {
        PluginCommand command = getCommand("pnbn");
        if (command == null) {
            throw new IllegalStateException("pnbn command not found in plugin.yml");
        }
        PnBookNewsCommand executor = new PnBookNewsCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter((sender, cmd, label, args) -> {
            if (args.length == 1 && sender.hasPermission("pnbooknews.admin")) {
                List<String> completions = new ArrayList<>();
                if ("reload".startsWith(args[0].toLowerCase())) {
                    completions.add("reload");
                }
                return completions;
            }
            return Collections.emptyList();
        });
    }

    private void logHookStatus() {
        if (placeholderApiHooked) {
            getLogger().info(TextUtil.color(msg("papi-hooked")));
        } else {
            getLogger().info(TextUtil.color(msg("papi-not-found")));
        }

        if (authMeHooked) {
            getLogger().info(TextUtil.color(msg("authme-hooked")));
        } else {
            getLogger().info(TextUtil.color(msg("authme-not-found")));
        }
    }
}
