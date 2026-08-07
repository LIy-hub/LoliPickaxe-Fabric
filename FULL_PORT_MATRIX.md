# Full content restoration matrix

## Acceptance baseline

- Target branch: `mc/26.2` (Minecraft 26.2, Fabric Loader 0.19.3,
  Fabric API 0.155.2+26.2, Java 25).
- Legacy source: IslenautsGK/LoliPickaxe `master` at
  `c9a01e493cc7c8c265837b2d43f29a28a61d59fa` (1.12.2 / 1.2.16f).
- Existing Fabric Loli Pickaxe behavior is frozen by `PORTING_BASELINE.md`.
- Completion requires an independent review result of **Pass** and a measured
  completeness strictly greater than 90%. Any missing part must be listed
  separately.

## Weighted review areas

| Area | Weight | Upstream scope | Status |
|---|---:|---|---|
| Stable Fabric Loli core | 10 | Existing pickaxe, execution tickets, holder defense, special drops, same-item immunity | Preserved; regression checks pending |
| Static content and assets | 10 | 21 fixed items, one default record, five block items, textures/models/translations | Complete |
| Small Loli progression | 13 | Small pickaxe, 14 add-ons, ten-tier superposition, transformed mining/combat stats | Complete: tier data, formulas, Fortune/Looting, range mining/attack, auto-smelt, flight, buffs, dodge, damage return and storage integration |
| Recipes and enchantment | 8 | 20 legacy JSON recipes, dynamic upgrade recipes, Auto Furnace enchantment | Complete: 20 static recipes, 3 dynamic recipes and native 26.2 Auto-Smelt enchantment |
| Functional blocks | 9 | Three effect TNT blocks, Loli Altar, Password Workbench | Complete: three TNT blocks, exact 63x63 altar and server-authoritative password workbench; its released password recipe registry intentionally starts empty like upstream |
| Entities and altar summoning | 10 | Loli entity, target/attack/swim AI, effect TNT entity and rendering | Complete: both entity ids, persistent/invulnerable Loli AI, safe effect TNT, exact altar ritual and client renderers |
| Storage and automation | 12 | Internal inventories, blacklist, auto-accept, drop-all, auto-smelt and range mining | Complete: bounded 81-slot paging (100 final-pickaxe pages), tiered Small Loli pages, blacklist, nearby auto-accept, direct mining-drop insertion, drop-all, auto-smelt and range mining |
| Configuration and commands | 6 | Per-item settings, server config, `/loli`, `/loliattack` | Not started |
| Client screens and networking | 12 | Config, enchantment, potion, folding, storage, blacklist and password screens plus payloads | Safe TNT, password, storage and blacklist screens/payloads complete, including B/Shift+B/U and ordered page synchronization; config, enchantment, potion and folding screens pending |
| Cards, record and auxiliary tools | 6 | Card, album, bounded online card, dispersal, client-ghost cleanup and music disc | Not started |
| Validation, metadata and documentation | 4 | Build, server smoke test, content contract, attribution and migration notes | In progress |
| **Total** | **100** | | |

The independent reviewer may lower a score when an id exists but its behavior
is only a stub. The safety substitutions documented in `PORTING_BASELINE.md`
count as implemented only when the original trigger and a clear safe in-game
result both work.

## Canonical static ids

Items restored around the existing `liymod:loli_pickaxe` and `liymod:loli`:

`small_loli_pickaxe`, `loli_coal_addon`, `loli_iron_addon`,
`loli_gold_addon`, `loli_redstone_addon`, `loli_lapis_addon`,
`loli_diamond_addon`, `loli_emerald_addon`, `loli_obsidian_addon`,
`loli_glow_addon`, `loli_quartz_addon`, `loli_nether_star_addon`,
`loli_auto_furnace_addon`, `loli_fly_addon`, `loli_entity_soul_addon`,
`loli_dispersal`, `bug_entity_clear`, `loli_card`, `loli_card_album`,
`loli_card_online`, and `loli_record`.

Blocks restored as both block and item ids:

`loli_blue_screen_tnt`, `loli_exit_tnt`, `loli_fail_respond_tnt`,
`loli_altar`, and `password_work_bench`.

Entity ids: `loli` and `loli_buff_attack_tnt`. Enchantment id:
`loli_auto_furnace`.

## Compatibility boundaries

Optional 1.12.2 integrations with IC2, Redstone Flux, Touhou Little Maid,
LLibrary and Ice and Fire are recorded as optional compatibility surfaces.
Their absence must not prevent the standalone Fabric mod from loading or using
its native content. Equivalent modern integrations may be added when the
corresponding dependency is present.
