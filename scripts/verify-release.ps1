param(
    [Parameter(Mandatory = $true)]
    [string]$ExpectedMinecraftVersion,

    [Parameter(Mandatory = $true)]
    [ValidateSet(17, 21, 25)]
    [int]$ExpectedJavaRelease,

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
Assert-True (-not $properties.ContainsKey('yarn_mappings')) `
    "26.x builds must use Minecraft's unobfuscated official names"
Assert-True ($properties.fabric_api_version -notmatch 'SNAPSHOT') `
    "Fabric API must be an exact release"

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
Assert-True ($buildScript -notmatch '\bmappings\s+') `
    "26.x builds must not add a mappings dependency"

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

$mixinSourceRoot = Join-Path $projectRoot 'src/main/java/com/liymod/mixin'
$annotationCounts = [ordered]@{
    '@Inject' = 43
    '@ModifyVariable' = 3
    '@ModifyExpressionValue' = 1
    '@Accessor' = 3
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

    $requiredEntries = @(
        "LICENSE_LoliPickaxe-$ExpectedMinecraftVersion",
        "CREDITS.md_LoliPickaxe-$ExpectedMinecraftVersion",
        'assets/liymod/icon.png',
        'assets/liymod/textures/item/loli.png',
        'assets/liymod/textures/item/loli_pickaxe.png',
        'assets/liymod/sounds/loli_immunity_first.ogg',
        'assets/liymod/sounds/loli_immunity_second.ogg',
        'assets/liymod/sounds.json',
        'assets/liymod/items/loli.json',
        'assets/liymod/items/loli_pickaxe.json',
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
Write-Host "VERIFY_RELEASE_OK minecraft=$ExpectedMinecraftVersion java=$ExpectedJavaRelease"
Write-Host "JAR=$JarPath"
Write-Host "SHA256=$jarHash"
