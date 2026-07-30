# LoliPickaxe compatibility contract

This document is the frozen gameplay and asset contract for compatibility-only
ports. A Minecraft-version branch may change build configuration, mappings,
API calls, mixin targets and resource format, but it must not remove, weaken,
extend or rebalance the behavior below.

## Stable identities

- Mod id: `liymod`
- Items: `liymod:loli_pickaxe`, `liymod:loli`
- Item group: `liymod:liymod`
- Damage type: `liymod:loli_damage`
- Sounds: `liymod:loli_immunity_first`,
  `liymod:loli_immunity_second`
- Translation keys, texture ids and model ids remain unchanged.
- The implementation has no crafting recipes.

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

- Protection applies only while the player's main-hand stack is the Loli
  Pickaxe.
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

## Immutable asset hashes

| Asset | SHA-256 |
|---|---|
| `assets/liymod/icon.png` | `7873CEEF555488D5EA08B54ECD29DC05D96A55A918F915D1011BFA14D0AA1E82` |
| `assets/liymod/textures/item/loli_pickaxe.png` | `7873CEEF555488D5EA08B54ECD29DC05D96A55A918F915D1011BFA14D0AA1E82` |
| `assets/liymod/textures/item/loli.png` | `B3E5D9B7606DDFD7E3AC1D137AFC8D1BBF9EF28101479553598DC71D698A41C0` |
| `assets/liymod/sounds/loli_immunity_first.ogg` | `98A24E9A3BB6DD17FF2C4EDE7D7671FCFDD1F03D3F71DDF54846A43E52B95848` |
| `assets/liymod/sounds/loli_immunity_second.ogg` | `B207F7C47B14A4CE3988A3F291F3F00B045C44EDAE13476F51B8C977363F0382` |

## 中文冻结合同

各 Minecraft 版本分支只能修改构建、映射、API、Mixin 目标与资源格式，不得
删减、增强或重平衡玩法。必须保持上述注册键、无配方状态、工具数值、27 类
特殊掉落、32 格扩展范围右键处决与闪电、1024 格/6 度挥击解析、两级处决权限
与单向票据状态、持有者防伤害/死亡/移除/锁定、飞行与恢复、目标隔离、二级
创造管理员检查、高幸运与经验替换、同持有者免疫、单 tick 去重、双音效交替及
全部素材哈希。服务端风险属于既有玩法，也不得在“兼容迁移”中暗中改动。
