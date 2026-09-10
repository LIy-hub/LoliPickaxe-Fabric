# Development and combat behavior / 开发与战斗行为

[Project overview / 返回项目介绍](../README.md)

## Source branches

Use the `mc/<Minecraft version>` branch matching the target release. The default `main` branch retains the original Fabric 1.20.1 development source. The version branches contain their own dependency and mapping configuration.

构建发布版时请选择 `mc/<Minecraft 版本>` 分支。默认 `main` 保留最初的 Fabric 1.20.1 开发源码；各版本分支有各自的依赖与映射配置。

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
