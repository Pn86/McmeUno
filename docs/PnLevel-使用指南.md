# PnLevel 插件使用指南（Paper 1.21.1）

## 1. 插件信息
- 插件名：`PnLevel`
- 版本：`1.0.0`
- 作者：`Pn86`
- 核心功能：等级系统、经验获取与升级、等级奖励、排行榜、GUI菜单、PAPI占位符。

## 2. 指令说明
### 管理员指令（`/pnlv`）
- `/pnlv reload`：重载全部配置（config/lang/exp/gift/gui）。
- 配置文件读取失败时，插件会在后台日志和在线管理员处提示具体错误原因；错误文件会被跳过，插件继续使用旧配置保持运行。

- `/pnlv list [页码]`：查看全部玩家等级信息（每页20条）。
- `/pnlv look <玩家>`：查看指定玩家等级/经验。
- `/pnlv set <玩家> <exp|level> <值>`：设置经验或等级（exp上限999）。
- `/pnlv add <玩家> <exp|level> <值>`：增加经验或等级（exp上限999）。
- `/pnlv remove <玩家> <exp|level> <值>`：减少经验或等级（exp最低0）。
- `/pnlv reset <玩家>`：重置玩家数据为初始等级/初始经验。

### 玩家指令
- `/level`：查看自己的等级、经验、距下一级差值。
- `/leveltop`：打开5排“金字塔”等级排行榜菜单（TOP 15）。
- `/levelgift`：打开等级奖励大菜单（分页、状态按钮、一键领取）。
- `/levelgift get`：直接领取当前全部可领取等级奖励。

## 3. 菜单功能设计

### 3.1 等级奖励菜单（分页）
- 大菜单按“从左到右、从上到下”展示每个等级的奖励按钮。
- 自动根据 `max-level` 计算总页数，即使最大等级很高也只渲染当前页，避免卡顿。
- 每个等级按钮有三种状态：
  - 已领取（灰）
  - 可领取（绿）
  - 未解锁（红）
- 支持上一页/下一页。
- 底部支持“一键领取全部可领取奖励”。

### 3.2 排行榜菜单（金字塔）
- 固定5排。
- 前15名按金字塔布局摆放（1-2-3-4-5层）。

### 3.3 返回按钮与安全执行
- 两个菜单都带返回按钮（左下或中下位置可配置）。
- 点击后玩家会临时以 OP 身份执行 `config.yml -> menu.return-command` 指令，然后自动还原原权限状态。

## 4. 安全性说明
- 菜单禁止取出、放入、拖拽物品：
  - 点击事件全取消。
  - 拖拽事件全取消。
  - 仅处理顶部菜单按钮点击。
- 奖励领取防重复：
  - 按 `claimed-levels` 精准判断每个等级是否已领。
  - 单级领取只领取当前点击等级，不会误把前面等级标记为已领取。
  - 一键领取仅领取“已达成且未领取”的等级，防止刷奖励。

## 5. 配置文件详解

## `config.yml`
用于控制等级规则与标签：
- `max-level`：最大等级。
- `initial-level`、`initial-exp`：新玩家初始数据。
- `level-exp.fixed`：统一每级所需经验（最常用，开启后所有等级一致）。
- `level-exp.default/increment/custom`：旧兼容模式（仅当 fixed <= 0 时生效）。
- `level-tags`：等级区间前缀标签（如`1-10`、`11-20`）。
- `menu.return-command`：菜单返回按钮执行的命令（无 `/`）。

> 升级判定逻辑：玩家当前等级为 N 时，比较当前 exp 是否达到“升到 N+1 所需经验”。

## `lang.yml`
可配置插件全部文本；每条文本都支持 `message/title/subtitle` 三类，并且每类可单独 `enable` 开关。默认主要只开启 message。
- 重载提示、找不到玩家、升级提示、经验提示、奖励提示。
- 奖励状态提示（已领取、未解锁、单级领取成功、一键领取成功）。
- 经验条字符：`bar-filled` 与 `bar-empty`（用于 `%pnlevel.expimg%`）。

## `exp.yml`
升级经验获取方案，可定义多条规则：
- `type` 支持：`time`（在线时长）、`kill`（击杀实体）、`destroy`（破坏方块）、`place`（放置方块）。
- `int` 为触发条件数组：
  - `time`：支持秒数（如`300`）或每天时间点（如`13:00`）。
  - `kill`：实体命名ID（如`minecraft:zombie`）。
  - `destroy/place`：方块命名ID（如`minecraft:diamond_block`）。
- `upexp`：触发后增加经验。
- `message/title/subtitle`：触发后给玩家提示（空字符串表示不发送）。

## `gift.yml`
等级奖励配置：
- `level.<等级>`：指定等级奖励（优先触发）。
- `all`：通用等级奖励（当该等级没有专属奖励时触发）。
- `message`：奖励提示文本。
- `action`：控制台执行指令，支持 `%player_name%` 与 `%level%` 变量。

## `gui.yml`
菜单外观配置：
- `common.return-name`：返回按钮名称。
- `gift.title`、`gift.rows`：奖励菜单标题与行数。
- `gift.content-slots`：奖励等级按钮展示槽位（完全自定义）。
- `gift.buttons.prev/next/claim-all/return`：所有功能按钮的槽位、材质、名称、Lore。
- `gift.level-item.claimed/claimable/locked`：三种状态的材质、名称、Lore。
- `top.rows/title/layout-slots`：排行榜行数、标题和金字塔槽位布局。
- `top.item`：排行按钮格式。
- `top.buttons.return`：返回按钮完整配置。

## `playerdata.yml`
玩家数据存档（UUID键值）：
- `name`：最后一次玩家名。
- `level`、`exp`：玩家等级/经验。
- `last-claimed-level`：历史兼容字段（当前主要用于统计）。
- `claimed-levels`：已领取过的等级列表（用于精准单级领取，不会误标记前置等级）。
- `daily-record`：按天时间点奖励记录，防止同一天重复领取。

## 6. PAPI占位符
- `%pnlevel.level%`：带标签的等级文本。
- `%pnlevel.levelnum%`：纯数字等级。
- `%pnlevel.exp%`：当前经验。
- `%pnlevel.explast%`：距下一级经验差。
- `%pnlevel.expimg%`：经验进度条（10格）。
- `%pnlevel.top.1%`：等级榜第1名（数字可改为2、3、4...）。
- `%pnlevel.top%`：默认等价第1名。

## 7. 安装与部署
1. 将编译后的 `PnLevel-1.0.0.jar` 放入服务器 `plugins/`。
2. 首次启动后自动生成全部配置。
3. 按需编辑配置后执行 `/pnlv reload` 即可热更新。
4. 若要启用占位符，确保已安装 PlaceholderAPI。
