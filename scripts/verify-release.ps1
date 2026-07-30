param(
    [Parameter(Mandatory = $true)]
    [string]$ExpectedMinecraftVersion,

    [Parameter(Mandatory = $true)]
    [ValidateSet(17, 21)]
    [int]$ExpectedJavaRelease,

    [string]$ExpectedFabricApiVersion,

    [string]$JarPath
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Read-Properties {
    param([string]$Path)

    $properties = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith('#')) {
            continue
        }
        $separator = $trimmed.IndexOf('=')
        if ($separator -lt 1) {
            continue
        }
        $properties[$trimmed.Substring(0, $separator).Trim()] =
            $trimmed.Substring($separator + 1).Trim()
    }
    return $properties
}

function Get-ZipEntryText {
    param(
        [System.IO.Compression.ZipArchive]$Archive,
        [string]$Path
    )

    $entry = $Archive.GetEntry($Path)
    Assert-True ($null -ne $entry) "Missing JAR entry: $Path"
    $reader = [System.IO.StreamReader]::new($entry.Open())
    try {
        return $reader.ReadToEnd()
    } finally {
        $reader.Dispose()
    }
}

$properties = Read-Properties (Join-Path $projectRoot 'gradle.properties')
Assert-True ($properties.minecraft_version -eq $ExpectedMinecraftVersion) `
    "minecraft_version is $($properties.minecraft_version), expected $ExpectedMinecraftVersion"
Assert-True ($properties.loader_version -eq '0.19.3') `
    "loader_version must remain pinned to 0.19.3"
Assert-True ($properties.loom_version -eq '1.17.17') `
    "loom_version must remain pinned to stable 1.17.17"
Assert-True ($properties.yarn_mappings -notmatch 'SNAPSHOT') `
    "Yarn mappings must be an exact release"
Assert-True ($properties.yarn_mappings -like "$ExpectedMinecraftVersion+build.*") `
    "Yarn mappings do not match Minecraft $ExpectedMinecraftVersion"
Assert-True ($properties.fabric_version -notmatch 'SNAPSHOT') `
    "Fabric API must be an exact release"
if (-not [string]::IsNullOrWhiteSpace($ExpectedFabricApiVersion)) {
    Assert-True ($properties.fabric_version -eq $ExpectedFabricApiVersion) `
        "fabric_version is $($properties.fabric_version), expected $ExpectedFabricApiVersion"
}

$buildScript = Get-Content -LiteralPath (Join-Path $projectRoot 'build.gradle') -Raw
Assert-True ($buildScript -match "options\.release\s*=\s*$ExpectedJavaRelease") `
    "Java compile release is not $ExpectedJavaRelease"
Assert-True ($buildScript -match "sourceCompatibility\s*=\s*JavaVersion\.VERSION_$ExpectedJavaRelease") `
    "Java source compatibility is not $ExpectedJavaRelease"
Assert-True ($buildScript -match "targetCompatibility\s*=\s*JavaVersion\.VERSION_$ExpectedJavaRelease") `
    "Java target compatibility is not $ExpectedJavaRelease"
Assert-True ($buildScript -match 'https://maven\.fabricmc\.net/') `
    "Official Fabric Maven repository is missing"
Assert-True ($buildScript -notmatch 'hanbings|aliyun|SNAPSHOT') `
    "Unapproved proxy or snapshot dependency remains in build.gradle"

if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $JarPath = Join-Path $projectRoot (
        "build/libs/LoliPickaxe-$ExpectedMinecraftVersion-$($properties.mod_version).jar"
    )
}
Assert-True (Test-Path -LiteralPath $JarPath -PathType Leaf) "JAR not found: $JarPath"

$expectedAssets = [ordered]@{
    'src/main/resources/assets/liymod/icon.png' =
        '7873CEEF555488D5EA08B54ECD29DC05D96A55A918F915D1011BFA14D0AA1E82'
    'src/main/resources/assets/liymod/textures/item/loli_pickaxe.png' =
        '7873CEEF555488D5EA08B54ECD29DC05D96A55A918F915D1011BFA14D0AA1E82'
    'src/main/resources/assets/liymod/textures/item/loli.png' =
        'B3E5D9B7606DDFD7E3AC1D137AFC8D1BBF9EF28101479553598DC71D698A41C0'
    'src/main/resources/assets/liymod/sounds/loli_immunity_first.ogg' =
        '98A24E9A3BB6DD17FF2C4EDE7D7671FCFDD1F03D3F71DDF54846A43E52B95848'
    'src/main/resources/assets/liymod/sounds/loli_immunity_second.ogg' =
        'B207F7C47B14A4CE3988A3F291F3F00B045C44EDAE13476F51B8C977363F0382'
}

foreach ($relativePath in $expectedAssets.Keys) {
    $assetPath = Join-Path $projectRoot $relativePath
    Assert-True (Test-Path -LiteralPath $assetPath -PathType Leaf) "Asset missing: $relativePath"
    $actualHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $assetPath).Hash
    Assert-True ($actualHash -eq $expectedAssets[$relativePath]) `
        "Asset hash changed: $relativePath"
}

$dataRoot = Join-Path $projectRoot 'src/main/resources/data'
$recipeFiles = @(
    Get-ChildItem -LiteralPath $dataRoot -Recurse -File |
        Where-Object { $_.FullName -match '[\\/](recipes?)[\\/]' }
)
Assert-True ($recipeFiles.Count -eq 0) "Compatibility branch must not add recipes"

$attackBlockSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/event/AttackBlockEvents.java'
) -Raw
$expectedDropBlocks = @(
    'SPAWNER', 'STRUCTURE_BLOCK', 'JIGSAW', 'END_PORTAL_FRAME',
    'COMMAND_BLOCK', 'CHAIN_COMMAND_BLOCK', 'REPEATING_COMMAND_BLOCK',
    'BEDROCK', 'BARRIER', 'COAL_ORE', 'DEEPSLATE_COAL_ORE', 'IRON_ORE',
    'DEEPSLATE_IRON_ORE', 'GOLD_ORE', 'DEEPSLATE_GOLD_ORE',
    'REDSTONE_ORE', 'DEEPSLATE_REDSTONE_ORE', 'DIAMOND_ORE',
    'DEEPSLATE_DIAMOND_ORE', 'EMERALD_ORE', 'DEEPSLATE_EMERALD_ORE',
    'LAPIS_ORE', 'DEEPSLATE_LAPIS_ORE', 'COPPER_ORE',
    'DEEPSLATE_COPPER_ORE', 'NETHER_QUARTZ_ORE', 'ANCIENT_DEBRIS'
)
$dropCount = ([regex]::Matches($attackBlockSource, 'Map\.entry\(')).Count
Assert-True ($dropCount -eq 27) "Special drop count is $dropCount, expected 27"
foreach ($block in $expectedDropBlocks) {
    Assert-True ($attackBlockSource -match "Map\.entry\($block,") `
        "Special drop source missing: $block"
}

$pickaxeSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/item/LoliPickaxeItem.java'
) -Raw
Assert-True ($pickaxeSource -match 'ABILITY_RANGE\s*=\s*32\.0') `
    "Ability range must remain 32 blocks"
Assert-True ($pickaxeSource -match 'DataComponentTypes\.UNBREAKABLE') `
    "Version-appropriate unbreakable component is missing"
Assert-True ($pickaxeSource -match 'stack\.setDamage\(0\)') `
    "Pickaxe damage reset is missing"

$resolverSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/combat/LoliAttackResolver.java'
) -Raw
Assert-True ($resolverSource -match 'MAX_RANGE\s*=\s*1024\.0D') `
    "Swing resolver range must remain 1024 blocks"
Assert-True ($resolverSource -match 'Math\.toRadians\(6\.0D\)') `
    "Swing resolver fallback must remain six degrees"

$materialSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/tool/ModToolMaterials.java'
) -Raw
foreach ($requiredValue in @(
    'Integer\.MAX_VALUE', 'Float\.MAX_VALUE',
    'Float\.POSITIVE_INFINITY', '\b30\b'
)) {
    Assert-True ($materialSource -match $requiredValue) `
        "Frozen tool material value missing: $requiredValue"
}

$authoritySource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/combat/ExecutionAuthority.java'
) -Raw
Assert-True ($authoritySource -match 'STANDARD\(0,\s*false\)') `
    "STANDARD authority changed"
Assert-True ($authoritySource -match 'ABSOLUTE_EXECUTION\(Integer\.MAX_VALUE,\s*true\)') `
    "ABSOLUTE_EXECUTION authority changed"

$ticketSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/combat/LoliExecutionTicket.java'
) -Raw
Assert-True ($ticketSource -match 'PREPARE,\s*COMMITTING,\s*DEAD_LOCK') `
    "Execution ticket state order changed"

$protectionSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/protection/LoliProtection.java'
) -Raw
Assert-True ($protectionSource -match 'ModItems\.LOLI_PICKAXE') `
    "Holder protection no longer checks the Loli Pickaxe"

$abilitySource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/loliability/LoliAbilityEvents.java'
) -Raw
Assert-True ($abilitySource -notmatch '(?i)add(?:Persistent|Temporary)?Modifier\s*\(') `
    "Movement-speed modifier must remain retired"
Assert-True ($abilitySource -match 'removeModifier\(LEGACY_SPEED_BOOST_ID\)') `
    "Legacy movement-speed cleanup is missing"

$immunitySource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/combat/LoliImmunityFeedback.java'
) -Raw
foreach ($requiredToken in @(
    'server\.getTicks\(\)', 'world\.getRegistryKey\(\)',
    'attacker\.getUuid\(\)', 'protectedTarget\.getUuid\(\)',
    'eventsThisTick', 'playFirstNext',
    'LOLI_IMMUNITY_FIRST', 'LOLI_IMMUNITY_SECOND'
)) {
    Assert-True ($immunitySource -match $requiredToken) `
        "Same-item immunity/audio contract missing: $requiredToken"
}

$mixinSourceRoot = Join-Path $projectRoot 'src/main/java/com/liymod/mixin'
$annotationCounts = [ordered]@{
    '@Inject' = 39
    '@ModifyVariable' = 3
    '@ModifyExpressionValue' = 1
    '@Accessor' = 4
}
$javaSources = Get-ChildItem -LiteralPath $mixinSourceRoot -Recurse -Filter '*.java'
$combinedMixinSource = ($javaSources | Get-Content) -join "`n"
foreach ($annotation in $annotationCounts.Keys) {
    $actualCount = ([regex]::Matches(
        $combinedMixinSource,
        [regex]::Escape($annotation)
    )).Count
    Assert-True ($actualCount -eq $annotationCounts[$annotation]) `
        "Mixin contract changed: $annotation count is $actualCount, expected $($annotationCounts[$annotation])"
}

Add-Type -AssemblyName System.IO.Compression
$archive = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $JarPath))
try {
    $metadata = (Get-ZipEntryText $archive 'fabric.mod.json') | ConvertFrom-Json
    Assert-True ($metadata.id -eq 'liymod') "Unexpected mod id: $($metadata.id)"
    Assert-True ($metadata.version -eq $properties.mod_version) `
        "JAR version $($metadata.version) does not match gradle.properties"
    Assert-True ($metadata.depends.minecraft -eq "~$ExpectedMinecraftVersion") `
        "JAR Minecraft dependency is $($metadata.depends.minecraft)"
    Assert-True ($metadata.depends.java -eq ">=$ExpectedJavaRelease") `
        "JAR Java dependency is $($metadata.depends.java)"
    Assert-True ($metadata.depends.fabricloader -eq '>=0.19.3') `
        "JAR Fabric Loader dependency is not >=0.19.3"
    $expectedFabricMinimum = '>=' + ($properties.fabric_version -split '\+')[0]
    Assert-True ($metadata.depends.'fabric-api' -eq $expectedFabricMinimum) `
        "JAR Fabric API dependency is not $expectedFabricMinimum"

    $requiredEntries = @(
        "LICENSE_LoliPickaxe-$ExpectedMinecraftVersion",
        "CREDITS.md_LoliPickaxe-$ExpectedMinecraftVersion",
        'assets/liymod/icon.png',
        'assets/liymod/textures/item/loli.png',
        'assets/liymod/textures/item/loli_pickaxe.png',
        'assets/liymod/sounds/loli_immunity_first.ogg',
        'assets/liymod/sounds/loli_immunity_second.ogg',
        'assets/liymod/sounds.json',
        'data/liymod/damage_type/loli_damage.json',
        'data/liymod/tags/block/incorrect_for_loli_tool.json',
        'data/liymod/tags/item/loli_repair_materials.json',
        'liymod.mixins.json'
    )
    foreach ($entryPath in $requiredEntries) {
        Assert-True ($null -ne $archive.GetEntry($entryPath)) "Missing JAR entry: $entryPath"
    }

    $mixinConfig = (Get-ZipEntryText $archive 'liymod.mixins.json') | ConvertFrom-Json
    Assert-True ($mixinConfig.required -eq $true) "Mixin config must remain required"
    Assert-True ($mixinConfig.compatibilityLevel -eq "JAVA_$ExpectedJavaRelease") `
        "Mixin compatibility level must match Java $ExpectedJavaRelease"
    Assert-True ($mixinConfig.injectors.defaultRequire -eq 1) `
        "Mixin defaultRequire must remain 1"
    foreach ($mixinName in $mixinConfig.mixins) {
        $classPath = 'com/liymod/mixin/' + $mixinName.Replace('.', '/') + '.class'
        Assert-True ($null -ne $archive.GetEntry($classPath)) `
            "Configured mixin class missing from JAR: $classPath"
    }
} finally {
    $archive.Dispose()
}

$jarHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $JarPath).Hash
Write-Host "VERIFY_RELEASE_OK minecraft=$ExpectedMinecraftVersion java=$ExpectedJavaRelease fabricApi=$($properties.fabric_version)"
Write-Host "JAR=$JarPath"
Write-Host "SHA256=$jarHash"
