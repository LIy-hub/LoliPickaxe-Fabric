# Forge 1.20.1 full-port review matrix

## Acceptance baseline

- Target branch: `forge/1.20.1` (Minecraft 1.20.1, Forge 47.4.22, Java 17).
- Legacy source: IslenautsGK/LoliPickaxe `master` at
  `c9a01e493cc7c8c265837b2d43f29a28a61d59fa` (1.12.2 / 1.2.16f).
- The stronger modern final-pickaxe behavior is frozen by `PORTING_BASELINE.md`.
- Completion requires an independent reviewer to report **Pass** with measured
  completeness strictly greater than 90%. Missing or deliberately substituted
  behavior must be listed separately.

## Weighted review areas

| Area | Weight | Forge 1.20.1 evidence |
|---|---:|---|
| Preserved final-pickaxe core | 10 | ABSOLUTE execution tickets, one-way `PREPARE -> COMMITTING -> DEAD_LOCK`, 20-tick non-player enforcement, main-hand defense, same-item immunity with per-tick dedup, 1024-block right-click, 1024-block/six-degree swing resolver, and 1024 block/entity reach |
| Static content and assets | 10 | 28 visible item IDs, five blocks, original models/textures, bilingual names, ten card artworks, GUI textures, sounds and record audio |
| Small Loli progression | 13 | Small pickaxe, 14 add-ons, ten-tier superposition, dynamic attack attributes, range mining/attack, Fortune/Looting, auto-smelt, flight, buffs, dodge, reflection and tiered storage |
| Recipes and enchantment | 8 | 20 fixed recipes, three dynamic upgrade recipes and registered `liymod:loli_auto_furnace` enchantment |
| Functional blocks | 9 | Three safe-effect TNT blocks/entities, exact 63x63/1169-block altar and server-authoritative password workbench; its released password registry is empty as upstream |
| Entities and altar summoning | 10 | Loli AI/model/renderer, target exclusions, configurable legacy target teleport, absolute attack, persistence/removal defense, water/height behavior and custom primed TNT renderer |
| Storage and automation | 12 | 81-slot pages, final 100 pages, tiered Small pages, 32 KiB per-stack/4 MiB total encoding limits, blacklist, per-stack AUTO_ACCEPT, mining insertion, nearby pickup only while held, permanent ejection marker and drop-all |
| Configuration and commands | 6 | Validated per-stack settings, persisted Forge server properties, `/loli`, `/loliattack`, entity filters, transactional clear/disarm/kick, reincarnation and soul lists |
| Client screens and networking | 12 | Storage, blacklist, password, four-page config, one-payload range-mining refresh, 0..32768 enchantments, effects and safe relative teleport with dimension blacklist; B/Shift+B/U/N/M/P/K bindings and server validation |
| Cards, record and auxiliary tools | 6 | Card/album viewers, bounded HTTPS online card, original record, dispersal and client-only ghost cleanup |
| Validation, metadata and documentation | 4 | Native Forge source set, Forge metadata, clean build, strict JSON/JAR/safety verification and isolated four-mod production-client co-load evidence |
| **Total** | **100** | Independent reviewer assigns the final achieved score. |

An ID or GUI that exists without its real gameplay consumer is a stub and must
lose credit. Safe substitutions count as implemented only when the upstream
trigger still produces an explicit safe in-game result.

## Canonical static IDs

Items restored around `liymod:loli_pickaxe` and `liymod:loli`:

`small_loli_pickaxe`, `loli_coal_addon`, `loli_iron_addon`,
`loli_gold_addon`, `loli_redstone_addon`, `loli_lapis_addon`,
`loli_diamond_addon`, `loli_emerald_addon`, `loli_obsidian_addon`,
`loli_glow_addon`, `loli_quartz_addon`, `loli_nether_star_addon`,
`loli_auto_furnace_addon`, `loli_fly_addon`, `loli_entity_soul_addon`,
`loli_dispersal`, `bug_entity_clear`, `loli_card`, `loli_card_album`,
`loli_card_online`, and `loli_record`.

Blocks restored as both block and item IDs:

`loli_blue_screen_tnt`, `loli_exit_tnt`, `loli_fail_respond_tnt`,
`loli_altar`, and `password_work_bench`.

Entity IDs: `loli`, `loli_buff_attack_tnt`. Enchantment ID:
`loli_auto_furnace`.

## Compatibility boundaries

The historical IC2, Redstone Flux, Touhou Little Maid, LLibrary and Ice and Fire
hooks remain optional integrations rather than hard dependencies. Their absence
does not block native Forge content. The explicit confrontation target for this
branch is the tested Forge 1.20.1 combination documented in `COMPATIBILITY.md`:
Forever Love Sword, EntityEraser and PIG2.
