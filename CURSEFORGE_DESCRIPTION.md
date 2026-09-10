# LoliPickaxe Fabric

An unbreakable pickaxe with extreme mining power, flight, and attacks that wipe out nearby entities. This Fabric port brings the deliberately overpowered gameplay of [IslenautsGK's LoliPickaxe](https://github.com/IslenautsGK/LoliPickaxe) to newer Minecraft versions. It is maintained by Liy and is not an official continuation by the original authors.

### Getting the pickaxe

Find the Loli Pickaxe and Loli Fragment in the mod's creative inventory tab. There is no crafting recipe. With commands enabled, use:

```text
/give @s liymod:loli_pickaxe
```

### Using it

- **Left-click blocks** for instant mining. Selected ores and otherwise restricted blocks have special drops; ores can drop their corresponding storage blocks.
- **Left-click an entity** for the pickaxe's direct attack.
- **Right-click** to attack entities in a box extending 32 blocks outward from the player, with lightning at successfully removed targets.
- **Hold it in your main hand** for flight, protection from ordinary damage, and continuously restored health and air. Holding it does not increase movement speed.
- **Two main-hand holders** cannot defeat each other with the pickaxe. Their attacks trigger alternating immunity sounds.

The item does not lose durability and is fireproof. Its attacks can affect players and nearby non-hostile entities, and its mining can affect normally restricted blocks. This is intended for worlds that allow this level of power; server owners should decide who receives it.

### Installation

Install Fabric Loader 0.19.3 or newer, Fabric API, and the matching LoliPickaxe file on both the server and each client.

| Minecraft | Java |
| --- | --- |
| 1.20.1–1.20.4 | 17 |
| 1.20.5–1.20.6; 1.21–1.21.11 | 21 |
| 26.1, 26.1.1, 26.1.2, 26.2 | 25 |

Use files built for the same Minecraft version. Compatibility with third-party combat, claims, and protection mods depends on their implementation.

## 中文

一把不会损坏的镐，集强力挖掘、飞行和范围攻击于一身。LoliPickaxe Fabric（氪金萝莉）由 Liy 维护，将 [IslenautsGK 原作](https://github.com/IslenautsGK/LoliPickaxe) 的超强度玩法带到新版 Fabric 环境。它是纪念移植版，并非原作者推出的官方续作。

### 获取方式

在模组的创造物品栏中可以找到“氪金萝莉”和“萝莉碎片”。当前没有合成配方。开启命令后，也可以输入：

```text
/give @s liymod:loli_pickaxe
```

### 使用方式

- **左键方块**：快速破坏方块；部分矿石和通常受限的方块带有特殊掉落，矿石可掉落对应的储存方块。
- **左键实体**：使用镐子的直接攻击。
- **右键**：攻击玩家周围、向外扩展 32 格的方形区域内的实体，并在成功清除的目标处生成闪电。
- **主手持有**：获得飞行、常规伤害防护，以及持续的生命与空气值恢复；不会额外提高移动速度。
- **双方都主手持有**：无法用这把镐击败对方，攻击时会交替播放免疫提示音。

镐子不消耗耐久，掉落物防火。范围攻击会波及玩家和友好实体，挖掘能力也可影响通常受限的方块。它适合允许超强度道具的世界，服务器中应由服主决定发放范围。

### 安装

需要 Fabric Loader 0.19.3 或更高版本、Fabric API，以及对应游戏版本的 LoliPickaxe。客户端与服务端都要安装。

- Minecraft 1.20.1–1.20.4：Java 17。
- Minecraft 1.20.5–1.20.6、1.21–1.21.11：Java 21。
- Minecraft 26.1、26.1.1、26.1.2、26.2：Java 25。

各文件须对应相同的 Minecraft 版本。与其他战斗、领地或保护模组的兼容情况取决于对方实现。

## Credits and license / 署名与许可

Original project: **IslenautsGK and contributors**. Fabric port: **Liy**. Licensed under [GPL-3.0-only](https://github.com/LIy-hub/LoliPickaxe-Fabric/blob/main/LICENSE). See [CREDITS.md](https://github.com/LIy-hub/LoliPickaxe-Fabric/blob/main/CREDITS.md) for contributor and asset notices.

原作由 **IslenautsGK 及其贡献者**开发，Fabric 移植由 **Liy** 维护。项目采用 GPL-3.0-only，作者与素材署名见上述文件。

[Source code and issue tracker / 源码与问题反馈](https://github.com/LIy-hub/LoliPickaxe-Fabric)
