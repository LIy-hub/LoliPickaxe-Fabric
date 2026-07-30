# LoliPickaxe

> A Fabric 1.20.4 memorial port of the classic LoliPickaxe mod.

This project preserves the concept and gameplay identity of the original
[IslenautsGK/LoliPickaxe](https://github.com/IslenautsGK/LoliPickaxe) while
reimplementing it for modern Fabric. It is distributed under GPL-3.0 in
accordance with the original project's license. See [CREDITS.md](CREDITS.md)
for attribution and third-party asset notes.

LoliPickaxe is a Fabric mod for Minecraft 1.20.4. It adds the Loli Pickaxe, an
intentionally overpowered item with special combat, mining, flight, and survival
abilities. The gameplay contract is frozen across supported Minecraft branches;
version branches contain compatibility changes only.

## Gameplay and acquisition

- Adds the fireproof `liymod:loli_pickaxe` and `liymod:loli` items to the
  LoliPickaxe creative tab.
- There is no crafting recipe in the current implementation. Use the creative
  inventory or commands to obtain the items.
- The pickaxe is kept unbreakable and has deliberately extreme mining and
  combat attributes.
- Attacking a block attempts to break it immediately. A fixed set of normally
  restricted blocks and ores also produces the special drops defined by the
  mod, including storage-block drops for ores.
- A deliberate main-hand attack uses the mod's absolute execution path. If
  normal targeting filters out a protected player, the server resolves the
  swing from the attacker's view up to 1024 blocks with a six-degree aim
  fallback.
- Using the pickaxe processes non-lightning entities in the user's
  32-block-expanded bounding box. Each successful execution creates a
  lightning bolt at the target's previous position.
- A main-hand holder rejects ordinary damage, death, forced removal and
  standard execution attempts; the holder is also excluded from ordinary
  combat targeting, raycasts, projectile collision and mob target predicates.
- Holders receive flight, continuously restored survival state, high luck and
  the existing level-two creative-operator check. The mod does not add the old
  movement-speed attribute boost.
- Two players holding the pickaxe in their main hands are immune to each
  other's Loli attack. Unique immunity events alternate two bundled sound
  samples for both players.

## Multiplayer and administration warning

This mod is intentionally unbalanced. It can execute players and multiple
nearby entities, and it can break or drop normally restricted administrative
blocks. Server owners should only distribute the item to trusted players and
should test compatibility with their protection and administration mods. This
project does not claim universal compatibility with every third-party combat or
protection implementation.

The normative compatibility contract and immutable asset hashes are documented
in [PORTING_BASELINE.md](PORTING_BASELINE.md).

## 中文说明

LoliPickaxe（氪金萝莉）是经典 LoliPickaxe 模组的 Minecraft 1.20.4
Fabric 纪念复刻版。本项目保留原模组的核心概念与玩法特色，并针对现代
Fabric 环境重新实现；它并非原作者发布的官方续作。

### 主要内容

- 加入防火的 `liymod:loli_pickaxe`（氪金萝莉）与
  `liymod:loli`（萝莉碎片），并放入独立创造物品栏。
- 当前实现没有合成配方，只能通过创造物品栏或命令获取；项目介绍不会凭空
  捏造配方。
- 氪金萝莉保持不可破坏，并拥有刻意设计为超规格的挖掘与战斗属性。
- 左击方块会尝试立即破坏；固定清单中的管理类方块、不可破坏方块与矿石还会
  产生模组定义的特殊掉落，矿石可额外掉落对应储存方块。
- 持有镐的玩家拥有完整的伤害、死亡、移除与处决防护。
- 使用氪金萝莉主动攻击时，可通过独立的绝对处决机制命中其他目标。
- 普通命中被保护层过滤时，服务端会根据玩家视线在最远 1024 格内解析目标，
  并提供 6 度辅助瞄准。
- 右键会处理玩家碰撞箱向外扩展 32 格范围内除闪电外的实体；每个成功处决的
  目标原位置都会生成闪电。
- 当攻击者与玩家目标都主手持有氪金萝莉时，同类武器免疫优先，不会创建处决请求，并会交替播放两段免疫提示音。
- 持有者会从常规近战选取、射线检测、弹射物碰撞与生物目标判定中排除，但移动、背包、区块加载和网络同步仍保持正常。
- 持有者获得飞行、持续恢复的生存状态、高幸运值和既有的二级创造管理员检查；
  当前实现不会重新添加旧版移动速度属性加成。

### 多人服务器与管理风险

本模组刻意保持不平衡玩法：它能处决玩家及多个附近实体，也能破坏或掉落通常
受限制的管理类方块。服主应只向可信玩家发放，并自行验证领地、权限、战斗等
第三方模组兼容性。本项目不声称能够兼容所有第三方保护或战斗实现。

跨版本迁移必须遵守的完整玩法合同与不可变素材哈希见
[PORTING_BASELINE.md](PORTING_BASELINE.md)。

### 运行要求

- Java 17
- Minecraft 1.20.4
- Fabric Loader 0.19.3 或更高版本
- Fabric API

### 原版与许可

本项目基于并纪念原版
[IslenautsGK/LoliPickaxe](https://github.com/IslenautsGK/LoliPickaxe)，按照
原项目的 GNU GPL-3.0 许可证继续以相同许可证发布。作者署名及第三方素材
说明见 [CREDITS.md](CREDITS.md)，完整许可证见 [LICENSE](LICENSE)。

## Requirements

- Java 17
- Minecraft 1.20.4
- Fabric Loader 0.19.3 or newer
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
