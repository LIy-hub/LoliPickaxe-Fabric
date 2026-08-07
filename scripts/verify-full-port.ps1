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
} finally {
    $archive.Dispose()
}

Write-Host "VERIFY_FULL_PORT_STATIC_OK items=$($itemIds.Count) blocks=$($blockIds.Count)"
