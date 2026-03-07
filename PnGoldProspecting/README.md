# PnGoldProspecting

Paper 1.21.1 淘金插件，支持管理员创建可配置的淘金方块、权重战利品、命令战利品、实时存档与重载。

## 基础信息
- 插件名：`PnGoldProspecting`
- 作者：`Pn86`
- 指令前缀：`/pngp`
- 权限：`pngp.admin`（默认 OP）

## 指令
> 全部为管理员指令

- `/pngp help [页码]`
  - 查看详细帮助页（分页显示）。
- `/pngp creat [ID]`
  - 在玩家所指方块上方创建淘金方块。
- `/pngp move [ID]`
  - 将指定淘金方块移动到玩家所指方块上方。
- `/pngp delete [ID]`
  - 删除淘金方块并删除对应数据文件。
- `/pngp skin [ID] [gravel/sand]`
  - 设置外观为可疑沙砾或可疑沙子。
- `/pngp additem [ID] [自定义物品ID] [权重(可选)] [指令(可选)]`
  - **不填权重时默认 0**。
  - **不填指令**：使用玩家主手物品作为战利品（保留完整 NBT）。
  - **填写指令**：该战利品改为“命令战利品”，不会发放玩家物品；触发时控制台执行该指令。
  - 命令支持 `%player%` 占位符。
  - 同一个“自定义物品ID”允许重复添加（会生成唯一键）。
- `/pngp listitem [ID]`
  - 查看指定淘金方块所有战利品（唯一键、ID、物品显示名、权重、命令）。
- `/pngp removeitem [ID] [物品ID或唯一键]`
  - 删除指定战利品（优先按唯一键删除，否则按物品ID删除第一条匹配）。
- `/pngp resettime [ID] [时间(秒)]`
  - 设置该淘金方块自动重置时间。
- `/pngp list`
  - 列出所有淘金方块。
- `/pngp look [ID]`
  - 查看指定淘金方块详细信息。
- `/pngp reload`
  - 重载配置和数据，并重置全部淘金方块状态。

## 行为说明

### 1) 淘金工具限制与原版刷洗流程
- 只能使用 `defaults.required-tool` 指定工具进行淘金（默认 `BRUSH`）。
- 淘金为持续刷洗，不是瞬间触发。
- 可在 `defaults.brushing-duration-ticks` 调整持续时长（默认 `40` ticks ≈ 2 秒）。
- 刷洗粒子会生成在“朝向玩家的一面”而不是仅顶部。
- 刷洗粒子颜色：沙砾白色、沙子淡黄色。

### 2) 淘金完成后的方块形态
- 刷洗完成后，淘金方块会变为普通方块。
- 可在 `config.yml` 配置：
  - `defaults.opened-block.sand`（默认 `SAND`）
  - `defaults.opened-block.gravel`（默认 `GRAVEL`）
- 自动重置后会恢复为可疑方块外观。

### 3) 命令战利品与显示物
- 普通战利品：掉落并可被玩家拾取。
- 命令战利品：不会给玩家实际奖励物品，但会显示一个临时掉落物用于“掏出展示”。
- 临时展示物可在 `config.yml` 配置：
  - `defaults.command-loot-display-item`（默认 `DIAMOND`）

### 4) 保护机制
- 淘金方块不可被玩家破坏、不可被火烧/流体/爆炸/活塞等替换或破坏。
- 只能通过 `/pngp delete [ID]` 删除。

### 5) 实时存储与重载
- 每个淘金方块独立保存至：
  - `plugins/PnGoldProspecting/data/<ID>.yml`
- 创建、移动、修改、战利品变更、淘金状态变更都会立即保存。
- `/pngp reload` 后会把所有淘金方块重置为未淘开状态。

## 配置文件

### `config.yml`
- `defaults.skin`: 默认皮肤（`sand/gravel`）
- `defaults.reset-time-seconds`: 默认重置秒数
- `defaults.required-tool`: 允许淘金工具
- `defaults.brushing-duration-ticks`: 刷洗完成所需时长（tick）
- `defaults.command-loot-display-item`: 命令战利品的展示物材质
- `defaults.opened-block.sand`: 沙子类型淘金完成后替换为的普通方块
- `defaults.opened-block.gravel`: 沙砾类型淘金完成后替换为的普通方块
- `messages.*`: 所有指令提示文本

### `data/<ID>.yml`
每个淘金方块对应一个文件，记录：
- ID
- 坐标
- 皮肤
- 重置时间
- 已淘开状态
- 战利品列表（支持物品/NBT、权重、命令、唯一键）

