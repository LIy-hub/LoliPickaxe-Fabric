param(
    [string]$ExpectedMinecraftVersion = '1.20.1',
    [int]$ExpectedJavaRelease = 17,
    [string]$JarPath
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
function Assert-True([bool]$Condition, [string]$Message) { if (-not $Condition) { throw $Message } }
$properties = Get-Content -Raw -LiteralPath (Join-Path $root 'gradle.properties')
Assert-True ($properties -match "(?m)^minecraft_version=$([regex]::Escape($ExpectedMinecraftVersion))$") 'Minecraft version mismatch'
Assert-True ((Get-Content -Raw -LiteralPath (Join-Path $root 'build.gradle')) -match "options\.release\s*=\s*$ExpectedJavaRelease") 'Java release mismatch'
Assert-True ($properties -match '(?m)^forge_version=47\.4\.22$') 'Forge version mismatch'
if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $JarPath = Get-ChildItem (Join-Path $root 'build/libs') -Filter '*.jar' | Where-Object { $_.Name -notmatch 'sources|slim' } | Select-Object -First 1 -ExpandProperty FullName
}
Assert-True (Test-Path -LiteralPath $JarPath) 'Release JAR is missing'
$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $JarPath).Hash
Write-Output "FORGE_RELEASE_OK minecraft=$ExpectedMinecraftVersion java=$ExpectedJavaRelease sha256=$hash jar=$JarPath"
