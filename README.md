# LoliPickaxe Forge 1.20.1

这是经典 [LoliPickaxe](https://github.com/IslenautsGK/LoliPickaxe) 的独立 Forge 1.20.1 移植分支。它恢复 1.12.2 `master` 的物品、方块、实体、配方、储存、界面和工具机制，同时保留现代移植版更强的氪金萝莉处决、防护、同物免疫与超远距离能力。

本分支专门用于与 Forge 1.20.1 的 Forever Love Sword、EntityEraser、PIG2 一起进行隔离强度测试，不依赖 Fabric API，也不会把第三方模组 JAR 打进成品。

## 主要内容

- 28 个可见物品、5 个方块、2 个实体、1 个附魔、20 个固定配方和 3 个动态升级配方。
- 普通萝莉镐与 14 类升级材料：原版等级公式、范围采掘/攻击、时运/抢夺、自动熔炼、飞行、状态效果、闪避、反伤和分级储存。切换范围会播放原版 `lolisuccess.ogg`。
- 氪金萝莉镐：单次服务端事件内完成范围采掘；27 类特殊掉落；默认自动熔炼与单件 `AUTO_ACCEPT`；1024 格内全部已加载实体右键处决；1024 格方块/实体接触距离和 6 度挥击解析；主手防护、同物免疫、反伤、主人绑定掉落物保护与召回。
- B 打开 9×9 分页储存，Shift+B 丢出全部储存物，U 编辑黑名单。最终镐 100 页，普通镐页数随升级变化。主动丢出的物品带永久排除标记，不会被自动收纳重新吸回；附近吸取与采掘掉落都读取当前单件镐的 `AUTO_ACCEPT`。
- N 打开四页单件设置，M 编辑最高 32768 级附魔，P 编辑状态效果，K 使用服务端校验的相对空间折叠。界面文本使用高对比亮色。
- 已恢复清背包、缴械、踢出、自动攻击实体过滤、强制处决、轮回名单、灵魂超度名单和豁免名单。危险选项默认全部关闭；物品转移在处决失败时按原槽回滚，成功时只生成绑定目标、无敌、无限寿命且不会被储存吸回的可回收掉落物。
- 63×63 精确祭坛、萝莉实体、退散与异常实体清理物品、密码工作台、三种安全 TNT、十张卡图/卡册、HTTPS 网络卡片和萝莉唱片。
- 萝莉实体默认恢复旧版锁敌攻击、合法目标位置瞬移与移动速度；服务端可通过 `/loli set loli_attack|loli_teleport|loli_speed <值>` 分别调整。
- 原版蓝屏、强退和未响应攻击不会移植为操作系统或 JVM 破坏行为，只提供默认关闭的游戏内安全替代。

## 按键

| 按键 | 功能 |
|---|---|
| B | 打开当前手持萝莉镐储存 |
| Shift+B | 丢出全部储存物 |
| U | 编辑储存黑名单 |
| N | 氪金萝莉单件配置 |
| M | 附魔编辑 |
| P | 状态效果编辑 |
| K | 空间折叠 |

## 管理命令

- `/loli list|get|set|reload`
- `/loli playerlist <reincarnation|soul_redemption|soul_whitelist> <list|add|remove>`
- `/loliattack <player> <blue_screen|exit|fail_respond>`，总开关与每种效果默认关闭

配置保存在 `config/liymod-forge.properties`。三类玩家名单最多各 24 个去重后的 UUID 或 1–16 字符玩家名。

## 强度对抗兼容

可选运行时兼容钩子只在对应模组实际加载时启用：

- Forever Love Sword：绝对处决前撤销目标 UUID 的 FLS 防护记录，不调用其危险击杀函数。
- EntityEraser：保护氪金萝莉持有者，并只在其成为绝对处决目标时撤销 EntityEraser 防护记录。
- PIG2：持续恢复氪金萝莉持有者的 PIG2 许可；绝对处决 PIG2 后短时压制其复制/复活实体。

直接攻击、1024 格右键和 1024 格挥击入口都继续经过统一处决服务；双方主手都持有氪金萝莉时，同物免疫优先。

第三方模组本身带有高权限 Mixin/Java Agent/变换服务。四模组联载应在独立实例或虚拟机中测试。本项目不重新分发第三方 JAR。

## 要求与构建

- Minecraft 1.20.1
- Forge 47.4.22 或同系列兼容版本
- Java 17

Windows 构建：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:JAVA_OPTS = '-Dnet.minecraftforge.gradle.check.certs=false'
.\gradlew.bat clean build --no-daemon
.\scripts\verify-full-port.ps1
.\scripts\verify-release.ps1
```

成品位于 `build/libs/liymod-forge-1.20.1-1.0.0-forge.1.jar`。开发客户端使用 `.\gradlew.bat runClient`；带混淆名称的第三方模组应在正式 Forge 客户端测试，不能以 ForgeGradle 开发映射客户端的加载结果代替成品结论。

## 许可与致谢

本项目按原项目的 GPL-3.0 继续发布。作者、素材来源与许可说明见 [CREDITS.md](CREDITS.md)，完整移植边界见 [PORTING_BASELINE.md](PORTING_BASELINE.md)。
