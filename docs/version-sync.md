# 26.2 behavior synchronization (2026-10-04)

Frozen source: 3a23c628ea957dfcf862caf7352214574396c434; target Minecraft 26.1.

Ports selectable fluid mining, single/range preview, direct block replacement,
batched section visuals and storage writes, direct mining XP/Mending, 20 ms
completion cooldown, bounded compressed storage network encoding with native
disk NBT preserved, storage/NBT caching with owning-stack isolation, current-page
menu synchronization, per-action kill/item/XP summaries, and bilingual divine
summaries with client-only animated rainbow title.

26.1 native LevelRenderer extraction, renderBlockOutline/ShapeRenderer, getGameRenderState, and RenderRegionCache/rebuildSectionAsync used.
Pinned dependencies, target Java 25 metadata, existing recipes, artwork and GUI
are preserved. Native check includes GUI, storage correctness and codec bounds,
fluid/mining/XP/cooldown, kill aggregation and rainbow chat regressions.

Native isolated runClient startup checks mixin/linkage failures; it does not
establish in-game visuals, multiplayer/network behavior or performance acceptance.
Evidence is recorded separately under build/version-sync-evidence.