# PnWarp 使用手册

PnWarp 是一个 Paper 1.21.1 的简单地标（Warp）插件，支持玩家创建自己的地标、GUI 菜单翻页浏览、延迟传送、数据持久化，以及 PlaceholderAPI 占位符。

## 1. 环境要求

- Java 21
- Paper 1.21.1
- （可选）PlaceholderAPI 2.11+

## 2. 指令说明

### 玩家指令

- `/addwarp [地标名] [地标简介] [地标图标(MC物品ID)]`
  - 在当前位置创建自己的地标。
- `/remwarp [地标名]`
  - 删除自己的地标（管理员可删除任意地标）。
- `/warps`
  - 打开地标菜单（6 排箱子，底部一排按钮/玻璃板）。
- `/gowarp [地标名]`
  - 直接传送到地标（支持等待时间与移动取消）。

### 管理员指令（`pnwarp.admin`）

- `/pnwp reload`
  - 重载 `config.yml`、`gui.yml`、`data.yml`。
- `/pnwp remwarp [地标名]`
  - 删除任意地标。
- `/pnwp remove [玩家名]`
  - 删除指定玩家全部地标。
- `/pnwp removeall`
  - 触发二次确认提示。
- `/pnwp removeall confirm`
  - 删除全部地标。

## 3. 权限

- `pnwarp.admin`
  - 默认 OP，允许执行管理命令。

## 4. 配置文件

### `config.yml`

可配置：

- `max-warps-per-player`：每个玩家最多可拥有地标数。
- `teleport.wait-seconds`：传送等待秒数（0 为秒传）。
- `teleport.*-sound`：开始/取消/成功音效。
- `messages.*`：所有消息文本。
- `default-warps`：默认地标（首次无地标数据时自动导入）。

### `gui.yml`

可配置：

- 菜单标题
- 地标物品名称、Lore 模板
- 底部按钮样式/位置（上一页、下一页、关闭、填充）

### `data.yml`

- 插件自动保存地标数据。
- 重启后地标仍然可用。

## 5. PlaceholderAPI 占位符

当服务器安装 PlaceholderAPI 后，PnWarp 自动注册以下占位符：

- `%pnwarp_total%`：全服地标总数
- `%pnwarp_owned%`：当前玩家拥有地标数
- `%pnwarp_max%` 或 `%pnwarp_max_per_player%`：每位玩家地标上限

## 6. 构建

在 `PnWarp` 目录执行：

```bash
mvn clean package
```

编译成功后，插件位于：

- `target/PnWarp-1.0.0.jar`

将其放入服务器 `plugins` 目录并重启即可。
