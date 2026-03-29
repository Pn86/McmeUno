# PnExtremeSurvival 使用说明

> 适用版本：Paper 1.21.1（Java 21）

## 1. 插件简介
PnExtremeSurvival 是一个“极限生存”玩法插件，核心功能：

1. **有限生命系统**
   - 玩家死亡会扣除最大生命上限（默认每次扣 1♥）。
   - 玩家击杀玩家会增加最大生命上限（默认每次加 1♥）。
   - 最大生命默认封顶 40（20♥）。
   - 生命归零后可进入永久旁观（可在配置中关闭/限制）。
   - 创造模式玩家默认不受有限生命影响。

2. **死亡战利品箱系统**
   - 玩家死亡后，物品不会散落一地。
   - 插件会在死亡点（或附近可放置位置）自动生成箱子并存放掉落物。
   - 若附近无法生成，会回退到最高处位置并强制处理。
   - 死亡玩家会收到箱子坐标提示。

---

## 2. 安装方法
1. 将构建好的 `PnExtremeSurvival-*.jar` 放入服务器 `plugins/` 目录。
2. 启动或重启服务器。
3. 首次启动后会生成：
   - `plugins/PnExtremeSurvival/config.yml`
   - `plugins/PnExtremeSurvival/database.db`

---

## 3. 指令说明
主命令：`/pnes`

- `/pnes reload`
  - 作用：重载 `config.yml`。
  - 权限：`pnes.admin`

- `/pnes see [玩家]`
  - 作用：查看自己或目标玩家当前最大生命。
  - 权限：`pnes.see`（或 `pnes.admin`）

- `/pnes add [玩家] [生命]`
  - 作用：给目标玩家增加最大生命。
  - 输入单位：**心（♥）**，例如输入 `1` 表示 +1♥。
  - 权限：`pnes.admin`

- `/pnes remove [玩家] [生命]`
  - 作用：扣减目标玩家最大生命。
  - 输入单位：**心（♥）**，例如输入 `1` 表示 -1♥。
  - 权限：`pnes.admin`

- `/pnes spawn [玩家]`
  - 作用：将“彻底死亡/旁观”的玩家恢复为 **1♥** 并复活到生存模式。
  - 权限：`pnes.admin`

---

## 4. 权限节点
- `pnes.admin`
  - 管理权限（reload/add/remove/spawn）。
  - 默认：OP
- `pnes.see`
  - 查看生命信息权限（see）。
  - 默认：true

---

## 5. 配置说明（config.yml）
配置文件已提供中文注释，重点项如下：

- `features.limited-life`
  - 是否启用有限生命系统。

- `features.death-loot-chest`
  - 是否启用死亡战利品箱。

- `limited-life.default-health`
  - 新玩家默认最大生命（点数），`20.0 = 10♥`。

- `limited-life.max-health`
  - 最大生命封顶（点数），`40.0 = 20♥`。

- `limited-life.min-health`
  - 最低生命下限。
  - 设为 `0.0` 允许生命归零。
  - 设为 `>0` 可阻止归零（常用于关闭永久旁观玩法）。

- `limited-life.only-player-kill-death-penalty`
  - `true` 时仅“玩家击杀玩家”触发死亡扣血。
  - `false` 时所有死亡方式都扣血。

- `limited-life.enable-permanent-spectator-on-zero`
  - 生命归零后是否进入永久旁观。

- `limited-life.exempt-creative`
  - 创造模式玩家是否免疫有限生命影响。

- `death-loot.search-radius`
  - 原地不可放置箱子时，附近搜索范围（方块）。

---

## 6. 数据存储与安全
- 玩家数据持久化在 `database.db`（SQLite）中。
- 生命变更会实时写入数据库，减少服务器崩溃导致的数据丢失风险。
- 建议定期备份 `plugins/PnExtremeSurvival/` 目录。

---

## 7. 常见问题
1. **玩家死后没有掉落战利品箱？**
   - 检查 `features.death-loot-chest` 是否为 `true`。
   - 某些保护插件可能拦截方块放置，请检查保护规则。

2. **不希望玩家彻底旁观怎么办？**
   - 将 `limited-life.min-health` 设置为大于 0（例如 `2.0`）。
   - 或将 `limited-life.enable-permanent-spectator-on-zero` 设置为 `false`。

3. **想只让 PVP 死亡扣血？**
   - 将 `limited-life.only-player-kill-death-penalty` 改为 `true`。

---

## 8. 维护建议
- 修改配置后执行 `/pnes reload`。
- 大版本升级前请先备份数据库与配置。
- 若与其他“死亡箱/生命上限”类插件冲突，建议只保留一种同类功能。
