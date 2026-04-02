# PnOreMine

Paper 1.21.1 自动矿场插件，支持矿场定时刷新、挖矿收益（Vault 经济）和 PlaceholderAPI。

## 功能
- 自动矿场：按配置时间自动将矿区重置为随机矿物。
- 挖矿赚钱：根据矿物配置给玩家发放经济收益（Vault）。
- 物品掉落模式：也可配置直接掉落物品。
- 世界保护：被矿场使用的世界中，仅矿区可破坏方块。
- PlaceholderAPI：`%pnoremine_time_矿场名%` 返回剩余刷新秒数。

## 指令
- `/pnom reload` 重载插件
- `/pnom list` 查看矿场列表
- `/pnom see [矿场名]` 查看矿场信息
- `/pnom reset [矿场名]` 手动刷新矿场

## 权限
- `pnoremine.admin` 使用管理命令
- `pnoremine.bypass` 绕过世界保护

## 配置说明
配置文件：`plugins/PnOreMine/config.yml`

```yml
mine:
  mine1:
    world: 'mine_world'
    xyz:
      a:
        x: -10
        y: 5
        z: -10
      b:
        x: 10
        y: 10
        z: 10
    time: '120'
    drop: 'vaule' # vaule/value 经济模式；item 物品掉落模式
    ore:
      - 'STONE:30:1'    # 方块ID:权重:收益(或掉落数量)
      - 'GOLD_ORE:5:3'
```

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
