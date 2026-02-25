package cn.pn86.pnhub;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigManager {
    private static final String DEFAULT_TARGET = "lobby";

    private final Path dataDirectory;
    private final Logger logger;

    private String targetServer = DEFAULT_TARGET;
    private Map<String, String> messages = new LinkedHashMap<>();

    public ConfigManager(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    public void reload() throws IOException {
        Path configPath = dataDirectory.resolve("config.yml");

        Yaml yaml = new Yaml(new SafeConstructor());
        Map<String, Object> root;

        try (InputStream in = Files.newInputStream(configPath)) {
            Object loaded = yaml.load(in);
            if (!(loaded instanceof Map<?, ?> map)) {
                throw new IOException("config.yml 格式错误：根节点必须是映射");
            }
            root = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    root.put(entry.getKey().toString(), entry.getValue());
                }
            }
        }

        this.targetServer = readString(root, "target-server", DEFAULT_TARGET);

        Map<String, String> loadedMessages = new LinkedHashMap<>();
        loadedMessages.put("player-only", "&c该命令仅玩家可用。");
        loadedMessages.put("console-only", "&c该命令仅控制台可用。");
        loadedMessages.put("usage", "&e用法: /pnhub reload");
        loadedMessages.put("reload-success", "&aPnHub 配置重载成功。");
        loadedMessages.put("reload-failed", "&cPnHub 配置重载失败，请查看控制台日志。");
        loadedMessages.put("server-not-found", "&c目标服务器 &e{server} &c不存在，请联系管理员。");
        loadedMessages.put("connecting", "&a正在将你传送到 &e{server}&a ...");
        loadedMessages.put("already-in-hub", "&e你已经在大厅服务器。"
        );

        Object messagesSection = root.get("messages");
        if (messagesSection instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    loadedMessages.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
        }

        this.messages = loadedMessages;
        logger.info("PnHub 配置加载完成，目标服务器: {}", targetServer);
    }

    public String getTargetServer() {
        return targetServer;
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "");
    }

    private String readString(Map<String, Object> root, String key, String fallback) {
        Object value = root.get(key);
        return value == null ? fallback : value.toString();
    }
}
