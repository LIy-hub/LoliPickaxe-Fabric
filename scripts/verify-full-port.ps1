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

$javaSourceRoots = @(
    (Join-Path $projectRoot 'src/main/java'),
    (Join-Path $projectRoot 'src/client/java')
)
$allModJava = ($javaSourceRoots | ForEach-Object {
    Get-ChildItem -LiteralPath $_ -Recurse -Filter '*.java'
} | Get-Content) -join "`n"
$unsafePatterns = @(
    'ProcessBuilder',
    'Runtime\.getRuntime\(\)\.exec',
    'System\.exit',
    'BlueScreen\.exe',
    'new\s+Thread\s*\(',
    'while\s*\(\s*true\s*\)'
)
foreach ($pattern in $unsafePatterns) {
    Assert-True ($allModJava -notmatch $pattern) `
        "Unsafe operating-system or JVM behavior found: $pattern"
}

$functionalSources = @(
    'src/main/java/com/liymod/block/BuffAttackTntBlock.java',
    'src/main/java/com/liymod/block/LoliAltarBlock.java',
    'src/main/java/com/liymod/entity/ModEntities.java',
    'src/main/java/com/liymod/entity/LoliEntity.java',
    'src/main/java/com/liymod/entity/LoliPrimedTntEntity.java',
    'src/main/java/com/liymod/safe/SafeTntEffect.java',
    'src/main/java/com/liymod/safe/SafeTntEffectPayload.java',
    'src/main/java/com/liymod/safe/SafeTntEffectService.java',
    'src/main/java/com/liymod/network/ModNetworking.java',
    'src/client/java/com/liymod/client/LiyModClient.java',
    'src/client/java/com/liymod/client/render/LoliEntityModel.java',
    'src/client/java/com/liymod/client/render/LoliEntityRenderer.java',
    'src/client/java/com/liymod/client/safe/BlueScreenEffectScreen.java',
    'src/client/java/com/liymod/client/safe/FailRespondEffectScreen.java'
)
foreach ($relativePath in $functionalSources) {
    Assert-True (Test-Path -LiteralPath (Join-Path $projectRoot $relativePath)) `
        "Functional block/entity source missing: $relativePath"
}

$storageAndPasswordSources = @(
    'src/main/java/com/liymod/block/PasswordWorkbenchBlock.java',
    'src/main/java/com/liymod/password/PasswordRecipe.java',
    'src/main/java/com/liymod/password/PasswordRecipeRegistry.java',
    'src/main/java/com/liymod/menu/PasswordWorkbenchMenu.java',
    'src/main/java/com/liymod/menu/StorageMenu.java',
    'src/main/java/com/liymod/menu/BlacklistMenu.java',
    'src/main/java/com/liymod/storage/LoliStorageData.java',
    'src/main/java/com/liymod/storage/LoliStorageEvents.java',
    'src/main/java/com/liymod/network/PasswordUpdatePayload.java',
    'src/main/java/com/liymod/network/StorageOpenPayload.java',
    'src/main/java/com/liymod/network/StoragePagePayload.java',
    'src/main/java/com/liymod/network/StoragePageSyncPayload.java',
    'src/main/java/com/liymod/network/StorageDropAllPayload.java',
    'src/main/java/com/liymod/network/BlacklistUpdatePayload.java',
    'src/client/java/com/liymod/client/input/LoliKeyMappings.java',
    'src/client/java/com/liymod/client/storage/LoliStorageClient.java',
    'src/client/java/com/liymod/client/screen/PasswordWorkbenchScreen.java',
    'src/client/java/com/liymod/client/screen/LoliStorageScreen.java',
    'src/client/java/com/liymod/client/screen/LoliBlacklistScreen.java'
)
foreach ($relativePath in $storageAndPasswordSources) {
    Assert-True (Test-Path -LiteralPath (Join-Path $projectRoot $relativePath)) `
        "Storage/password source missing: $relativePath"
}

$passwordPayloadSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/network/PasswordUpdatePayload.java'
) -Raw
$storageSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/storage/LoliStorageData.java'
) -Raw
$storageEventsSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/storage/LoliStorageEvents.java'
) -Raw
$storageMenuSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/menu/StorageMenu.java'
) -Raw
$storageKeySource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/client/java/com/liymod/client/input/LoliKeyMappings.java'
) -Raw
Assert-True ($passwordPayloadSource -match 'MAX_CODE_POINTS\s*=\s*64') `
    'Password payload must remain bounded to 64 Unicode code points'
Assert-True ($passwordPayloadSource -match 'MAX_UTF8_BYTES\s*=\s*256') `
    'Password payload must remain bounded to 256 UTF-8 bytes'
Assert-True ($storageSource -match 'SLOTS_PER_PAGE\s*=\s*81') `
    'Loli storage page size must remain 81 slots'
Assert-True ($storageSource -match 'FINAL_PAGE_COUNT\s*=\s*100') `
    'Final Loli storage must retain 100 pages'
Assert-True ($storageSource -match 'MAX_TOTAL_NBT_BYTES\s*=\s*4\s*\*\s*1024\s*\*\s*1024') `
    'Loli storage total NBT safety bound is missing'
Assert-True ($storageSource -match 'isStorageItem\(stack\)') `
    'Loli storage self-nesting rejection is missing'
Assert-True ($storageEventsSource -match 'COLLECT_RANGE\s*=\s*4\.0D') `
    'Nearby Loli storage collection range is missing'
Assert-True ($smallMiningSource -match 'storage\.insert') `
    'Small Loli mining drops are not routed through storage'
Assert-True ($storageMenuSource -match 'StoragePageSyncPayload') `
    'Ordered storage page synchronization is missing'
Assert-True ($storageKeySource -match 'InputConstants\.KEY_B') `
    'Loli storage B/Shift+B key binding is missing'
Assert-True ($storageKeySource -match 'InputConstants\.KEY_U') `
    'Loli blacklist U key binding is missing'

$entitiesSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/entity/ModEntities.java'
) -Raw
Assert-True ($entitiesSource -match '"loli"') 'Entity id missing: liymod:loli'
Assert-True ($entitiesSource -match '"loli_buff_attack_tnt"') `
    'Entity id missing: liymod:loli_buff_attack_tnt'

$altarPatternPath = Join-Path $projectRoot (
    'src/main/resources/data/liymod/loli_altar_pattern/default.json'
)
Assert-True (Test-Path -LiteralPath $altarPatternPath) 'Exact Loli altar pattern is missing'
$altarPattern = Get-Content -LiteralPath $altarPatternPath -Raw | ConvertFrom-Json
$altarRows = @($altarPattern.rows_by_x)
Assert-True ($altarRows.Count -eq 63) 'Loli altar pattern must contain 63 rows'
$altarCharacters = $altarRows -join ''
Assert-True ($altarCharacters.Length -eq 3969) 'Loli altar pattern must contain 3969 positions'
Assert-True (($altarRows | Where-Object { $_.Length -ne 63 }).Count -eq 0) `
    'Every Loli altar pattern row must be 63 positions wide'
$altarCount = ($altarCharacters.ToCharArray() | Where-Object { $_ -eq '#' }).Count
$airCount = ($altarCharacters.ToCharArray() | Where-Object { $_ -eq '.' }).Count
Assert-True ($altarCount -eq 1169) "Loli altar block count is $altarCount, expected 1169"
Assert-True ($airCount -eq 2800) "Loli altar air count is $airCount, expected 2800"

$safeServiceSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/safe/SafeTntEffectService.java'
) -Raw
Assert-True ($safeServiceSource -match 'new Settings\(false, false, false\)') `
    'Safe TNT effects must remain disabled by default'
Assert-True ($safeServiceSource -match 'player\.connection\.disconnect') `
    'Safe EXIT effect must disconnect only the affected player'

$metadataSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/resources/fabric.mod.json'
) -Raw | ConvertFrom-Json
Assert-True ($metadataSource.entrypoints.client -contains 'com.liymod.client.LiyModClient') `
    'Client entrypoint for safe effects and entity rendering is missing'

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
    foreach ($key in @(
        'container.liymod.password_workbench',
        'container.liymod.loli_storage',
        'container.liymod.loli_blacklist',
        'key.liymod.loli_container',
        'key.liymod.loli_container_blacklist'
    )) {
        Assert-True $lang.ContainsKey($key) "Missing $language utility translation: $key"
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
    $functionalEntries = @(
        'com/liymod/entity/LoliEntity.class',
        'com/liymod/entity/LoliPrimedTntEntity.class',
        'com/liymod/block/LoliAltarBlock.class',
        'com/liymod/block/BuffAttackTntBlock.class',
        'com/liymod/client/LiyModClient.class',
        'com/liymod/client/render/LoliEntityRenderer.class',
        'com/liymod/client/safe/BlueScreenEffectScreen.class',
        'com/liymod/client/safe/FailRespondEffectScreen.class',
        'assets/liymod/textures/entity/loli.png',
        'data/liymod/loli_altar_pattern/default.json'
        'com/liymod/block/PasswordWorkbenchBlock.class',
        'com/liymod/menu/PasswordWorkbenchMenu.class',
        'com/liymod/menu/StorageMenu.class',
        'com/liymod/menu/BlacklistMenu.class',
        'com/liymod/storage/LoliStorageData.class',
        'com/liymod/storage/LoliStorageEvents.class',
        'com/liymod/client/input/LoliKeyMappings.class',
        'com/liymod/client/storage/LoliStorageClient.class',
        'com/liymod/client/screen/PasswordWorkbenchScreen.class',
        'com/liymod/client/screen/LoliStorageScreen.class',
        'com/liymod/client/screen/LoliBlacklistScreen.class',
        'assets/liymod/textures/gui/container/password_crafting_table.png',
        'assets/liymod/textures/gui/container/loli_pickaxe_container.png',
        'assets/liymod/textures/gui/container/loli_pickaxe_container_blacklist.png'
    )
    foreach ($entryPath in $functionalEntries) {
        Assert-True ($null -ne $archive.GetEntry($entryPath)) `
            "JAR functional block/entity entry missing: $entryPath"
    }
} finally {
    $archive.Dispose()
}

Write-Host "VERIFY_FULL_PORT_PROGRESS_OK items=$($itemIds.Count) blocks=$($blockIds.Count) recipes=$($recipeIds.Count)"
