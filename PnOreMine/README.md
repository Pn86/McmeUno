# PnOreMine

Paper 1.21.1 自动矿场插件，支持矿场定时刷新、挖矿收益（Vault 经济）和 PlaceholderAPI。

## 功能
- 自动矿场：按配置时间自动将矿区重置为随机矿物，刷新前 10 秒全服预警，刷新时全服通报。
- 挖矿赚钱：根据矿物配置给玩家发放经济收益（Vault）。
- 物品掉落模式：也可配置直接掉落物品。
- 世界保护：仅配置到 `mine` 的世界会启用保护与规则，未配置世界不会受影响。
- 出生点限制：矿场重置后，该矿场世界内的玩家统一传送到 `spawn` 坐标。
- 区域 PVP：`pvp: true` 时，仅在矿区 `xyz` 的平面范围（X/Z）允许 PVP；矿区外仍禁止。
- PlaceholderAPI：`%pnoremine_time_矿场名%` 返回剩余刷新秒数。

## 指令
- `/pnom reload` 重载插件
- `/pnom list` 查看矿场列表
- `/pnom see [矿场名]` 查看矿场信息
- `/pnom reset [矿场名]` 手动刷新矿场

## 权限
- `pnoremine.admin` 使用管理命令
- `pnoremine.bypass` 绕过世界保护

## 配置说明（简化）
配置文件：`plugins/PnOreMine/config.yml`

```yml
mine:
  mine1:
    world: 'mine_world'
    pos1: '-10 5 -10'   # 起点
    pos2: '10 10 10'    # 终点
    spawn: '0 80 0'     # 矿区出生点，矿场重置时传送全世界玩家到此
    pvp: false          # 是否允许PVP（仅矿区 X/Z 平面内允许）
    time: '120'
    drop: 'vaule'       # vaule/value 经济模式；item 物品掉落模式
    ore:
      - 'STONE 30 1'    # 方块ID 权重 收益(或掉落数量)
      - 'GOLD_ORE 5 3'
```

> 兼容旧格式：`xyz.a/xyz.b` 和 `STONE:30:1` 仍可继续使用。

## 语言显示模式（可选）
`language` 下任意文本都可在前缀添加显示标签（可组合）：
- `[message]` 聊天消息（默认）
- `[actionbar]` 动作栏
- `[title]` 标题
- `[subtitle]` 副标题

示例：
- `[message]&a重载完成`
- `[actionbar]&e矿场即将刷新`
- `[title][subtitle]&6矿场刷新||&f请注意安全`

不写标签时默认按聊天消息发送。

## 错误恢复机制
- 每次成功加载配置后会保存一份 `config-lastgood.yml` 备份。
- 若新配置存在错误，插件会：
  1. 在控制台输出详细错误。
  2. 向在线管理员发送报错消息。
  3. 自动恢复到上一份可用配置并继续运行。

## 安装
1. 将生成的 `PnOreMine-1.0.0.jar` 放入 `plugins/`。
2. 确保服务器安装了 Vault 与任意经济实现（如 EssentialsX Economy）。
3. 可选安装 PlaceholderAPI 以启用变量。
4. 重启服务器或 `/pnom reload`。
