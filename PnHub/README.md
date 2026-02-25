# PnHub

基于 Velocity 的全版本 Hub 传送插件。

## 指令
- `/hub`：玩家传送到 `config.yml` 的 `target-server`
- `/pnhub reload`：仅控制台可用，重载配置

## 功能
- 玩家每次进入代理时，会优先尝试连接 `target-server`
- 插件重启后会重新读取 `plugins/PnHub/config.yml`

## 构建
```bash
mvn -f PnHub/pom.xml clean package
```

生成的插件文件在：
- `PnHub/target/PnHub-1.0.0.jar`
