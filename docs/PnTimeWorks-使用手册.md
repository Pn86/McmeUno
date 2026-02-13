# PnTimeWorks 使用手册

## 1. 插件信息
- 插件名：`PnTimeWorks`
- 作者：`Pn86`
- 适配版本：`Paper 1.21.1`

## 2. 功能简介
PnTimeWorks 可以通过 `works.yml` 配置多个「动作组」，在指定时间自动执行控制台指令，并向全服玩家发送提示消息。

## 3. 指令与权限
### 管理指令
- `/pntws reload`：重载 `config.yml` 与 `works.yml`
- `/pntws list`：查看当前已加载的动作组

### 权限
- `pntimeworks.admin`（默认 OP 拥有）

## 4. 配置文件
插件启动后会在插件目录生成：
- `config.yml`：语言文本、提示消息
- `works.yml`：动作组与时间规则

### 4.1 config.yml
可修改消息文本，支持颜色符号 `&`。常用配置示例：

```yml
messages:
  prefix: '&8[&bPnTimeWorks&8] '
  no-permission: '&c你没有权限执行这个命令。'
  usage: '&e用法: /pntws <reload|list>'
  reload-success: '&aPnTimeWorks 配置重载完成。'
  list-header: '&a当前已加载操作组数量: &f{count}'
  work-executed-broadcast: '&e定时操作组 &b{group} &e已执行，共 &a{count} &e条指令。'
```

### 4.2 works.yml
格式如下：

```yml
1:
  time:
    - '0;13:00'
  action:
    - 'say 这是一个示例定时任务'
    - 'time set day'
```

#### time 字段规则
时间字符串格式：`日期规则;HH:mm`

- `0`：每天
- `1~7`：每周一到周日
- `01~31`：每月某日（必须两位数，例：`01`、`09`、`31`）

例如：
- `0;13:00`：每天 13:00
- `1;20:30`：每周一 20:30
- `05;08:15`：每月 5 号 08:15

#### action 字段规则
- 每行一条命令
- 以控制台身份执行
- 不要加前导 `/`

## 5. 错误格式自动修复
如果 `time` 中某条规则格式错误，插件会把该条自动改为 `none`，并且不会执行这条规则。

## 6. 重启后持久化说明
PnTimeWorks 会在启动时读取磁盘中的 `works.yml`，重启服务器后仍会按已保存配置继续运行。

## 7. 使用建议
1. 先修改 `works.yml`。
2. 执行 `/pntws reload` 热重载。
3. 用 `/pntws list` 检查是否加载成功。
4. 观察到点后是否执行命令并广播。
