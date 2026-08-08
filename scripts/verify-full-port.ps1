param([string]$JarPath)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
function Assert-True([bool]$Condition, [string]$Message) { if (-not $Condition) { throw $Message } }
function Source([string]$Relative) { Get-Content -Raw -LiteralPath (Join-Path $root $Relative) }

$itemIds = @(
    'loli','loli_pickaxe','small_loli_pickaxe','loli_coal_addon','loli_iron_addon','loli_gold_addon',
    'loli_redstone_addon','loli_lapis_addon','loli_diamond_addon','loli_emerald_addon','loli_obsidian_addon',
    'loli_glow_addon','loli_quartz_addon','loli_nether_star_addon','loli_auto_furnace_addon','loli_fly_addon',
    'loli_entity_soul_addon','loli_dispersal','bug_entity_clear','loli_card','loli_card_album','loli_card_online',
    'loli_record','loli_blue_screen_tnt','loli_exit_tnt','loli_fail_respond_tnt','loli_altar','password_work_bench'
)
$blockIds = @('loli_blue_screen_tnt','loli_exit_tnt','loli_fail_respond_tnt','loli_altar','password_work_bench')

$properties = Source 'gradle.properties'
Assert-True ($properties -match '(?m)^minecraft_version=1\.20\.1$') 'Minecraft target is not 1.20.1'
Assert-True ($properties -match '(?m)^forge_version=47\.4\.22$') 'Forge target is not 47.4.22'
Assert-True ((Source 'build.gradle') -match "sourceSets\.main\.java\.setSrcDirs\(\['src/forge/java'\]\)") 'Forge source set is not isolated'
Assert-True ((Source 'src/main/resources/META-INF/mods.toml') -match 'modLoader="javafml"') 'Forge mods.toml is missing'

foreach ($id in $itemIds) {
    Assert-True (Test-Path (Join-Path $root "src/main/resources/assets/liymod/models/item/$id.json")) "Missing item model: $id"
}
foreach ($id in $blockIds) {
    Assert-True (Test-Path (Join-Path $root "src/main/resources/assets/liymod/blockstates/$id.json")) "Missing blockstate: $id"
}

$jsonFiles = Get-ChildItem (Join-Path $root 'src/main/resources') -Recurse -Filter '*.json'
if ($PSVersionTable.PSEdition -eq 'Core') {
    foreach ($json in $jsonFiles) {
        $document = [System.Text.Json.JsonDocument]::Parse((Get-Content -Raw -Encoding UTF8 -LiteralPath $json.FullName))
        $document.Dispose()
    }
} else {
    Add-Type -AssemblyName System.Web.Extensions
    $serializer = New-Object System.Web.Script.Serialization.JavaScriptSerializer
    $serializer.MaxJsonLength = [int]::MaxValue
    foreach ($json in $jsonFiles) {
        $null = $serializer.DeserializeObject((Get-Content -Raw -Encoding UTF8 -LiteralPath $json.FullName))
    }
}
$recipes = @(Get-ChildItem (Join-Path $root 'src/main/resources/data/liymod/recipes') -Filter '*.json')
Assert-True ($recipes.Count -eq 23) "Expected 23 recipes, found $($recipes.Count)"

$content = Source 'src/forge/java/com/liymod/registry/ModContent.java'
Assert-True (($content | Select-String -AllMatches 'block\("').Matches.Count -eq 5) 'Expected five block registrations'
Assert-True ($content -match 'LOLI_AUTO_FURNACE') 'Auto Furnace enchantment is not registered'

$finalItem = Source 'src/forge/java/com/liymod/item/FinalLoliPickaxeItem.java'
$resolver = Source 'src/forge/java/com/liymod/combat/LoliAttackResolver.java'
$events = Source 'src/forge/java/com/liymod/event/ForgeEvents.java'
$network = Source 'src/forge/java/com/liymod/network/ModNetwork.java'
$storage = Source 'src/forge/java/com/liymod/storage/LoliStorageData.java'
$tier = Source 'src/forge/java/com/liymod/item/LoliToolTier.java'
$smallItem = Source 'src/forge/java/com/liymod/item/SmallLoliPickaxeItem.java'
$settings = Source 'src/forge/java/com/liymod/config/FinalToolSettings.java'
$logicalEnchantments = Source 'src/forge/java/com/liymod/config/LogicalEnchantments.java'
$mixins = Source 'src/main/resources/liymod.mixins.json'
$entityMixin = Source 'src/forge/java/com/liymod/mixin/EntityMixin.java'
$livingMixin = Source 'src/forge/java/com/liymod/mixin/LivingEntityMixin.java'
$enchantmentMixin = Source 'src/forge/java/com/liymod/mixin/EnchantmentHelperMixin.java'
$damageService = Source 'src/forge/java/com/liymod/combat/LoliErasureService.java'
$legacy = Source 'src/forge/java/com/liymod/combat/LoliLegacyExecutionPolicy.java'
$execution = Source 'src/forge/java/com/liymod/combat/LoliExecutionManager.java'
$compat = Source 'src/forge/java/com/liymod/compat/StrengthConfrontation.java'
$loliEntity = Source 'src/forge/java/com/liymod/entity/LoliEntity.java'
$serverConfig = Source 'src/forge/java/com/liymod/config/LoliServerConfig.java'
$cardViewer = Source 'src/forge/java/com/liymod/client/card/CardViewerScreen.java'
Assert-True ($finalItem -match 'RIGHT_CLICK_RANGE\s*=\s*1024\.0D') 'Right-click execution is not 1024 blocks'
Assert-True ($finalItem -match 'hand == InteractionHand\.MAIN_HAND' -and $finalItem -match 'new ArrayList') 'Right-click execution is not main-hand-only or snapshot-safe'
Assert-True ($resolver -match 'MAX_RANGE\s*=\s*1024\.0D' -and $resolver -match 'Math\.toRadians\(6\.0D\)') '1024-block/six-degree swing resolver is missing'
Assert-True ($resolver -match 'isDeadLocked\(attacker\)') 'Dead-locked players can still use the swing resolver'
Assert-True ($events -match 'ForgeMod\.BLOCK_REACH' -and $events -match 'ForgeMod\.ENTITY_REACH') 'Block/entity reach modifiers are missing'
Assert-True (($events | Select-String -AllMatches 'Map\.entry\(').Matches.Count -eq 27) 'Expected 27 special drops'
Assert-True ($events -match 'for \(int x = -radius' -and $events -notmatch 'schedule|enqueue') 'Range mining is not one server action'
Assert-True ($events -match 'setBlock\(pos, replacement, Block\.UPDATE_NEIGHBORS' -and $events -notmatch 'destroyBlock\(pos') 'Range mining still emits progressive per-block server updates'
Assert-True ($events -match 'new BlockEvent\.BreakEvent' -and $events -match 'MinecraftForge\.EVENT_BUS\.post\(breakEvent\)' -and $events -match 'breakEvent\.isCanceled\(\)' -and $events -match 'if \(creative\) return true') 'Range mining bypasses Forge break cancellation or creative no-drop semantics'
Assert-True ($network -match 'RangeMiningPacket' -and $network -match 'sendRangeMining\(ServerPlayer initiator' -and $network -match 'recipients\.add\(initiator\)' -and $network -match 'MAX_BLOCKS\s*=\s*4096') 'Single bounded range-mining sync payload or guaranteed initiator delivery is missing'
Assert-True ($events -match 'AMETHYST_BLOCK_BREAK' -and $events -match 'ExperienceOrb\.award') 'Range-mining sound or aggregate XP is missing'
Assert-True ($events -match 'LoliStorageData\.autoAccept\(tool\)') 'Mining does not use per-stack AUTO_ACCEPT'
Assert-True ($storage -match 'LoliStorageEjected' -and $storage -match 'autoAccept\(ItemStack tool\)' -and $storage -match '32 \* 1024' -and $storage -match '4 \* 1024 \* 1024' -and $storage -match 'encodedSize\(list\)' -and $storage -match 'canStoreAt' -and $storage -match 'isStorageTool') 'Storage ejection/AUTO_ACCEPT/exact NBT guards are missing'
Assert-True ($smallItem -match 'harvestLevel\(ItemStack stack\)' -and $smallItem -match 'NEEDS_DIAMOND_TOOL') 'Small Loli dynamic harvest level is missing'
Assert-True ($smallItem -match 'LogicalEnchantments\.setLevel' -and $smallItem -notmatch 'stack\.enchant') 'Small Loli enchantments can duplicate every inventory tick'
Assert-True ($smallItem -match 'liymod\$getAttackStrengthTicker' -and $smallItem -match 'liymod\$setAttackStrengthTicker' -and $smallItem -match 'resetAttackStrengthTicker\(\)') 'Small Loli range attacks do not share and then consume one attack charge'
Assert-True ($tier -match 'getLevel\(\) \{ return Integer\.MAX_VALUE; \}' -and $tier -match 'getAttackDamageBonus\(\) \{ return Float\.POSITIVE_INFINITY; \}' -and $tier -match 'getEnchantmentValue\(\) \{ return 30; \}' -and $tier -match 'Ingredient\.of\(ModContent\.LOLI') 'Final tool tier/repair/enchantability contract is wrong'
Assert-True ($settings -match 'stop_on_liquid' -and $settings -match 'auto_kill_range') 'Final per-stack mining/attack range settings are incomplete'
Assert-True ($network -match 'Math\.min\(32768, packet\.level\)') 'Enchantment packet does not accept level 32768'
Assert-True ($logicalEnchantments -match 'MAXIMUM_LEVEL\s*=\s*32768' -and $logicalEnchantments -match 'putInt\("lvl"' -and $enchantmentMixin -match 'LogicalEnchantments\.MAXIMUM_LEVEL' -and $mixins -match 'EnchantmentHelperMixin') 'Level 32768 is not connected to the real enchantment consumer path'
Assert-True ($execution -match 'PREPARE|LoliExecutionTicket' -and $execution -match 'NON_PLAYER_TICKET_LIFETIME\s*=\s*20') 'Execution ticket lifecycle is missing'
Assert-True ($legacy -match 'PreparedExecution implements AutoCloseable' -and $legacy -match 'reincarnation_list') 'Transactional legacy policy is missing'
Assert-True ($compat -match 'ForeverUtils' -and $compat -match 'DeathList' -and $compat -match 'disableEraserDefense' -and $compat -match 'permitEntity' -and $compat -match 'void reset\(MinecraftServer server\)') 'Three strength-confrontation hooks are incomplete'
Assert-True ($damageService -match 'loli_damage') 'Registered Loli damage type has no execution consumer'
Assert-True ($entityMixin -match 'setInvisible' -and $entityMixin -match 'isAttackable' -and $entityMixin -match 'setPosRaw') 'Protected-holder visibility, attackability or hostile-position guard is missing'
Assert-True ($livingMixin -match 'safeMaximumHealth' -and $livingMixin -match 'isTerminal') 'Protected/terminal health semantics are incomplete'
Assert-True ($serverConfig -match '"loli_attack", "true"' -and $serverConfig -match '"loli_teleport", "true"' -and $serverConfig -match '"loli_speed", "1\.0"' -and $loliEntity -match 'teleportTo\(target\.getX') 'Legacy Loli AI attack/teleport/speed settings are missing or unused'
Assert-True ($cardViewer -match 'pose\(\)\.scale' -and $cardViewer -match 'image\.width, image\.height, image\.width, image\.height') 'Card viewer does not render the complete image when scaled'
foreach ($mixin in @('PlayerMixin','ProjectileUtilMixin','TargetPredicateMixin','ServerGamePacketListenerMixin')) {
    Assert-True ($mixins -match [regex]::Escape($mixin)) "Protection mixin is missing: $mixin"
}
foreach ($block in $blockIds) {
    Assert-True (Test-Path (Join-Path $root "src/main/resources/data/liymod/loot_tables/blocks/$block.json")) "Missing block loot table: $block"
}
Assert-True (-not ((Get-ChildItem (Join-Path $root 'src/main/resources/data/liymod/advancements') -Recurse -Filter '*.json' | Get-Content -Raw) -match '"items"\s*:\s*"')) 'Forge 1.20.1 advancement item arrays are malformed'
Assert-True (Test-Path (Join-Path $root 'src/main/resources/assets/liymod/sounds/lolisuccess.ogg')) 'lolisuccess.ogg is missing'

$danger = Get-ChildItem (Join-Path $root 'src/forge/java') -Recurse -Filter '*.java' | Select-String -Pattern 'ProcessBuilder|System\.exit|Runtime\.getRuntime|new\s+Thread\s*\(|while\s*\(\s*true\s*\)'
Assert-True ($null -eq $danger) 'Dangerous OS/JVM behavior found in production sources'
$thirdParty = git -C $root ls-files '*.jar' | Where-Object { $_ -match 'ForeverLove|entityeraser|pig2mod' }
Assert-True ($null -eq $thirdParty) 'Third-party confrontation JAR is tracked in the repository'

if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $JarPath = Get-ChildItem (Join-Path $root 'build/libs') -Filter '*.jar' | Where-Object { $_.Name -notmatch 'sources|slim' } | Select-Object -First 1 -ExpandProperty FullName
}
Assert-True (Test-Path -LiteralPath $JarPath) 'Built Forge JAR is missing'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $JarPath))
try {
    $entries = @{}; foreach ($entry in $zip.Entries) { $entries[$entry.FullName] = $true }
    foreach ($required in @(
        'META-INF/mods.toml','liymod.mixins.json','com/liymod/LiyMod.class',
        'com/liymod/combat/LoliExecutionManager.class','com/liymod/combat/LoliAttackResolver.class',
        'com/liymod/combat/LoliLegacyExecutionPolicy.class','com/liymod/compat/StrengthConfrontation.class',
        'com/liymod/enchantment/AutoFurnaceEnchantment.class','assets/liymod/sounds/lolisuccess.ogg'
    )) { Assert-True $entries.ContainsKey($required) "JAR entry missing: $required" }
    Assert-True (-not $entries.ContainsKey('fabric.mod.json')) 'Fabric metadata leaked into Forge JAR'
} finally { $zip.Dispose() }

Write-Output "FORGE_FULL_PORT_OK items=$($itemIds.Count) blocks=$($blockIds.Count) recipes=$($recipes.Count) json=$($jsonFiles.Count) jar=$JarPath"
