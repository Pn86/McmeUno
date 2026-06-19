# PnPlayerTask

PnPlayerTask 是 Pn86 开发的 Paper 1.21.1+ 玩家任务插件。插件会按配置间隔自动检测玩家是否完成 `task.yml` 中的任务目标，完成后发送 Title、Subtitle 与聊天提示，并提醒玩家使用 `/task` 打开任务页面领取奖励。

## 功能
- `/task` 打开高度可配置的任务 GUI。
- `/pnplayertask reload` 重载 `config.yml`、`task.yml`、`gui.yml` 与玩家数据。
- `/pnplayertask reset [玩家名]` 重置指定玩家任务数据。
- 每 `settings.check-interval-seconds` 秒自动检测一次在线玩家任务完成状态，默认 5 秒。
- 任务完成后自动发送 Title、Subtitle 和聊天框提示；每个任务可在 `task.yml` 单独覆盖完成提示。
- 支持 PlaceholderAPI：任务条件、GUI 文本、完成提示、奖励命令与插件占位符均可解析。
- 玩家数据写入 `player.yml`，服务器重启后继续保留进度、完成状态、领取状态与下一次刷新时间。
- 支持一次性、每日、每周、每月、完成后 N 分钟刷新任务。
- 提供 `PnPlayerTaskApi` 供其他插件扩展。

## 指令
- `/task`：打开任务页面。
- `/pnplayertask reload`：重载插件。
- `/pnplayertask reset [玩家名]`：重置玩家任务数据。

## 权限
- `pnplayertask.use`：允许玩家使用 `/task`，默认所有玩家拥有。
- `pnplayertask.admin`：允许使用管理命令，默认 OP 拥有。

## 配置文件
### config.yml
配置语言文本和自动检测间隔，所有文本支持 `&` 颜色代码。

```yml
settings:
  check-interval-seconds: 5
messages:
  complete-title: "&e%task_name%"
  complete-subtitle: "&a任务已完成！输入 /task 领取奖励"
  complete-message: "&a任务 &e%task_name% &a已完成！输入 &e/task &a打开任务页面领取奖励。"
```

### gui.yml
配置 GUI 标题、大小、任务槽位、翻页按钮、关闭按钮、填充物品，以及不同任务状态追加的 lore 或替换物品。

### player.yml
插件自动维护玩家数据，请不要在服务器运行时手动修改。包含：
- `completed`：已检测完成的任务。
- `claimed`：已领取奖励的任务。
- `completed-at`：任务完成时间。
- `refresh-at`：可刷新任务的下一次刷新时间。
- `progress`：玩家使用、拾取、丢弃物品的累计进度。

### task.yml
示例：
```yml
TestTask:
  item: oak_log
  name: "&e&l获得木头"
  lore:
    - "&7获得一个木头"
  retime: none
  complete-title: "&e%task_name%"
  complete-subtitle: "&a任务完成！输入 /task 领取奖励"
  complete-message: "&a你完成了 &e%task_name% &a，输入 &e/task &a打开页面领取奖励。"
  if:
    - "[have_item] oak_log = 1"
    - "[int] %player_money% > 499"
    - "[use_item] apple = 5"
    - "[get_item] coal = 3"
    - "[drop_item] coal = 3"
  action:
    - "eco give %player_name% 1000"
```

字段说明：
- `item`：任务在 GUI 中显示的物品材质。
- `name`：任务显示名。
- `lore`：任务描述。
- `retime`：刷新规则。
- `complete-title`：任务完成时显示的 Title，不设置则使用 `config.yml` 默认值。
- `complete-subtitle`：任务完成时显示的 Subtitle，不设置则使用 `config.yml` 默认值。
- `complete-message`：任务完成时发送的聊天消息，不设置则使用 `config.yml` 默认值。
- `if`：任务条件列表，全部满足才会被自动检测为完成。
- `action`：领取后以控制台身份执行的命令，支持 `%player_name%`、`%player%` 和 PlaceholderAPI。

兼容用户给出的 `aciton` 拼写；若同时存在，优先读取正确的 `action`。

## retime 刷新规则
- `none`：一次性任务，不刷新。
- `day`：每天 0 点刷新。
- `week`：每周一 0 点刷新。
- `month`：每月 1 日 0 点刷新。
- `100`：完成后 100 分钟刷新；数字可改为任意正整数分钟数。

任务刷新时会清除该任务的 `completed`、`claimed`、`completed-at`、`refresh-at`，并清除该任务使用到的 `use_item/get_item/drop_item` 进度。

## 条件类型
- `[have_item] oak_log = 1`：比较玩家背包中指定物品数量。
- `[int] %player_money% > 499`：比较两个数字/PlaceholderAPI 结果。
- `[use_item] apple = 5`：比较玩家使用/食用指定物品累计数量。
- `[get_item] coal = 3`：比较玩家拾取指定物品累计数量。
- `[drop_item] coal = 3`：比较玩家丢弃指定物品累计数量。

支持比较符：`>`、`>=`、`<`、`<=`、`=`、`!=`。

## PlaceholderAPI
如果服务器安装 PlaceholderAPI，插件会自动在以下位置解析：
- GUI 标题、按钮名称、lore、任务名称与描述。
- `[int]` 条件的左右两侧。
- 任务完成 Title、Subtitle、聊天提示。
- `action` 奖励命令。

插件提供以下占位符：
- `%pnplayertask_taskyes%`：完成的任务数量。
- `%pnplayertask_taskall%`：一共的任务数量。
- `%pnplayertask_taskno%`：未完成的任务数量。
- `%pnplayertask_tasktime%`：距离下一次任务刷新剩余秒数；没有可刷新任务时返回 `none`。
- `%pnplayertask_tasktimeend%`：下一次任务刷新后的日期时间；没有可刷新任务时返回 `none`。

## 开发者 API
```java
PnPlayerTaskPlugin plugin = (PnPlayerTaskPlugin) Bukkit.getPluginManager().getPlugin("PnPlayerTask");
if (plugin != null) {
    boolean complete = plugin.isComplete(player, "TestTask");
    boolean claimed = plugin.isClaimed(player, "TestTask");
    plugin.claim(player, "TestTask");
}
```

也可以依赖接口 `uno.mcme.pnplayertask.api.PnPlayerTaskApi` 获取任务列表、检测状态和触发领取。

## 安装
1. 使用 Maven 构建：`mvn package`。
2. 将 `target/PnPlayerTask-1.0.0.jar` 放入服务器 `plugins` 目录。
3. 启动服务器生成默认配置。
4. 修改配置后执行 `/pnplayertask reload`。
