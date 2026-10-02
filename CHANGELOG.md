# Changelog

## 1.0.0 for Minecraft 26.3

- Create the `mc/26.3` port from `mc/26.2` without changing older branches.
- Target Minecraft 26.3, Fabric Loader 0.19.5 and Fabric API 0.161.0+26.3.
- Update Fabric Loom to 1.17.21 and Gradle to 9.6.0, with its official
  distribution SHA-256 pinned. Java 25 and official unobfuscated names remain
  required.
- Update metadata, bilingual requirements and CI release-contract expectations.
- Add 78 executable configuration/safe-effect regression checks to `build` and
  run the full content contract in CI.

### Validation status

- Standalone Java 25 configuration regression checks: passed (78 assertions).
- Full build and content contracts: pending GitHub Actions validation.
- Dedicated-server startup and interactive/multiplayer gameplay: not yet verified
  for 26.3. Older content-matrix gameplay claims describe the inherited 26.2
  baseline, not a completed 26.3 playtest.
- The local cloud environment blocks the Unix-domain socket capability probe
  required by unmodified Fabric Loom before compilation. CI uses the official
  tooling unchanged.
