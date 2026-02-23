# PnSpeedLimit 使用手册

## 1. 插件简介

- 插件名：`PnSpeedLimit`
- 作者：`Pn86`
- 适用核心：`Paper 1.21.1`
- 功能：限制玩家的**移动速度**、**飞行/鞘翅滑翔速度**、**击退与爆炸推动速度**。

## 2. 安装步骤

1. 将构建得到的 `PnSpeedLimit-1.0.0.jar` 放入服务器 `plugins/` 目录。
2. 启动（或重启）服务器，生成默认配置文件 `plugins/PnSpeedLimit/config.yml`。
3. 按需修改配置后执行 `/pnsl reload` 重载。

## 3. 指令说明

> 需要权限：`pnspeedlimit.admin`（默认 OP）

- `/pnsl whitelist [add/remove] [玩家名]`
  - 添加或移除白名单。
  - 白名单玩家不受任何限速影响。

- `/pnsl limit [all/move/fly/repel] [速度值]`
  - `all`：全局总上限（最终上限会取 `all` 与对应类型上限的较小值）
  - `move`：普通移动限速
  - `fly`：飞行/鞘翅/滑翔限速
  - `repel`：击退/爆炸推动限速

- `/pnsl use [on/off]`
  - 全局开关限速功能。

- `/pnsl reload`
  - 重载配置到内存。

## 4. 配置文件说明（config.yml）

### 4.1 `settings`

- `settings.enabled`：是否启用限速（`true/false`）

### 4.2 `limits`

- `limits.all`：全局总上限（m/s）
- `limits.move`：地面/普通移动上限（m/s）
- `limits.fly`：飞行/鞘翅上限（m/s）
- `limits.repel`：击退与爆炸推动上限（m/s）

### 4.3 `warning`

- `warning.percent`：达到限速多少比例触发警告（如 `0.8` 即 80%）
- `warning.cooldown-ms`：警告冷却毫秒
- `warning.messages`：警告文本列表，支持变量：
  - `%player%` 玩家名
  - `%speed%` 当前速度（含单位）
  - `%speednum%` 当前速度（仅数字）
  - `%limit%` 当前状态限速
  - `%type%` 当前状态类型

### 4.4 `punishment`

- `punishment.commands`：玩家超速时由控制台执行的命令列表。
- 支持占位符 `%player%` 等（同上）。

### 4.5 `whitelist`

- 白名单玩家列表，不受限速。

### 4.6 `messages`

- 插件所有文本提示（支持 `&` 颜色代码）。

### 4.7 `placeholders`

- 占位符说明区域，便于服主查看与维护。

## 5. PlaceholderAPI 变量

当安装了 PlaceholderAPI 后，可使用：

- `%pnsl_speed%`：玩家速度（如 `12.34m/s`）
- `%pnsl_speednum%`：玩家速度数值（如 `12.34`）
- `%pnsl_limit%`：玩家当前状态限速值
- `%pnsl_type%`：玩家当前速度状态（`move` / `fly` / `repel`）

## 6. 数据持久化说明

- 使用命令修改白名单、限速开关、限速数值后会立即写入 `config.yml`。
- 服务器重启后仍可保留上次设置。

## 7. 常见问题

1. **为什么我的玩家被频繁警告？**
   - 适当提高 `warning.percent`（如 0.9）或增加 `warning.cooldown-ms`。
2. **为什么飞行速度限制看起来更严格？**
   - 实际生效上限为 `min(limits.all, limits.fly)`。
3. **如何只限制击退？**
   - 将 `move/fly` 设置较高值，仅保留较低的 `repel`。
