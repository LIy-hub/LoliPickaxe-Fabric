# 26.2 behavior synchronization

Source contract: 26.2 commit 3a23c628ea957dfcf862caf7352214574396c434.
The target uses its existing Java 17 and pinned dependencies. Existing GUI and artwork remain unchanged.

Ports fluid selection/removal, authoritative single/range mining, configurable preview,
section-batched client rebuilds, direct block replacement, batched/indexed storage,
direct mining XP with native Mending and merged orb counts, 20 ms completion cooldown,
per-action kill/reward summaries and the localized animated divine chat title.

Legacy item NBT remains the native disk/save format. Only item packets exceeding 1 MiB
storage accounting use the NetworkVersion=1 gzip envelope; decoding keeps a 4 MiB
quota. Cached owning-stack storage is independent after copy and refreshes after
external snapshot replacement or in-place legacy NBT edits. Client menus receive only
their visible authoritative slots and never rewrite full server storage.

Native build/check, seven regression entry points, release-contract checks and isolated
client startup are required. Startup does not establish in-game visuals, network/world
interaction or runtime performance; the user operates Minecraft for those checks.
