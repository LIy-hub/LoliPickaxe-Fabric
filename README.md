# LoliPickaxe

> A Fabric 1.20.1 memorial port of the classic LoliPickaxe mod.

This project preserves the concept and gameplay identity of the original
[IslenautsGK/LoliPickaxe](https://github.com/IslenautsGK/LoliPickaxe) while
reimplementing it for modern Fabric. It is distributed under GPL-3.0 in
accordance with the original project's license. See [CREDITS.md](CREDITS.md)
for attribution and third-party asset notes.

LoliPickaxe is a Fabric mod for Minecraft 1.20.1. It adds the Loli Pickaxe, an intentionally overpowered item with special combat, mining, flight, movement, and survival abilities.

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
