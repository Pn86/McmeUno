# PnBookNews 使用说明

PnBookNews 是一个适用于 Paper 1.21.1+ 的简易公告书插件。玩家进入服务器时，插件会自动在玩家界面打开一本写好的公告书；如果服务器安装并启用了 AuthMe，且配置开启兼容，则会在玩家完成 AuthMe 登录后再打开公告书。

## 基本信息

- 插件名：`PnBookNews`
- 作者：`Pn86`
- 服务器核心：Paper `1.21.1+`
- 可选兼容：`AuthMe`、`PlaceholderAPI`
- 管理命令：`/pnbn reload`
- 管理权限：`pnbooknews.admin`

## 安装方法

1. 将编译出的 `PnBookNews-1.0.0.jar` 放入服务器 `plugins` 文件夹。
2. 重启服务器。
3. 打开 `plugins/PnBookNews/config.yml` 修改公告内容。
4. 修改完成后执行 `/pnbn reload` 重载配置。

## config.yml 配置说明

```yaml
settings:
  enabled: true
  show-on-every-join: true
  open-delay-ticks: 20
  authme-compat: true
  placeholderapi: true

book:
  title: '&6服务器公告'
  author: '&bPn86'
  pages:
    - |-
      &6&l欢迎来到服务器
      &0你好，&a%player_name%&0！

      &0请阅读本公告。
      &e祝你游戏愉快！
```

### settings 设置

| 配置项 | 说明 |
| --- | --- |
| `enabled` | 插件总开关，`true` 为启用，`false` 为关闭打开公告书功能。 |
| `show-on-every-join` | 是否每次进入服务器都打开公告书。设置为 `false` 后玩家进入或 AuthMe 登录后都不会自动弹出。 |
| `open-delay-ticks` | 延迟打开公告书的时间，单位是 tick，`20` tick = 1 秒。 |
| `authme-compat` | 是否启用 AuthMe 兼容。服务器有 AuthMe 时，玩家登录 AuthMe 后打开公告书。 |
| `placeholderapi` | 是否启用 PlaceholderAPI 变量解析。需要服务器安装 PlaceholderAPI。 |

## 如何配置 book 内容

`book.pages` 是公告书页面列表。每个 `- |-` 代表一页书页，下面缩进的文字就是这一页的内容。

### 单页公告示例

```yaml
book:
  title: '&6服务器公告'
  author: '&bPn86'
  pages:
    - |-
      &6&l欢迎来到服务器
      &0玩家：&a%player_name%
      &0当前世界：&e%player_world%

      &c请遵守服务器规则！
```

### 多页公告示例

```yaml
book:
  title: '&6服务器公告'
  author: '&bPn86'
  pages:
    - |-
      &6&l第一页：欢迎
      &0欢迎 &a%player_name% &0加入服务器！
    - |-
      &b&l第二页：常用命令
      &0/spawn &7返回出生点
      &0/tpa 玩家名 &7请求传送
    - |-
      &c&l第三页：规则
      &01. 禁止作弊
      &02. 禁止恶意破坏
```

## 颜色字符

插件支持使用 `&` 颜色字符，例如：

- `&0` 黑色
- `&a` 绿色
- `&b` 青色
- `&c` 红色
- `&e` 黄色
- `&l` 加粗
- `&n` 下划线
- `&r` 重置格式

示例：

```yaml
- |-
  &6&l服务器公告
  &a这是一行绿色文字
  &c这是一行红色文字
```

## PlaceholderAPI 变量

如果服务器安装了 PlaceholderAPI，并且 `settings.placeholderapi: true`，公告书标题、作者和每一页内容都可以使用 PAPI 变量。

示例：

```yaml
- |-
  &0玩家名：&a%player_name%
  &0所在世界：&e%player_world%
  &0在线人数：&b%server_online%
```

## AuthMe 兼容说明

- 如果服务器没有安装 AuthMe：玩家进入服务器后按 `open-delay-ticks` 延迟打开公告书。
- 如果服务器安装了 AuthMe，并且 `settings.authme-compat: true`：玩家进入服务器时不会立刻打开公告书，而是在 AuthMe 登录成功后打开公告书。
- 如果想无视 AuthMe，进服就弹公告书，请设置：

```yaml
settings:
  authme-compat: false
```

## 重载配置

修改 `config.yml` 后执行：

```text
/pnbn reload
```

需要权限：

```text
pnbooknews.admin
```
