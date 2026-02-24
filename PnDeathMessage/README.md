# PnDeathMessage 使用手册

## 1. 插件信息
- 插件名：`PnDeathMessage`
- 作者：`Pn86`
- 适配版本：Paper/Spigot 1.21.1（Java 21）

## 2. 功能说明
- 自动关闭原版死亡消息（防止与自定义消息重叠）。
- 按死亡方式显示自定义 `message/title/subtitle`。
- `title` / `subtitle` 是广播给 **所有在线玩家**，不是只给死亡玩家。
- 支持变量：`%player%`、`%attack%`、`%item%`。
- 支持 `message/title/subtitle` 使用列表随机一条，实现随机死亡信息效果。
- 支持独立配置攻击者名称（`attack.yml`）和物品名称（`item.yml`）。
- `attack.yml` 与 `item.yml` 会在插件启动时自动补全全部实体ID和全部物品ID。
- 玩家击杀时 `%attack%` 永远显示玩家名，不读取 attack.yml。
- 插件卸载/停用后，原版死亡消息会恢复（因为仅在监听事件时拦截）。

## 3. 指令
- `/pndm reload`：重载配置。
- 权限：`pndeathmessage.reload`（默认 OP）。

## 4. 配置文件
### config.yml
- 路径：`plugins/PnDeathMessage/config.yml`
- `broadcast.enabled`：是否启用自定义播报。
- `deathmessage.<死亡方法>.message/title/subtitle`：每种死亡方式对应的内容。
- `title` 或 `subtitle` 填 `none` 表示不显示。
- 默认所有死亡方式的 `title` 和 `subtitle` 都是 `none`。

#### 随机文本格式示例
```yml
message:
  - '&e%player%&f 死亡信息1'
  - '&e%player%&f 死亡信息2'
title:
  - 'none'
  - '&c你倒下了'
subtitle:
  - 'none'
  - '&7凶手: &e%attack%'
```

### attack.yml
- 路径：`plugins/PnDeathMessage/attack.yml`
- `none`：未知攻击者显示内容。
- `names.<ENTITY_TYPE>`：实体名称映射，用于 `%attack%`。
- 插件会自动补齐全部实体ID。

### item.yml
- 路径：`plugins/PnDeathMessage/item.yml`
- `none`：未知物品显示内容。
- `names.<MATERIAL>`：物品名称映射，用于 `%item%`。
- 插件会自动补齐全部物品ID。

## 5. 变量说明
- `%player%`：死亡玩家名。
- `%attack%`：攻击来源（玩家永远显示玩家名；非玩家优先实体自定义名，否则读 attack.yml）。
- `%item%`：攻击者主手物品（优先物品自定义名，其次 item.yml 映射）。

## 6. 打包
在 `PnDeathMessage` 目录执行：

```bash
mvn clean package
```

打包后将 `target/PnDeathMessage-1.0.0.jar` 放入服务器 `plugins` 目录，重启服务器即可。
