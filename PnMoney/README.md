# PnMoney (Paper 1.21.1)

PnMoney 是一个带有基础货币系统与简单商店系统的插件。

## 指令
- `/pnmy reload` 重载插件
- `/pnmy give [玩家] [数量]` 给予玩家货币
- `/pnmy take [玩家] [数量]` 拿去玩家货币
- `/pnmy reset [玩家]` 重置玩家货币
- `/pnmy set [玩家] [数量]` 设置玩家货币
- `/pnmy pay [玩家] [数量]` 将自己的货币给予其他玩家
- `/pnmy list [玩家]` 查看玩家货币
- `/pnmy top` 查看货币排行榜
- `/pnmy shop [商品ID]` 购买物品

## 权限
- `pnmoney.admin`：管理权限（reload/give/take/reset/set/pay/list他人）
- `pnmoney.player`：普通玩家权限（shop/top/list自己）
- `pnmoney.use`：兼容旧权限节点（与 `pnmoney.player` 等效用于普通功能）

## 配置文件
- `config.yml`
  - 货币名称
  - 允许负数
  - 小数最大位数
  - 货币最大值
  - 默认货币值
  - 语言信息
  - 存储方式（SQLite / MySQL）
- `shop.yml`
  - `use`：是否启用商店
  - 商品节点示例（正确格式）：
    ```yml
    use: true

    "1":
      int: "10"
      item:
        - "eco give %player% 100"

    "vip_day":
      int: "88.50"
      item:
        - "lp user %player% parent addtemp vip 1d"
        - "tellraw %player% {\"text\":\"你成功购买了1天VIP！\",\"color\":\"gold\"}"
    ```

## 存储与迁移
- 默认使用 SQLite（`plugins/PnMoney/pnmoney.db`）。
- 当 `storage.type: mysql` 时，插件会连接 MySQL（MariaDB 驱动），并尝试把 SQLite 的 `pnmoney_balances` 自动迁移到 MySQL。
- 余额操作为实时写入数据库，重启后可继续读取。

## PlaceholderAPI
- `%pnmoney.money%` 货币名称
- `%pnmoney.bal%` 玩家余额

## Shop 购买校验说明
- 商店购买时会优先通过 PlaceholderAPI 解析 `%pnmoney.bal%` 来确认玩家余额。
- 若服务器未安装 PlaceholderAPI，则自动回退为插件内部余额读取。
- 仅当余额充足且扣款成功时才会执行 `shop.yml` 中的商品命令。

## 对外 API
插件主类提供 `getMoneyService()`，返回 `MoneyService` 接口。

### API 接口
```java
import org.bukkit.Bukkit;
import uno.mcme.pnmoney.PnMoneyPlugin;
import uno.mcme.pnmoney.api.MoneyService;

PnMoneyPlugin plugin = (PnMoneyPlugin) Bukkit.getPluginManager().getPlugin("PnMoney");
if (plugin != null) {
    MoneyService api = plugin.getMoneyService();
    // api.getBalance(player)
    // api.addBalance(player, new BigDecimal("100"))
}
```

`MoneyService` 方法：
- `BigDecimal getBalance(OfflinePlayer player)`
- `boolean setBalance(OfflinePlayer player, BigDecimal amount)`
- `boolean addBalance(OfflinePlayer player, BigDecimal amount)`
- `boolean takeBalance(OfflinePlayer player, BigDecimal amount)`
