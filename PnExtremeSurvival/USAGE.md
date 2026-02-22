# PnExtremeSurvival 完整使用手册

## 1. 插件概述
PnExtremeSurvival 是一个自动化运行的我的世界极限生存小游戏插件。

核心目标：
- 自动等待玩家、自动开局、自动缩圈、自动判定胜利、自动重置地图。
- 支持单人/多人组队模式一键切换。
- 高度可自定义：`config.yml` 控制规则，`message.yml` 控制文本、计分板、ActionBar、音效。

## 2. 安装方式
1. 使用 Maven 打包：
   ```bash
   mvn -f PnExtremeSurvival/pom.xml clean package
   ```
2. 将生成的 `PnExtremeSurvival-1.0.0.jar` 放入服务器 `plugins/`。
3. 启动服务器，插件会生成默认配置文件。

## 3. 指令说明
### 玩家指令
- `/join`：参与游戏（默认进服自动参与，等待阶段会处于旁观者）
- `/leave`：退出本局参与（全程旁观者）

### 管理员指令（需要 `pnes.admin`）
- `/pnes start`：强制开始游戏
- `/pnes stop`：强制结束并恢复玩家状态、边界状态
- `/pnes resetworld`：踢出全员并关闭服务器，关闭后删除 world 下除 datapacks 外所有文件
- `/pnes reload`：重载配置
- `/pnes mode [solo/multiplayer]`：切换单人/多人模式

## 4. 自动流程
1. **等待阶段**：参与者数量达到最小值开始倒计时；满员时切换短倒计时。
2. **开局阶段**：
   - 玩家最大生命值设为配置值；
   - 切换生存模式并发放基础物资；
   - 传送到边界内安全高点；
   - 进入无敌时间。
3. **缩圈阶段**：无敌结束后开始从初始边长缩到最终边长。
4. **结算阶段**：剩余一个玩家/队伍后结束，烟花庆祝并倒计时后自动重置地图。
5. **地图重置**：服务端关机后自动清理世界目录（保留 datapacks）。

## 5. 配置文件说明
## `config.yml`
- `game.world`：游戏世界名
- `game.default-mode`：默认模式 `solo`/`multiplayer`
- `game.waiting`：等待人数和倒计时
- `game.start`：初始生命值、无敌时长
- `game.border`：缩圈起始/终点大小与时长
- `game.multiplayer.team-size`：组队人数
- `game.end.auto-reset-seconds`：结算后自动重置秒数

## `message.yml`
可改内容包括：
- 所有系统提示文本
- 开局标题 Title/SubTitle
- ActionBar 文本格式
- 计分板标题与行
- 音效枚举名（写 Bukkit/Paper Sound 名称）

## 6. 我额外增强的功能
1. **实时 HUD**：ActionBar 持续显示坐标、到中心距离、危险状态。
2. **动态计分板变量**：支持状态、模式、存活数、边界大小、缩圈进度。
3. **组队友伤保护**：多人模式同队无法互伤。
4. **开局沉浸反馈**：广播 + Title + 音效组合提示。
5. **异常容错**：音效名填错时自动使用后备音效，不会报错中断。

## 7. 注意事项
- `/pnes resetworld` 会触发关服并删除地图数据，请提前备份。
- 生产环境建议搭配自动拉起脚本（如 systemd、docker restart policy）以实现“关服重置后自动重启”。
- 若使用多世界，请确认 `game.world` 指向正确目标世界。
