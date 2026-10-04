# Development and combat behavior / 开发与战斗行为

[Project overview / 返回项目介绍](../README.md)

## Source branches

Use the `mc/<Minecraft version>` branch matching the target release. The default `main` branch contains the synchronized Fabric 1.20.1 source. The version branches contain their own dependency and mapping configuration.

构建发布版时请选择 `mc/<Minecraft 版本>` 分支。默认 `main` 保留同步后的 Fabric 1.20.1 源码；各版本分支有各自的依赖与映射配置。

```powershell
git clone --branch mc/1.20.1 --single-branch https://github.com/LIy-hub/LoliPickaxe-Fabric.git
cd LoliPickaxe-Fabric
.\gradlew.bat build
.\gradlew.bat runClient
```

The build writes the mod JAR to `build/libs/`. Consult the selected branch's `gradle.properties` and `build.gradle` for its JDK and dependency requirements. The [porting baseline](https://github.com/LIy-hub/LoliPickaxe-Fabric/blob/mc/26.1.2/PORTING_BASELINE.md) documents behavior and assets retained during version adaptation.

## Combat behavior

A main-hand holder is excluded from ordinary targeting, raycasts, projectile collision, and mob target predicates. Damage, death, and removal protections also cover direct calls that bypass those targeting checks. Movement, inventory, chunk loading, and synchronization continue normally.

The pickaxe's deliberate main-hand attack has its own resolver. If normal targeting filters a protected player, it searches the attacker's current world along the view ray, with a six-degree aim fallback and a maximum distance of 1024 blocks. When both players hold the pickaxe in their main hands, same-item immunity stops the attack before an execution ticket is created.

这些机制分别处理常规目标筛选和直接伤害调用。持有者仍正常移动、管理背包并与服务器同步；镐子自身的主动攻击使用独立判定，双方主手持有时优先触发同类免疫。

## Two-player immunity check

Use a separate LAN test world. As the host, run:

```text
/gamerule keepInventory true
/gamemode survival @a
/give @a liymod:loli_pickaxe
```

Have both players hold the pickaxe in their main hands. Left-click the other player once. The target should survive and both players should hear an immunity sound. Repeat once to check the second sound. Duplicate callbacks for the same attacker and target in one server tick count as one event.

在独立测试世界中让双方主手持镐，分别检查存活、提示音和两次攻击间的音效交替。再检查持有与收起镐子后的飞行、防护与移动速度。需要同时测试其他模组时，另行记录所用版本与结果。

## Synchronized mining, storage and menus / 挖掘、储存与界面同步

The 1.20.1 implementation includes the version-adapted gameplay from the 26.2 source: single/range mining, fluid selection, block/range outlines, a 20 ms completion gap between range actions, direct experience collection with Mending, batched storage writes, bounded compressed storage synchronization and one localized kill/drop/experience summary per attack action. Existing native NBT save data remains in its version-specific format. Client chat animates only the summary title.

Storage/blacklist, configuration, enchantment, effect, teleport, password-workbench and card menus use server-owned persistence and networking. The final pickaxe retains 100 storage pages of 81 slots; visible pages grow with stored contents. See [GUI behavior and validation](gui-validation.md) and [porting baseline](../PORTING_BASELINE.md). The build uses official Mojang mappings; Java release and dependencies are defined by the selected version branch.

1.20.1 已适配 26.2 的单方块/范围挖掘、流体选择、方块与范围轮廓、范围操作完成后的 20 ms 间隔、直接经验收集与经验修补、批量储存写入、有界压缩同步，以及每次攻击一次的击杀/掉落/经验汇总。原有 NBT 存档仍使用对应版本的原生格式；聊天动画只作用于汇总标题。

储藏室、黑名单、配置、附魔、药水、传送、密码工作台和卡片界面由服务端保存与同步。最终镐保留每页 81 格、最多 100 页的容量，可见页数随内容增长。界面行为与验证见上述文档。

Build/regression checks and native client startup are separate from in-game operation, visual, multiplayer and performance acceptance. Test those behaviors in a separate world before relying on them in an existing server. 构建、回归检查和原生客户端启动不代表游戏内操作、画面、多人或性能验收；请先在独立世界验证实际行为。
