# PnMoneyShop

Paper 1.21.1 指令商店插件，基于 PlaceholderAPI 读取余额并通过控制台指令扣款/加款。

## 指令
- `/pnms reload` 重载 `config.yml` 与 `shop.yml`
- `/pnms buy <商品ID>` 购买商品

## 配置文件
- `config.yml`
  - `money.balance-placeholder`：余额占位符（例如 `%vault_eco_balance%`）
  - `money.deduct-commands`：扣款命令模板
  - `money.add-commands`：加款命令模板
  - `messages`：所有文本
  - `feedback.success/fail`：成功/失败音效与 title/subtitle
- `shop.yml`
  - `use`：是否启用商店
  - `<id>.price`：商品价格
  - `<id>.action`：`deduct` 或 `add`
  - `<id>.commands`：购买成功后执行命令

## 占位符
命令和文本支持：
- `%player%`
- `%amount%`（同 `%price%`）
- `%price%`
- `%balance%`
