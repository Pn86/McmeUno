# PnSkill

一个基于 **Paper 1.21.1** 的轻量技能插件，支持为物品绑定技能（通过物品 NBT / PDC 持久化），并通过右键或 Shift+右键释放技能。

## 作者

- Pn86

## 核心特性

- `手持绑定技能的物品` + `右键` 触发技能 `a`
- `手持绑定技能的物品` + `Shift + 右键` 触发技能 `b`
- 每个技能模式（a/b）独立冷却
- 冷却期间在聊天框提示剩余时间
- 绑定信息写入物品 NBT（PersistentDataContainer），重启后仍然有效
- 技能通过 `skill.yml` 配置，文本通过 `config.yml` 配置

## 指令

- `/pnsk bind [技能ID]` 将手持物品绑定一个技能
- `/pnsk see` 查看手持物品是否绑定技能
- `/pnsk reload` 重载插件配置（需 `pnskill.admin`）
- `/pnsk list` 查看所有技能
- `/pnsk skill [技能ID] [a|b]` 直接释放指定技能模式

## 权限

- `pnskill.admin`：允许使用 `/pnsk reload`

## `skill.yml` 格式

```yml
agility:
  name: '&b&l敏捷'
  a:
    title: '&b加速'
    time: 30
    action:
      - 'effect give @s minecraft:speed 1 10'
  b:
    title: '&b超级加速'
    time: 60
    action:
      - 'effect give @s minecraft:speed 3 10'
```

## 构建

```bash
cd PnSkill
mvn clean package
```

构建产物：`target/PnSkill-1.0.0.jar`
