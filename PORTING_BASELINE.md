# LoliPickaxe high-version restoration contract

This document freezes the behavior that already exists in the Fabric port while
the remaining content from upstream LoliPickaxe is restored. Minecraft-version
adapters may change build configuration, mappings, API calls, mixin targets and
resource formats. They must not remove or weaken the behavior below. New
upstream-derived content may extend the mod around this core.

The source-of-truth for restored legacy content is upstream `master` commit
`c9a01e493cc7c8c265837b2d43f29a28a61d59fa` (Minecraft 1.12.2,
LoliPickaxe 1.2.16f). The smaller `1.7.10` branch is not the completeness
baseline. The item id `liymod:loli_pickaxe` keeps the Fabric port's stronger
execution, defense and same-item-immunity implementation. Legacy attack entry
points must call that implementation instead of reintroducing the old kill
path.

## Stable identities

- Mod id: `liymod`
- Items: `liymod:loli_pickaxe`, `liymod:loli`
- Item group: `liymod:liymod`
- Damage type: `liymod:loli_damage`
- Sounds: `liymod:loli_immunity_first`,
  `liymod:loli_immunity_second`
- Translation keys, texture ids and model ids remain unchanged.
- Existing ids remain stable. Restored legacy items, blocks, entities, recipes,
  screens and commands use the `liymod` namespace so existing worlds keep the
  current Fabric item ids.

## Item and mining behavior

- Both items are fireproof.
- The Loli Pickaxe has one stack per slot, is repaired by `liymod:loli`, is
  continuously kept undamaged and carries the version-appropriate unbreakable
  data/component.
- The tool keeps the original extreme material values: maximum integer mining
  level and durability, `Float.MAX_VALUE` mining speed, infinite attack damage
  contribution and enchantability 30.
- Attacking a block on the logical server attempts immediate vanilla breaking,
  plays the amethyst-block break sound and then emits the configured special
  drop for these 27 blocks:
  spawner, structure block, jigsaw, end portal frame, command block, chain
  command block, repeating command block, bedrock, barrier, coal ore,
  deepslate coal ore, iron ore, deepslate iron ore, gold ore, deepslate gold
  ore, redstone ore, deepslate redstone ore, diamond ore, deepslate diamond
  ore, emerald ore, deepslate emerald ore, lapis ore, deepslate lapis ore,
  copper ore, deepslate copper ore, nether quartz ore and ancient debris.

## Active execution behavior

- An entity attack with the main-hand pickaxe requests
  `ABSOLUTE_EXECUTION`.
- A main-hand swing also resolves a server-side player target by view geometry:
  exact expanded-box ray intersection first, then a six-degree aim fallback,
  with a maximum range of 1024 blocks.
- Using the pickaxe processes all non-lightning entities returned from the
  user's bounding box expanded by 32 blocks. A successful execution creates
  lightning at the saved target position.
- Authority ordering remains `STANDARD(0)` below
  `ABSOLUTE_EXECUTION(Integer.MAX_VALUE)`.
- Ticket state remains one-way:
  `PREPARE -> COMMITTING -> DEAD_LOCK`.
- Execution first tries normal damage/death, records player death exactly once,
  performs the existing fallback commit when necessary, enforces the dead lock,
  respawns completed players and removes non-player tickets after the existing
  20-tick lifetime.
- Non-player removal remains idempotent through the real removal reason; do not
  replace this with the mixin-modified `isRemoved()` result.

## Holder defense and abilities

- Protection applies while the player's main-hand stack is the Loli Pickaxe by
  default. An explicit server `inventory_protection=true` option may extend
  passive protection to inventory stacks, but the 1024-block swing resolver and
  active execution entry points still require the main-hand pickaxe.
- Ordinary damage and death are rejected. A non-holder attacker is retaliated
  against with `STANDARD` authority.
- Health, death/hurt timers, regeneration delay, fall distance, frozen ticks,
  air and fire state are restored each server tick.
- The mod grants invulnerability and flight while held, and only removes grants
  that it made when the item is no longer held.
- The retired movement-speed modifier remains removed.
- Protected holders report alive, cannot be killed, discarded, made invisible,
  stripped of invulnerability or removed outside trusted disconnect, respawn,
  dimension-change and cross-world teleport windows.
- Protected holders are excluded from normal attackability, living-entity hit
  checks, living target checks, `TargetPredicate`, projectile collision and
  server interaction-packet target resolution.
- The existing level-two creative-operator check, luck value `16384.0F` and
  experience-level replacement value `142857` remain unchanged.
- Dead-locked players cannot attack, break blocks or use items until their
  execution lifecycle completes.

## Same-item immunity and audio

- If attacker and player target both hold the Loli Pickaxe in their main hands,
  immunity is resolved before any absolute ticket is created.
- An immunity event is unique by server tick, world, attacker UUID and target
  UUID.
- Unique events alternate the first and second sound globally per server and
  play for both participating players.

## Existing core asset hashes

| Asset | SHA-256 |
|---|---|
| `assets/liymod/icon.png` | `7873CEEF555488D5EA08B54ECD29DC05D96A55A918F915D1011BFA14D0AA1E82` |
| `assets/liymod/textures/item/loli_pickaxe.png` | `7873CEEF555488D5EA08B54ECD29DC05D96A55A918F915D1011BFA14D0AA1E82` |
| `assets/liymod/textures/item/loli.png` | `B3E5D9B7606DDFD7E3AC1D137AFC8D1BBF9EF28101479553598DC71D698A41C0` |
| `assets/liymod/sounds/loli_immunity_first.ogg` | `98A24E9A3BB6DD17FF2C4EDE7D7671FCFDD1F03D3F71DDF54846A43E52B95848` |
| `assets/liymod/sounds/loli_immunity_second.ogg` | `B207F7C47B14A4CE3988A3F291F3F00B045C44EDAE13476F51B8C977363F0382` |

## Safety substitutions

The upstream blue-screen, exit and non-response attacks extracted and executed
an embedded Windows executable, terminated the client JVM, or started an
unbounded busy loop. Those operating-system destructive effects are not part of
the modern port. Their blocks, triggers, range, presentation and gameplay
feedback are restored with safe in-game/client visual equivalents. This is a
deliberate safety substitution, not an omitted content id.

Legacy online-card support must not download arbitrary content on the render
thread. The card and its configuration flow remain available, while remote
images are fetched only through a bounded, validated asynchronous path or are
represented by a safe placeholder.

## Restored legacy extensions

- The final pickaxe stores validated per-stack options for mining radius,
  mandatory drops, liquid boundaries, auto-smelting, auto-accept, thorns,
  reach and optional automatic range execution. Legacy kill-capable paths call
  `LoliErasureService`; none reintroduces the old direct kill/hack path.
- Final-pickaxe `auto_accept` controls both direct mining-drop insertion and
  nearby `ItemEntity` collection for that exact stack. Ordinary Loli mining-range
  changes play the restored upstream `lolisuccess.ogg` once from the server.
- N/M/P/K open server-authoritative configuration, enchantment, effect and
  relative space-folding screens. Space folding refuses unloaded chunks,
  blacklisted dimensions, out-of-border or colliding destinations and
  non-finite/over-limit offsets.
- Owner metadata protects and recalls dropped final pickaxes without blocking
  administrator entity cleanup. Internal storage remains bounded to normal
  stack limits, 32 KiB per encoded stack and 4 MiB total rather than restoring
  unsafe unbounded legacy counts.
- Cards, grouped albums, the HTTPS online-card editor/viewer, Loli dispersal,
  client-only ghost cleanup and the jukebox record are functional. Remote card
  loading is asynchronous, HTTPS-only, timeout-bounded and capped by response
  bytes and decoded image dimensions.
- The legacy `blue_screen`, `exit` and `fail_respond` triggers are present only
  as disabled-by-default safe effects. No implementation writes or starts an
  executable, terminates the JVM or creates an unbounded thread/loop.

## 中文冻结合同

完整移植以原版 `master` 的 1.12.2 / 1.2.16f 内容为基准，而氪金萝莉本体继续
采用当前 Fabric 移植已经强化的绝对处决、票据生命周期、主手防护、同物品免疫
与双音效机制。必须保持上述注册键、工具数值、27 类特殊掉落、32 格扩展范围
右键处决与闪电、1024 格/6 度挥击解析、两级处决权限与单向票据状态、持有者
防伤害/死亡/移除/锁定、飞行与恢复、目标隔离、二级创造管理员检查、高幸运与
经验替换、同持有者免疫和单 tick 去重。原版其余物品、方块、实体、配方、容器、
GUI、网络、配置和指令应围绕这条基线恢复。原版会执行蓝屏程序、强退 JVM 或
无限忙循环的三种客户端破坏效果只允许移植安全的游戏内等价表现。

## 旧版细粒度处决选项的现代安全语义

- `target_friendly_entities` 与 `target_all_entities` 只筛选默认关闭的自动范围处决；
  直接实体攻击、32 格右键处决和 1024 格/6 度挥击解析的冻结强度不受影响。
- `force_remove` 是默认关闭的旧版强制移除兼容开关。所有终结仍先经过
  `LoliErasureService.executeAbsolute`，该开关只委托既有 `LoliExecutionManager`
  提前落实非玩家实体移除，绝不直接调用 `kill`、`discard` 或 `remove`。
- `clear_inventory`、`drop_equipment` 与 `kick_player` 仅在服务端绝对处决成功后
  触发；同物品免疫优先。背包和装备不会被永久删除，而会转成目标归属、无敌、
  无限寿命的可回收掉落；踢出消息限 160 字符并移除换行和格式控制符。
- 原版“轮回”会阻止玩家数据读取和保存。现代实现改成持久化的一次性待处理名单：
  玩家重生或重连时仅恢复生命、食物和临时身体状态，保留存档与背包，随后自动
  移出名单。
- 原版“灵魂超度”会把名单玩家生命和最大生命永久强制为 0。现代实现改成可撤销
  名单：重生/重连时施加五分钟虚弱、缓慢和挖掘疲劳；管理员移出名单或加入
  `soul_redemption_whitelist` 后不再施加。名单最多 24 个逗号分隔、去重的 UUID
  或 1–16 字符玩家名，并可通过 `/loli playerlist ... list|add|remove` 管理。
- 上述危险/破坏性单件选项默认均为 `false`；名单默认均为空。
