# LoliPickaxe

> A Fabric 26.2 memorial port of the classic LoliPickaxe mod.

This project preserves the concept and gameplay identity of the original
[IslenautsGK/LoliPickaxe](https://github.com/IslenautsGK/LoliPickaxe) while
reimplementing it for modern Fabric. It is distributed under GPL-3.0 in
accordance with the original project's license. See [CREDITS.md](CREDITS.md)
for attribution and third-party asset notes.

LoliPickaxe is a Fabric mod for Minecraft 26.2. The high-version branch is
restoring the complete item, block, entity and utility catalog from the classic
1.12.2 release while preserving the Fabric port's stronger Loli Pickaxe combat
and survival implementation.

## Gameplay and acquisition

- Restores the Ordinary Loli Pickaxe, fourteen tiered upgrades, cards, record,
  utility items, five blocks and their original artwork in the LoliPickaxe tab.
- Restores twenty classic shaped/shapeless recipes plus the dynamic 9-to-1
  upgrade, Ordinary Loli upgrade and final Loli Pickaxe conversion recipes.
- The Ordinary Loli Pickaxe stores each upgrade independently and applies the
  original formulas for mining, attack, range, fortune, storage and defensive
  stats. Mining/attack attributes, Fortune/Looting, adjustable range mining,
  hostile-area attacks, auto-smelting, flight, status effects, dodge and
  damage-return upgrades are active; changing its mining range plays the restored
  upstream `lolisuccess.ogg`. Its tier-sized internal storage, blacklist,
  nearby item collection and direct mining-drop insertion are also active.
- Press B while holding either pickaxe to open its paged 9x9 storage, Shift+B
  to drop all stored stacks, and U to edit its 9x9 ghost-slot blacklist. The
  final Loli Pickaxe has 100 pages; Ordinary Loli page count follows its storage
  upgrade tier. Nearby auto-collection runs only while a storage pickaxe is in
  either hand, and intentional player drops are never immediately collected
  back. Stored stacks use normal modern stack limits and bounded NBT.
- While holding the final Loli Pickaxe, press N for per-item mining/combat
  settings, M for enchantments, P for status effects and K for bounded relative
  space folding. The server validates every id, level, setting, dimension,
  distance, loaded chunk and landing position. Enchantment editing supports
  levels through 32768.
- The final pickaxe now restores configurable radius mining, Fortune 32,
  auto-smelting, per-item automatic storage acceptance for both mining drops and
  nearby item entities, reach, thorns, optional automatic
  range execution, status effects and owner-bound dropped-item recall. Its
  existing 32-block right-click execution and 1024-block/six-degree attack
  resolver remain unchanged. Inventory-wide holder protection is available as
  an operator option but is disabled by default, preserving the port's original
  main-hand rule.
- Bundled cards and the album display all ten original artworks. The online card
  accepts HTTPS URLs through sneak-use and loads them asynchronously with strict
  time, size, MIME and image-dimension limits. The Loli record is a playable
  jukebox disc. Legacy card, album, creeper-record and entity-soul drop chances
  are restored and configurable.
- Operators can inspect and change the persisted server whitelist with
  `/loli list|get|set|reload`. The restored fine-grained final-pickaxe options
  include automatic-range friendly/non-living filters, eager removal, safe
  inventory clearing, disarming, disconnect messages, reincarnation and soul
  redemption. Every destructive toggle defaults to `false`, applies only after
  a successful server-side `ABSOLUTE_EXECUTION`, and never bypasses same-item
  immunity. Inventory clearing/disarming is detached transactionally before
  vanilla death drops, rolled back to exact slots on any abort, and committed
  only after `DEAD_LOCK` as owner-targeted, invulnerable, unlimited-lifetime
  drops. `force_remove` upgrades thorns from standard to absolute execution
  without bypassing the execution service. `/loli playerlist
  <reincarnation|soul_redemption|soul_whitelist> <list|add|remove>` manages the
  three persisted, deduplicated lists (at most 24 UUIDs or player names).
  `/loliattack` exposes only the bounded in-game
  replacements for the legacy destructive effects and is disabled by default.
- The Password Workbench restores the original 3x3/password interface and
  server-authoritative password matching. As in the released legacy build, its
  built-in password recipe registry is empty until integrations register recipes.
- Restores the level-one `liymod:loli_auto_furnace` enchantment through the
  native 26.2 enchantment registry and `minecraft:smelts_loot` contract.
- Restores the exact 63 by 63 Loli Altar ritual, the persistent Loli entity and
  the three special TNT blocks. The legacy operating-system attacks are replaced
  by bounded, responsive in-game effects or a single-player disconnect; they
  never start a process, terminate the JVM or run a busy loop.
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

The preserved Fabric behavior and full-restoration boundaries are documented in
[PORTING_BASELINE.md](PORTING_BASELINE.md). Build verification also includes
`scripts/verify-full-port.ps1`.

## 中文说明

LoliPickaxe（氪金萝莉）是经典 LoliPickaxe 模组的 Minecraft 26.2
Fabric 纪念复刻版。本项目保留原模组的核心概念与玩法特色，并针对现代
Fabric 环境重新实现；它并非原作者发布的官方续作。

### 主要内容

- 已恢复普通萝莉、十四类分级升级材料、卡片、唱片、辅助工具、五种方块及
  原版素材，并放入独立创造物品栏。
- 已恢复二十个固定配方，以及升级材料九合一/一拆九、普通萝莉升级和满级
  普通萝莉转化为氪金萝莉的三种动态配方。
- 普通萝莉独立保存各类升级并采用原版数值公式；采掘/攻击属性、飞行与状态
  升级、时运/抢夺、可调范围采掘、范围攻击、自动熔炼、闪避与反伤均已实际
  接入，切换采掘范围会播放恢复的原版 `lolisuccess.ogg`；分级内部储存、黑名单、
  附近掉落物自动收纳及采掘掉落直入储存也已接入。
- 手持任一种萝莉镐时，B 打开 9×9 分页储存，Shift+B 丢出全部储存物，U 编辑
  9×9 幽灵槽黑名单。氪金萝莉提供 100 页，普通萝莉页数随储存升级级数变化；
  只有主手或副手实际持有储存镐时才会自动吸取附近掉落物，玩家主动丢出的物品
  不会被立即吸回；储存遵守现代正常堆叠上限与有界 NBT 安全限制。
- 手持氪金萝莉时，N 打开单件配置，M 编辑附魔，P 编辑状态效果，K 打开空间折叠。
  附魔等级上限为 32768；选项、注册表 ID、等级、维度、距离、区块加载状态和
  落点安全均由服务端校验。
- 氪金萝莉已融合原版可调范围采掘、时运 32、自动熔炼/单件自动收纳、触及距离、反伤、
  可选自动范围处决、药水效果和主人绑定掉落物召回；现有 32 格右键处决与
  1024 格/6 度挥击解析保持不变。背包任意位置防护可由管理员开启，但默认关闭，
  因而仍保持当前移植的主手防护规则。
- 卡片与卡片册可浏览原版全部十张图片；网络卡片通过潜行右键配置 HTTPS 地址，
  并以异步、超时、大小/MIME/尺寸上限安全加载。萝莉唱片可由唱片机播放；卡片、
  卡册、苦力怕唱片和生物灵魂的原版掉落概率也已恢复并可配置。
- 管理员可用 `/loli list|get|set|reload` 管理持久化白名单配置；`/loliattack`
  仅调用安全的游戏内替代表现，且总开关与三个效果默认全部关闭。氪金萝莉单件
  配置还恢复了自动范围攻击的友好/非生物实体过滤、立即强制移除、清背包、缴械、
  自定义踢出消息、轮回与灵魂超度；这些危险开关默认均为 `false`。清背包与缴械
  会在原版死亡掉落前事务式移出物品，任何免疫/撤销分支均按原槽回滚，只在最终
  `DEAD_LOCK` 后生成绑定目标、无敌且无限寿命的可回收掉落物；名单与踢出也只在
  此提交点发生。`force_remove` 会将反伤从标准处决升级为绝对处决，但仍经过统一
  服务且不绕过同物免疫。管理员可用 `/loli playerlist
  <reincarnation|soul_redemption|soul_whitelist> <list|add|remove>` 管理三个持久化
  名单；名单去重且最多包含 24 个 UUID 或玩家名。
- 密码工作台已恢复 3×3 合成区、密码输入与服务端判定。与原版发行源码一致，
  内置密码配方注册表默认为空，供后续兼容集成注册配方。
- 已通过 26.2 原生附魔注册表与 `minecraft:smelts_loot` 标签恢复一级
  `liymod:loli_auto_furnace` 自动熔炼附魔。
- 已恢复精确 63×63 萝莉祭坛仪式、持久且只能被退散物品移除的萝莉实体，以及
  三种特殊 TNT。原版操作系统级破坏行为已替换为有时限、可响应的游戏内效果
  或仅断开受影响玩家，不会启动进程、退出 JVM 或制造忙循环。
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

- Java 25
- Minecraft 26.2
- Fabric Loader 0.19.3 或更高版本
- Fabric API

### 原版与许可

本项目基于并纪念原版
[IslenautsGK/LoliPickaxe](https://github.com/IslenautsGK/LoliPickaxe)，按照
原项目的 GNU GPL-3.0 许可证继续以相同许可证发布。作者署名及第三方素材
说明见 [CREDITS.md](CREDITS.md)，完整许可证见 [LICENSE](LICENSE)。

## Requirements

- Java 25
- Minecraft 26.2
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

The project uses Mojang's official mappings and Fabric Loom. Mod metadata is
in `src/main/resources/fabric.mod.json`.

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
