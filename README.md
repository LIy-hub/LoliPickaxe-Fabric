# LoliPickaxe

> A Fabric 1.20.1 memorial port of the classic LoliPickaxe mod.

This project preserves the concept and gameplay identity of the original
[IslenautsGK/LoliPickaxe](https://github.com/IslenautsGK/LoliPickaxe) while
reimplementing it for modern Fabric. It is distributed under GPL-3.0 in
accordance with the original project's license. See [CREDITS.md](CREDITS.md)
for attribution and third-party asset notes.

LoliPickaxe is a Fabric mod for Minecraft 1.20.1. It adds the Loli Pickaxe, an intentionally overpowered item with special combat, mining, flight, movement, and survival abilities.

## 中文说明

LoliPickaxe（氪金萝莉）是经典 LoliPickaxe 模组的 Minecraft 1.20.1
Fabric 纪念复刻版。本项目保留原模组的核心概念与玩法特色，并针对现代
Fabric 环境重新实现；它并非原作者发布的官方续作。

### 主要内容

- 加入刻意设计为超规格强度的“氪金萝莉”镐。
- 提供强力挖掘、特殊战斗、飞行、移动与生存能力。
- 持有镐的玩家拥有完整的伤害、死亡、移除与处决防护。
- 使用氪金萝莉主动攻击时，可通过独立的绝对处决机制命中其他目标。
- 当攻击者与玩家目标都主手持有氪金萝莉时，同类武器免疫优先，不会创建处决请求，并会交替播放两段免疫提示音。
- 持有者会从常规近战选取、射线检测、弹射物碰撞与生物目标判定中排除，但移动、背包、区块加载和网络同步仍保持正常。

### 运行要求

- Java 17
- Minecraft 1.20.1
- Fabric Loader 0.16.14 或更高版本
- Fabric API

### 原版与许可

本项目基于并纪念原版
[IslenautsGK/LoliPickaxe](https://github.com/IslenautsGK/LoliPickaxe)，按照
原项目的 GNU GPL-3.0 许可证继续以相同许可证发布。作者署名及第三方素材
说明见 [CREDITS.md](CREDITS.md)，完整许可证见 [LICENSE](LICENSE)。

## Requirements

- Java 17
- Minecraft 1.20.1
- Fabric Loader 0.16.14 or newer
- Fabric API

## Development

Build the mod on Windows:

```powershell
.\gradlew.bat build
```

The remapped mod JAR is written to `build/libs`.

Launch a development client:

```powershell
.\gradlew.bat runClient
```

The project uses Yarn mappings and Fabric Loom. Mod metadata is in `src/main/resources/fabric.mod.json`.

## License

Copyright notices and contributor attribution are preserved in
[CREDITS.md](CREDITS.md). The source code and distributed binaries are licensed
under the GNU General Public License version 3.0; see [LICENSE](LICENSE).

## Loli Pickaxe vs. Loli Pickaxe test

The holder keeps the full defensive stack against ordinary damage and standard
execution attempts. An intentional attack made with the Loli Pickaxe uses the
separate `ABSOLUTE_EXECUTION` authority against other targets. When both the
attacker and player target hold the Loli Pickaxe in their main hand, the
same-item immunity takes priority and no execution ticket is created.

Run these commands as the LAN host:

```text
/gamerule keepInventory true
/gamemode survival @a
/give @a liymod:loli_pickaxe
```

Have both players hold the pickaxe in their main hand. Aim at the other player
and left-click once. The protected player is excluded from the normal entity
hit result, so the server resolves the swing geometrically, then stops it at
the same-item immunity check. The first unique immunity event plays the first
bell sample for both players, the second event plays the second sample, and the
sequence continues alternating. Duplicate callbacks for the same attacker and
target during one server tick count only once.

Holding the pickaxe no longer changes the player's movement-speed attribute.

## Target isolation

A player holding the Loli Pickaxe in their main hand remains a normal player
for movement, inventory, chunk loading, and network synchronization, but is
excluded from the standard combat-targeting surface. The protected player is
not attackable or hittable, is filtered from vanilla raycasts and projectile
collision searches, cannot pass a `TargetPredicate`, and is discarded from
server-side entity-interaction packets before an attack or interaction is
resolved.

The Loli Pickaxe's own main-hand swing is the deliberate exception. A hand
swing is handled on the server, which searches the attacker's current world for
the closest player intersecting the view ray, with a six-degree aim-assist
fallback and a maximum distance of 1024 blocks. This resolver does not call
`canHit`, `isAttackable`, `TargetPredicate`, or `ProjectileUtil`, and its
absolute ticket cannot be rolled back by the passive Loli defense unless the
target and attacker are both current Loli Pickaxe holders. That same-item check
runs before ticket creation.

This is logical target isolation rather than a change to Java inheritance. A
`ServerPlayerEntity` must still extend `Entity` for Minecraft to keep the
player connected and ticking. The existing damage, death, removal, and
execution defenses remain responsible for rejecting direct references from
code that bypasses normal targeting APIs.
