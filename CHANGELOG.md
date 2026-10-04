# Changelog

## 1.0.0 for Minecraft 26.3

- Exclude duplicate legacy card PNGs from runtime and sources JARs. Keep the
  originals in the repository and all ten game-addressable textures unchanged.
- Create the `mc/26.3` port from `mc/26.2` without changing older branches.
- Target Minecraft 26.3, Fabric Loader 0.19.5 and Fabric API 0.161.0+26.3.
- Update Fabric Loom to 1.17.21 and Gradle to 9.6.0, with its official
  distribution SHA-256 pinned. Java 25 and official unobfuscated names remain
  required.
- Port main-hand punch handling, permanent/temporary invulnerability, optional
  loot parameters and server-only item-drop prediction to the 26.3 APIs.
- Move the manual-drop storage exclusion mixin to `ServerPlayer`, preserving
  intentional-drop protection after vanilla moved that method.
- Port keyboard binding categories and confirmed card-artwork URI links.
- Update metadata, bilingual requirements and CI release-contract expectations.
- Add 78 executable configuration/safe-effect regression checks to `build` and
  run the full content contract in CI. Add a Fabric Loader JUnit registry/mixin
  bootstrap test for all ten common-side target classes.

### Validation status

- Standalone Java 25 configuration regression checks: passed (78 assertions).
- Both main/client compilation and the full build passed on Java 25 in
  [GitHub Actions](https://github.com/LIy-hub/LoliPickaxe-Fabric/actions/runs/37044696643).
- Release contract passed (metadata, assets, dependency pins and mixins).
- Full content contract passed: 28 items, 5 blocks, 23 recipes.
- Fabric Loader JUnit registry/mixin bootstrap passed. This checks class
  transformation and registry bootstrap; it is not a world or gameplay test.
- Dedicated-server startup and interactive/multiplayer gameplay: not yet verified
  for 26.3. Older content-matrix gameplay claims describe the inherited 26.2
  baseline, not a completed 26.3 playtest.
- The local cloud environment blocks the Unix-domain socket capability probe
  required by unmodified Fabric Loom before compilation. CI uses the official
  tooling unchanged.
