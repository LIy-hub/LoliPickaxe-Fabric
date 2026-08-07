param(
    [string]$JarPath
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) {
        throw $Message
    }
}

$itemIds = @(
    'loli',
    'loli_pickaxe',
    'small_loli_pickaxe',
    'loli_coal_addon',
    'loli_iron_addon',
    'loli_gold_addon',
    'loli_redstone_addon',
    'loli_lapis_addon',
    'loli_diamond_addon',
    'loli_emerald_addon',
    'loli_obsidian_addon',
    'loli_glow_addon',
    'loli_quartz_addon',
    'loli_nether_star_addon',
    'loli_auto_furnace_addon',
    'loli_fly_addon',
    'loli_entity_soul_addon',
    'loli_dispersal',
    'bug_entity_clear',
    'loli_card',
    'loli_card_album',
    'loli_card_online',
    'loli_record',
    'loli_blue_screen_tnt',
    'loli_exit_tnt',
    'loli_fail_respond_tnt',
    'loli_altar',
    'password_work_bench'
)

$standaloneTextureIds = @(
    'loli',
    'loli_pickaxe',
    'loli_coal_addon',
    'loli_iron_addon',
    'loli_gold_addon',
    'loli_redstone_addon',
    'loli_lapis_addon',
    'loli_diamond_addon',
    'loli_emerald_addon',
    'loli_obsidian_addon',
    'loli_glow_addon',
    'loli_quartz_addon',
    'loli_nether_star_addon',
    'loli_auto_furnace_addon',
    'loli_fly_addon',
    'loli_entity_soul_addon',
    'loli_dispersal',
    'bug_entity_clear',
    'loli_card',
    'loli_card_album',
    'loli_card_online',
    'loli_record'
)

$blockIds = @(
    'loli_blue_screen_tnt',
    'loli_exit_tnt',
    'loli_fail_respond_tnt',
    'loli_altar',
    'password_work_bench'
)

$recipeIds = @(
    'loli_altar',
    'loli_auto_furnace_addon',
    'loli_blue_screen_tnt',
    'loli_card_online',
    'loli_coal_addon',
    'loli_diamond_addon',
    'loli_emerald_addon',
    'loli_exit_tnt',
    'loli_fail_respond_tnt',
    'loli_fly_addon',
    'loli_glow_addon',
    'loli_gold_addon',
    'loli_iron_addon',
    'loli_lapis_addon',
    'loli_nether_star_addon',
    'loli_obsidian_addon',
    'loli_quartz_addon',
    'loli_redstone_addon',
    'password_work_bench',
    'small_loli_pickaxe',
    'upgrade_superposition',
    'small_loli_upgrade',
    'loli_pickaxe_upgrade'
)

$itemSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/item/ModItems.java'
) -Raw
$blockSourcePath = Join-Path $projectRoot 'src/main/java/com/liymod/block/ModBlocks.java'
Assert-True (Test-Path -LiteralPath $blockSourcePath) 'ModBlocks.java is missing'
$blockSource = Get-Content -LiteralPath $blockSourcePath -Raw

foreach ($id in $itemIds) {
    Assert-True (($itemSource -match [regex]::Escape('"' + $id + '"')) -or
            ($blockSource -match [regex]::Escape('"' + $id + '"'))) `
        "Registered item id missing from Java sources: liymod:$id"
}
foreach ($id in $blockIds) {
    Assert-True ($blockSource -match [regex]::Escape('"' + $id + '"')) `
        "Registered block id missing from Java sources: liymod:$id"
}

$requiredProgressionSources = @(
    'src/main/java/com/liymod/item/UpgradeItem.java',
    'src/main/java/com/liymod/item/SmallLoliPickaxeItem.java',
    'src/main/java/com/liymod/item/SmallLoliGameplayEvents.java',
    'src/main/java/com/liymod/item/SmallLoliMiningEvents.java',
    'src/main/java/com/liymod/recipe/UpgradeSuperpositionRecipe.java',
    'src/main/java/com/liymod/recipe/SmallLoliUpgradeRecipe.java',
    'src/main/java/com/liymod/recipe/LoliPickaxeUpgradeRecipe.java',
    'src/main/java/com/liymod/recipe/ModRecipes.java'
)
foreach ($relativePath in $requiredProgressionSources) {
    Assert-True (Test-Path -LiteralPath (Join-Path $projectRoot $relativePath)) `
        "Progression source missing: $relativePath"
}

$smallPickaxeSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/item/SmallLoliPickaxeItem.java'
) -Raw
$smallGameplaySource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/item/SmallLoliGameplayEvents.java'
) -Raw
$smallMiningSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/item/SmallLoliMiningEvents.java'
) -Raw
Assert-True ($smallPickaxeSource -match 'refreshEnchantments') `
    'Small Loli Fortune/Looting component synchronization is missing'
Assert-True ($smallPickaxeSource -match 'cycleMiningRadius') `
    'Small Loli range selection is missing'
Assert-True ($smallPickaxeSource -match 'attackNearbyHostiles') `
    'Small Loli range attack is missing'
Assert-True ($smallMiningSource -match 'PlayerBlockBreakEvents\.AFTER') `
    'Small Loli range mining event is missing'
Assert-True ($smallMiningSource -match 'LootTableEvents\.MODIFY_DROPS') `
    'Small Loli auto-smelting event is missing'
Assert-True ($smallGameplaySource -match 'ServerLivingEntityEvents\.ALLOW_DAMAGE') `
    'Small Loli dodge and damage-return event is missing'

$enchantmentResources = @(
    'src/main/resources/data/liymod/enchantment/loli_auto_furnace.json',
    'src/main/resources/data/liymod/tags/enchantment/exclusive_set/loli_auto_furnace.json',
    'src/main/resources/data/liymod/tags/item/enchantable/loli_auto_furnace.json',
    'src/main/resources/data/minecraft/tags/enchantment/non_treasure.json',
    'src/main/resources/data/minecraft/tags/enchantment/smelts_loot.json'
)
foreach ($relativePath in $enchantmentResources) {
    $resourcePath = Join-Path $projectRoot $relativePath
    Assert-True (Test-Path -LiteralPath $resourcePath) `
        "Auto-Smelt enchantment resource missing: $relativePath"
    $null = Get-Content -LiteralPath $resourcePath -Raw | ConvertFrom-Json
}

$allModJava = (Get-ChildItem -LiteralPath (
    Join-Path $projectRoot 'src/main/java'
) -Recurse -Filter '*.java' | Get-Content) -join "`n"
$unsafePatterns = @(
    'ProcessBuilder',
    'Runtime\.getRuntime\(\)\.exec',
    'System\.exit',
    'BlueScreen\.exe'
)
foreach ($pattern in $unsafePatterns) {
    Assert-True ($allModJava -notmatch $pattern) `
        "Unsafe operating-system or JVM behavior found: $pattern"
}

foreach ($id in $recipeIds) {
    $recipePath = Join-Path $projectRoot "src/main/resources/data/liymod/recipe/$id.json"
    Assert-True (Test-Path -LiteralPath $recipePath) "Recipe JSON missing: liymod:$id"
    $null = Get-Content -LiteralPath $recipePath -Raw | ConvertFrom-Json
}

$languages = @('en_us', 'zh_cn')
foreach ($language in $languages) {
    $langPath = Join-Path $projectRoot "src/main/resources/assets/liymod/lang/$language.json"
    Assert-True (Test-Path -LiteralPath $langPath) "Language file missing: $language"
    $lang = Get-Content -LiteralPath $langPath -Raw | ConvertFrom-Json -AsHashtable
    foreach ($id in ($itemIds | Where-Object { $_ -notin $blockIds })) {
        Assert-True $lang.ContainsKey("item.liymod.$id") `
            "Missing $language item translation: item.liymod.$id"
    }
    foreach ($id in $blockIds) {
        Assert-True $lang.ContainsKey("block.liymod.$id") `
            "Missing $language block translation: block.liymod.$id"
    }
}

if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $properties = @{}
    foreach ($line in Get-Content -LiteralPath (Join-Path $projectRoot 'gradle.properties')) {
        if ($line -match '^\s*([^#][^=]*)=(.*)$') {
            $properties[$Matches[1].Trim()] = $Matches[2].Trim()
        }
    }
    $JarPath = Join-Path $projectRoot (
        "build/libs/LoliPickaxe-$($properties.minecraft_version)-$($properties.mod_version).jar"
    )
}
Assert-True (Test-Path -LiteralPath $JarPath -PathType Leaf) "JAR not found: $JarPath"

Add-Type -AssemblyName System.IO.Compression
$archive = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $JarPath))
try {
    foreach ($id in $itemIds) {
        Assert-True ($null -ne $archive.GetEntry("assets/liymod/items/$id.json")) `
            "JAR item definition missing: $id"
        Assert-True ($null -ne $archive.GetEntry("assets/liymod/models/item/$id.json")) `
            "JAR item model missing: $id"
    }
    foreach ($id in $standaloneTextureIds) {
        Assert-True ($null -ne $archive.GetEntry("assets/liymod/textures/item/$id.png")) `
            "JAR item texture missing: $id"
    }
    foreach ($id in $blockIds) {
        Assert-True ($null -ne $archive.GetEntry("assets/liymod/blockstates/$id.json")) `
            "JAR blockstate missing: $id"
        if ($id -ne 'password_work_bench') {
            Assert-True ($null -ne $archive.GetEntry("assets/liymod/models/block/$id.json")) `
            "JAR block model missing: $id"
        }
    }
    foreach ($id in $recipeIds) {
        Assert-True ($null -ne $archive.GetEntry("data/liymod/recipe/$id.json")) `
            "JAR recipe missing: liymod:$id"
    }
    foreach ($relativePath in $enchantmentResources) {
        $entryPath = $relativePath.Replace('src/main/resources/', '')
        Assert-True ($null -ne $archive.GetEntry($entryPath)) `
            "JAR Auto-Smelt resource missing: $entryPath"
    }
} finally {
    $archive.Dispose()
}

Write-Host "VERIFY_FULL_PORT_PROGRESS_OK items=$($itemIds.Count) blocks=$($blockIds.Count) recipes=$($recipeIds.Count)"
