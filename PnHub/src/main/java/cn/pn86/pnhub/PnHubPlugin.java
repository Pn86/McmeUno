package cn.pn86.pnhub;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Plugin(
        id = "pnhub",
        name = "PnHub",
        version = "1.0.0",
        authors = {"McmeUno"}
)
public final class PnHubPlugin {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;

    private ConfigManager configManager;

    @Inject
    public PnHubPlugin(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        this.configManager = new ConfigManager(dataDirectory, logger);
        try {
            saveDefaultConfig();
            configManager.reload();
        } catch (IOException e) {
            logger.error("无法初始化 PnHub 配置文件", e);
        }

        proxyServer.getCommandManager().register("hub", new HubCommand());
        proxyServer.getCommandManager().register("pnhub", new PnHubCommand());
        logger.info("PnHub 已启用");
    }

    @Subscribe
    public void onChooseInitialServer(PlayerChooseInitialServerEvent event) {
        connectToHub(event.getPlayer(), true);
    }

    private void connectToHub(Player player, boolean silentOnSuccess) {
        if (configManager == null) {
            player.sendMessage(color("&c插件尚未完成初始化，请联系管理员。"));
            return;
        }

        String targetName = configManager.getTargetServer();
        Optional<RegisteredServer> targetServer = proxyServer.getServer(targetName);

        if (targetServer.isEmpty()) {
            String template = configManager.getMessage("server-not-found");
            player.sendMessage(color(template.replace("{server}", targetName)));
            return;
        }

        RegisteredServer current = player.getCurrentServer().map(c -> c.getServer()).orElse(null);
        if (current != null && current.getServerInfo().getName().equalsIgnoreCase(targetName)) {
            if (!silentOnSuccess) {
                player.sendMessage(color(configManager.getMessage("already-in-hub")));
            }
            return;
        }

        if (!silentOnSuccess) {
            String msg = configManager.getMessage("connecting").replace("{server}", targetName);
            player.sendMessage(color(msg));
        }

        player.createConnectionRequest(targetServer.get()).fireAndForget();
    }

    private void saveDefaultConfig() throws IOException {
        Files.createDirectories(dataDirectory);
        Path configPath = dataDirectory.resolve("config.yml");
        if (Files.exists(configPath)) {
            return;
        }

        try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
            if (in == null) {
                throw new IOException("默认配置 config.yml 不存在");
            }
            Files.copy(in, configPath);
        }
    }

    private Component color(String text) {
        return LEGACY_SERIALIZER.deserialize(text);
    }

    private final class HubCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            if (!(source instanceof Player player)) {
                source.sendMessage(color(configManager.getMessage("player-only")));
                return;
            }
            connectToHub(player, false);
        }
    }

    private final class PnHubCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            String[] args = invocation.arguments();
            if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
                invocation.source().sendMessage(color(configManager.getMessage("usage")));
                return;
            }

            if (!(invocation.source() instanceof ConsoleCommandSource)) {
                invocation.source().sendMessage(color(configManager.getMessage("console-only")));
                return;
            }

            try {
                configManager.reload();
                invocation.source().sendMessage(color(configManager.getMessage("reload-success")));
                logger.info("PnHub 配置文件已重载");
            } catch (IOException e) {
                logger.error("重载 PnHub 配置文件失败", e);
                invocation.source().sendMessage(color(configManager.getMessage("reload-failed")));
            }
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return true;
        }

        @Override
        public java.util.List<String> suggest(Invocation invocation) {
            if (invocation.arguments().length == 1) {
                return java.util.List.of("reload");
            }
            return java.util.List.of();
        }
    }
}
