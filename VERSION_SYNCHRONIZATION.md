# Forge 1.20.1 behavior synchronization

The native Forge implementation tracks Fabric 26.2 behavioral baseline
`3a23c628ea957dfcf862caf7352214574396c434`. Minecraft 1.20.1, Forge
47.4.22, Java 17 bytecode, dependencies, assets and mod IDs are preserved.

- `stop_on_liquid` retains its saved key and now selects/removes pure fluids.
  Waterlogged solid blocks still mine normally. `range_preview` defaults on.
- Final-tool single/range mining sends an authoritative request instead of
  vanilla progressive prediction. Permission, distance, world/spawn bounds,
  native Forge break cancellation and creative behavior remain enforced.
  Range actions leave a 20 ms gap after completion on both sides; radius zero
  remains immediate. Accepted results update a bounded group of sections with
  the native renderer's player-priority flag.
- Ore/smelting XP is collected before orbs spawn, credits Mending first and
  then the player. Nearby merged orbs contribute all represented points.
- Storage insertion batches decode/measure once and save at scope exit,
  including exceptional exits. Decoded caches belong to each owning stack and
  compare mutable NBT snapshots; public loads return independent item copies.
  Forge auto-accept and its item-ID blacklist remain intact.
- Saves still store `LoliStorage` as the original `Slot`/`Stack` list. Only
  network buffers replace a large subtree with versioned gzip data. Serialized
  storage stays capped at 4 MiB, individual stored stacks at 32 KiB, compressed
  wire data at 1 MiB, and expanded NBT allocation at 16 MiB. Invalid versions,
  corrupt gzip, oversized expansion and trailing data fail decoding. The
  native 1.20.1 NBT accounter measures allocation separately from serialized
  bytes. Incompressible data that exceeds the wire cap is rejected safely.
  Network protocol 3 requires matching updated Forge peers; disk data is unchanged.
- Left-click, right-click and automatic execution each emit one localized
  kill/item/XP summary for newly committed live targets. Client-only chat
  title animation reuses cached style sequences without changing statistics,
  refreshing chat or sending animation packets.

Native recipe serializers already implement the 1.20.1 JSON/network contract;
modern Fabric recipe/component codecs are not copied into Forge.

Verification: `gradlew --no-daemon --max-workers=1 --no-parallel check build`,
then `scripts/verify-full-port.ps1`. Headless regression tasks cover GUI layout,
storage pages and wire bounds, XP/cooldown accounting, nested kill summaries and
cached rainbow formatting. Native isolated `runClient` startup is checked
separately. Gameplay, world/save reload, multiplayer, visuals and performance
require further in-game acceptance by the user.
