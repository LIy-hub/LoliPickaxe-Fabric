# Forge 1.20.1 strength-confrontation matrix

Target: Minecraft 1.20.1, Forge 47.4.22, Java 17.

Third-party JARs are test inputs only and are not stored in this repository or bundled in the LoliPickaxe JAR.

| Mod | Tested artifact | SHA-256 |
|---|---|---|
| Forever Love Sword | `ForeverLoveSword-1.20.1-1.8.1.jar` | `ED3EC2CAD88FDE6A02F534629F4CE6A0D9CF5B0C4224FF15D5342FED94FB46E3` |
| EntityEraser | `entityeraser-re1.1.0obf.jar` | `9703C4E47B8DE403AC867577B91D3112808728824202D0F94A5B85FEB0560A90` |
| PIG2 | `pig2mod-1.20.1-2.4.3.ThisIsOldVersion.jar` | `5A248B158B6D1D2FC330D16135FA1967EE88C49DB665686580FC4EBC76EE9D54` |

## Runtime policy

- Compatibility is optional and reflection-based; absent mods do not affect standalone loading.
- The three hooks use stable mod IDs plus verified 1.20.1 class/registry surfaces.
- Same-item Loli immunity is checked before any external defense is changed.
- Forever Love Sword handling removes only the target UUID from its private defense set. Protected Loli holders are also removed every tick from FLS's persistent player/class death lists; because FLS records deaths by Java class, clearing `ServerPlayer` necessarily protects every server player of that class while a Loli holder is present. Its own destructive `kill`/`killEntity` functions are never called.
- EntityEraser handling protects active Loli holders and removes the target profile only for an absolute Loli execution.
- PIG2 handling calls its public `permitEntity(Entity)` for protected holders and applies a bounded one-minute suppression window after an absolute execution.
- No third-party class is linked at compile time and no operating-system/JVM attack is implemented.

## Isolated load evidence

On 2026-08-08, all four JARs were launched together in a formal obfuscated Forge `forgeclient` runtime inside Ubuntu 24.04 WSL2. Host `C:` and `D:` mounts were detached before third-party code started. An earlier source-equivalent candidate reached the main menu, completed resource reload, initialized OpenAL and created all texture atlases without a fatal lifecycle or Mixin error.

The final-candidate rerun loaded and discovered all four JARs with no fatal lifecycle or Mixin error, but did not reach the main menu inside the bounded test window. A thread dump showed active CPU progress inside EntityEraser's ASM transformer (`EntityEraserTransformer.b2n` / `ClassReader.readCode`), not a deadlock or a LoliPickaxe exception. This is recorded as a timeout, not as a claimed final-hash main-menu pass.

The ForgeGradle `forgeclientuserdev` task is not a valid co-load test for Forever Love Sword: its Mixin shadows raw SRG method names and fails only after the development runtime remaps Minecraft to Mojang names. The same artifact loaded in the formal SRG production client.

The third-party artifacts have two independent limitations:

- Forever Love Sword logs missing models for `death` and `test_dead`, and missing attributes for its two rainbow-lightning entities. These warnings do not stop the main menu.
- Forever Love Sword and PIG2 reference client classes from common initialization, so a dedicated-server-only four-mod test fails before normal server startup. Co-load testing must use a client/integrated server.

These are properties of the tested third-party JARs, not LoliPickaxe load failures.
