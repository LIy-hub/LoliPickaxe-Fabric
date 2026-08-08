# Strength confrontation compatibility

This project treats destructive combat mods as optional behavior-level
compatibility targets. Their classes are never compile-time dependencies and no
third-party code or assets are redistributed.

## Audited builds

The following public artifacts were inspected statically on 2026-08-08. They
were not executed.

| Mod | Audited build | Platform | SHA-256 |
| --- | --- | --- | --- |
| [Forever Love Sword](https://modrinth.com/mod/forever-love-sword/versions) | `永爱之刃Neo-26.1~26.2.jar` | Minecraft 26.1-26.2 NeoForge | `347B838910F7223213B7D2E501F10BF5C023E695619186C4FF63A05094F2CF37` |
| [EntityEraser](https://modrinth.com/mod/entityeraser/versions) | `entityeraser-re1.1.0obf.jar` | Minecraft 1.20.1 Forge | `9703C4E47B8DE403AC867577B91D3112808728824202D0F94A5B85FEB0560A90` |
| [PIG2](https://www.curseforge.com/minecraft/mc-mods/pig2/files/all) | `pig2mod-1.20.1-2.4.3.ThisIsOldVersion.jar` | Minecraft 1.20.1 Forge | `5A248B158B6D1D2FC330D16135FA1967EE88C49DB665686580FC4EBC76EE9D54` |

The current LoliPickaxe branch is Fabric 26.2. The audited EntityEraser and
PIG2 jars therefore cannot be loaded into this development instance, and the
Forever Love Sword 26.2 artifact is NeoForge rather than Fabric. Compatibility
is keyed by stable registry ids and is ready for equivalent Fabric/26.2 ports;
narrow reflection hooks activate only when the corresponding mod id is loaded.

## Behavior matrix

| Opponent behavior found in the audited bytecode | Loli response |
| --- | --- |
| Forever Love Sword restores health/abilities and directly sets health to zero, removes, hides or moves targets | Holder health, visibility, death and removal invariants remain guarded. An intentional absolute execution first calls the optional public `ForeverUtils.remove` defense hook, then continues through the existing execution ticket. |
| EntityEraser records entities in a private dead map and removes them from tick, section and persistent entity indexes | Protected players and Loli entities are removed from the optional dead map each server tick. If a foreign eraser removed them from the server index, the server-authoritative recovery path clears only hostile `KILLED`/`DISCARDED` state and re-adds the same entity and UUID. |
| EntityEraser keeps a protected-player set | An intentional absolute execution of a holder removes that target from the optional foreign set before the normal `PREPARE -> COMMITTING -> DEAD_LOCK` transaction. Same-Loli immunity is checked first and is never bypassed. |
| PIG2 writes removal fields, rebuilds entity collections, records killed UUID/type pairs and revives/copies itself | Protected entities use PIG2's optional public `permitEntity` hook plus entity-index recovery. When an absolute execution dead-locks `pig2mod:pig2`, a 60-second quiet window catches loaded revival/copy instances and routes each one back through `LoliErasureService.executeAbsolute`; observing another revival restarts the window. |

The compatibility layer does not launch native code, attach agents, transform
third-party bytecode or weaken the existing same-item immunity. Reflection
failure is logged once and falls back to registry-level protection.

## Registry contract

- `forever_love_sword:forever_love_sword`
- `entityeraser_re:entity_eraser`
- `entityeraser_re:kill_self`
- `pig2mod:pig2`

If a future port changes these ids or its internal public compatibility hook,
the artifact must be re-audited before updating this matrix.
