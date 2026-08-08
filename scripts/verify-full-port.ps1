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
$modSoundsSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/sound/ModSounds.java'
) -Raw
$soundsJsonPath = Join-Path $projectRoot 'src/main/resources/assets/liymod/sounds.json'
$successSoundPath = Join-Path $projectRoot 'src/main/resources/assets/liymod/sounds/lolisuccess.ogg'
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
Assert-True ($smallPickaxeSource -match 'ModSounds\.LOLI_SUCCESS') `
    'Small Loli range switching does not play the restored success sound'
Assert-True ($modSoundsSource -match 'LOLI_SUCCESS_ID') `
    'Restored Small Loli success sound event is not registered'
Assert-True (Test-Path -LiteralPath $successSoundPath -PathType Leaf) `
    'Restored lolisuccess.ogg is missing'
Assert-True ((Get-FileHash -LiteralPath $successSoundPath -Algorithm SHA256).Hash -eq `
        'E8E5D906098AF7DFDAD501A517ECBF56D94369E4463A1355922D1455964D5F41') `
    'Restored lolisuccess.ogg does not match the upstream asset'
$soundsJson = Get-Content -LiteralPath $soundsJsonPath -Raw | ConvertFrom-Json
Assert-True ($null -ne $soundsJson.lolisuccess) `
    'sounds.json does not expose liymod:lolisuccess'

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
$playerMixinSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/mixin/PlayerMixin.java'
) -Raw
$smallPickaxeSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/item/SmallLoliPickaxeItem.java'
) -Raw
$enchantmentScreenSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/client/java/com/liymod/client/screen/FinalEnchantmentScreen.java'
) -Raw
$configOptionSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/config/LoliConfigOption.java'
) -Raw
$storageScreenSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/client/java/com/liymod/client/screen/LoliStorageScreen.java'
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
Assert-True ($storageEventsSource -match '(?s)allowsNearbyCollection.+AUTO_ACCEPT') `
    'Nearby final-pickaxe collection does not honor the per-item AUTO_ACCEPT setting'
Assert-True ($storageEventsSource -match 'entityTags\(\)\.contains\(MANUAL_EJECTION_TAG\)') `
    'Nearby storage collection does not exclude intentional player drops'
Assert-True ($storageEventsSource -notmatch 'getInventory\(\)\.getContainerSize') `
    'Nearby storage collection must not activate from a pickaxe stored only in inventory'
Assert-True ($playerMixinSource -match '(?s)method\s*=\s*"drop.+markManualEjection') `
    'Intentional player drops are not marked for storage ejection'
Assert-True ($smallMiningSource -match 'storage\.insert') `
    'Small Loli mining drops are not routed through storage'
Assert-True ($smallPickaxeSource -match '(?s)getCurrentMiningRadius\(stack\)\s*>\s*0\s*\?\s*Float\.MAX_VALUE') `
    'Ordinary Loli range mining is not immediate'
Assert-True ($storageMenuSource -match 'StoragePageSyncPayload') `
    'Ordered storage page synchronization is missing'
Assert-True ($enchantmentScreenSource -match 'MAX_LEVEL\s*=\s*32768') `
    'Final enchantment editor does not expose level 32768'
Assert-True ($configOptionSource -match '(?s)ENCHANTMENT_LEVEL_LIMIT.+32768.+32768\.0D') `
    'Server enchantment level limit is not 32768'
Assert-True ($storageScreenSource -match 'TEXT_COLOR\s*=\s*0xFFF5F5F5') `
    'Storage GUI labels are not rendered with high-contrast text'
Assert-True ($storageKeySource -match 'InputConstants\.KEY_B') `
    'Loli storage B/Shift+B key binding is missing'
Assert-True ($storageKeySource -match 'InputConstants\.KEY_U') `
    'Loli blacklist U key binding is missing'

$finalUtilitySources = @(
    'src/main/java/com/liymod/config/LoliConfigOption.java',
    'src/main/java/com/liymod/config/LoliServerConfig.java',
    'src/main/java/com/liymod/config/LoliItemSettings.java',
    'src/main/java/com/liymod/item/LoliFinalMiningEvents.java',
    'src/main/java/com/liymod/item/LoliFinalItemEvents.java',
    'src/main/java/com/liymod/item/LoliFinalEnchantments.java',
    'src/main/java/com/liymod/item/LoliFinalEffects.java',
    'src/main/java/com/liymod/item/LoliTeleportService.java',
    'src/main/java/com/liymod/combat/LoliLegacyExecutionPolicy.java',
    'src/main/java/com/liymod/command/LoliCommands.java',
    'src/main/java/com/liymod/menu/FinalConfigMenu.java',
    'src/main/java/com/liymod/menu/FinalEnchantmentMenu.java',
    'src/main/java/com/liymod/menu/FinalEffectMenu.java',
    'src/main/java/com/liymod/menu/FinalTeleportMenu.java',
    'src/main/java/com/liymod/network/LoliMenuOpenPayload.java',
    'src/main/java/com/liymod/network/LoliItemSettingPayload.java',
    'src/main/java/com/liymod/network/LoliEnchantmentUpdatePayload.java',
    'src/main/java/com/liymod/network/LoliEffectUpdatePayload.java',
    'src/main/java/com/liymod/network/LoliTeleportPayload.java',
    'src/client/java/com/liymod/client/screen/FinalConfigScreen.java',
    'src/client/java/com/liymod/client/screen/FinalEnchantmentScreen.java',
    'src/client/java/com/liymod/client/screen/FinalEffectScreen.java',
    'src/client/java/com/liymod/client/screen/FinalTeleportScreen.java'
)
foreach ($relativePath in $finalUtilitySources) {
    Assert-True (Test-Path -LiteralPath (Join-Path $projectRoot $relativePath)) `
        "Final Loli utility source missing: $relativePath"
}

$finalOptionSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/config/LoliConfigOption.java'
) -Raw
$finalMiningSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/item/LoliFinalMiningEvents.java'
) -Raw
$attackResolverSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/combat/LoliAttackResolver.java'
) -Raw
$pickaxeSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/item/LoliPickaxeItem.java'
) -Raw
$protectionSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/protection/LoliProtection.java'
) -Raw
$commandsSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/command/LoliCommands.java'
) -Raw
$legacyPolicySource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/combat/LoliLegacyExecutionPolicy.java'
) -Raw
$erasureServiceSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/combat/LoliErasureService.java'
) -Raw
Assert-True ($finalMiningSource -match 'SPECIAL_DROPS\s*=') `
    'Final Loli special-drop table is missing'
Assert-True ([regex]::Matches($finalMiningSource, 'Map\.entry\(').Count -eq 27) `
    'Final Loli special-drop table must contain exactly 27 entries'
Assert-True ($pickaxeSource -match 'ABILITY_RANGE\s*=\s*32\.0') `
    'Existing 32-block right-click execution range changed'
Assert-True ($attackResolverSource -match 'MAX_RANGE\s*=\s*1024\.0D') `
    'Existing 1024-block swing resolver range changed'
Assert-True ($attackResolverSource -match 'Math\.toRadians\(6\.0D\)') `
    'Existing six-degree swing resolver changed'
Assert-True ($attackResolverSource -match 'isMainHandProtected') `
    'Active execution resolver no longer requires the main-hand pickaxe'
Assert-True ($protectionSource -match 'INVENTORY_PROTECTION') `
    'Optional inventory protection integration is missing'
Assert-True ($commandsSource -match 'SafeTntEffectService\.apply') `
    '/loliattack must call only the safe effect service'
foreach ($defaultOff in @(
    'TARGET_FRIENDLY_ENTITIES',
    'TARGET_ALL_ENTITIES',
    'FORCE_REMOVE',
    'CLEAR_INVENTORY',
    'DROP_EQUIPMENT',
    'KICK_PLAYER',
    'REINCARNATION',
    'SOUL_REDEMPTION',
    'SAFE_ATTACK_COMMAND',
    'SAFE_BLUE_SCREEN',
    'SAFE_EXIT',
    'SAFE_FAIL_RESPOND'
)) {
    Assert-True ($finalOptionSource -match ($defaultOff + '\("[^"]+",\s*ValueType\.BOOLEAN,\s*false')) `
        "Dangerous legacy trigger is not disabled by default: $defaultOff"
}
Assert-True ($finalOptionSource -match 'entries\.size\(\)\s*>\s*24') `
    'Legacy player lists are not bounded to 24 entries'
Assert-True ($finalOptionSource -match '\[A-Za-z0-9_\]\{1,16\}') `
    'Legacy player-list name validation is missing'
Assert-True ($legacyPolicySource -match 'PreparedExecution implements AutoCloseable') `
    'Legacy execution inventory effects are not transactionally scoped'
Assert-True ($legacyPolicySource -match 'removeItemNoUpdate') `
    'Legacy execution transaction does not detach inventory before vanilla death drops'
Assert-True ($legacyPolicySource -match 'LoliExecutionManager\.isDeadLocked\(target\)') `
    'Legacy execution transaction can commit before DEAD_LOCK'
Assert-True ($legacyPolicySource -match '(?s)commitRecoverableDrops\(\).+committed\s*=\s*true') `
    'Legacy execution transaction marks itself committed before recoverable items are resolved'
Assert-True ($legacyPolicySource -match 'setUnlimitedLifetime\(\)') `
    'Safe inventory clearing does not create unlimited-lifetime recovery drops'
Assert-True ($legacyPolicySource -match 'SOUL_REDEMPTION_WHITELIST') `
    'Safe soul-redemption whitelist consumer is missing'
Assert-True ($erasureServiceSource -match '(?s)LoliLegacyExecutionPolicy\.prepare.+tryNormalDamage.+legacyExecution\.commit') `
    'Legacy execution transaction is not prepared before death and committed after execution'
Assert-True ($protectionSource -match '(?s)FORCE_REMOVE.+executeAbsolute.+else.+execute\(') `
    'force_remove does not observably upgrade retaliatory execution authority'
Assert-True ($commandsSource -match 'Commands\.literal\("playerlist"\)') `
    'Operator player-list recovery command is missing'
foreach ($keyCode in @('KEY_N', 'KEY_M', 'KEY_P', 'KEY_K')) {
    Assert-True ($storageKeySource -match ('InputConstants\.' + $keyCode)) `
        "Final Loli key binding missing: $keyCode"
}

$finalGuiTextures = @(
    'src/main/resources/assets/liymod/textures/gui/loli_pickaxe_config.png',
    'src/main/resources/assets/liymod/textures/gui/loli_pickaxe_space_folding.png'
)
foreach ($relativePath in $finalGuiTextures) {
    Assert-True (Test-Path -LiteralPath (Join-Path $projectRoot $relativePath)) `
        "Final Loli GUI texture missing: $relativePath"
}

$cardAndAuxiliarySources = @(
    'src/main/java/com/liymod/item/LoliCardCatalog.java',
    'src/main/java/com/liymod/item/LoliCardData.java',
    'src/main/java/com/liymod/item/LoliCardItem.java',
    'src/main/java/com/liymod/item/LoliOnlineCardItem.java',
    'src/main/java/com/liymod/item/LoliDispersalItem.java',
    'src/main/java/com/liymod/item/BugEntityClearItem.java',
    'src/main/java/com/liymod/item/LoliAuxiliaryDropEvents.java',
    'src/main/java/com/liymod/network/LoliCardOpenPayload.java',
    'src/main/java/com/liymod/network/LoliCardOnlineUpdatePayload.java',
    'src/client/java/com/liymod/client/card/CardClient.java',
    'src/client/java/com/liymod/client/card/CardViewerScreen.java',
    'src/client/java/com/liymod/client/card/CardOnlineConfigScreen.java',
    'src/client/java/com/liymod/client/card/OnlineCardImageLoader.java'
)
foreach ($relativePath in $cardAndAuxiliarySources) {
    Assert-True (Test-Path -LiteralPath (Join-Path $projectRoot $relativePath)) `
        "Card or auxiliary source missing: $relativePath"
}

$cardCatalogSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/item/LoliCardCatalog.java'
) -Raw
$auxiliaryDropSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/item/LoliAuxiliaryDropEvents.java'
) -Raw
$dispersalSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/item/LoliDispersalItem.java'
) -Raw
$bugClearSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/item/BugEntityClearItem.java'
) -Raw
$onlineLoaderSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/client/java/com/liymod/client/card/OnlineCardImageLoader.java'
) -Raw
Assert-True ([regex]::Matches($cardCatalogSource, 'new Art\(').Count -eq 10) `
    'Bundled Loli card catalog must contain exactly ten original images'
Assert-True ($cardCatalogSource -match 'xiaomo_daughter_8') `
    'Eight-image album group is incomplete'
Assert-True ($auxiliaryDropSource -match 'LOLI_CARD_DROP_CHANCE') `
    'Legacy card drop chance is not consumed'
Assert-True ($auxiliaryDropSource -match 'instanceof Creeper') `
    'Legacy record drop must remain creeper-only'
Assert-True ($dispersalSource -match '\.disperse\(\)') `
    'Loli dispersal item does not use the intended removal path'
Assert-True ($bugClearSource -match 'isClientSide\(\)') `
    'Bug-entity clear item must remain client-only'
Assert-True ($onlineLoaderSource -match 'supplyAsync') `
    'Online card loading is not asynchronous'
Assert-True ($onlineLoaderSource -match 'MAX_BYTES\s*=\s*8\s*\*\s*1024\s*\*\s*1024') `
    'Online card 8 MiB response limit is missing'
Assert-True ($onlineLoaderSource -match 'MAX_DIMENSION\s*=\s*4096') `
    'Online card decoded-dimension limit is missing'
Assert-True ($onlineLoaderSource -match 'setInstanceFollowRedirects\(false\)') `
    'Online card redirects must remain disabled'
$cardDataSource = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/java/com/liymod/item/LoliCardData.java'
) -Raw
Assert-True ($cardDataSource -match 'isPublicHostname') `
    'Online card localhost/private-host validation is missing'

$cardResourcePaths = @(
    'src/main/resources/assets/liymod/sounds/lolirecord.ogg',
    'src/main/resources/data/liymod/jukebox_song/loli_record.json',
    'src/main/resources/assets/liymod/textures/gui/loli_card_online_config.png',
    'src/main/resources/assets/liymod/textures/lolicards/card_xiaomo_1.png',
    'src/main/resources/assets/liymod/textures/lolicards/card_xiaomo_2.png',
    'src/main/resources/assets/liymod/textures/lolicards/card_xiaomo_3.png',
    'src/main/resources/assets/liymod/textures/lolicards/card_xiaomo_4.png',
    'src/main/resources/assets/liymod/textures/lolicards/card_xiaomo_5.png',
    'src/main/resources/assets/liymod/textures/lolicards/card_xiaomo_6.png',
    'src/main/resources/assets/liymod/textures/lolicards/card_xiaomo_7.png',
    'src/main/resources/assets/liymod/textures/lolicards/card_xiaomo_8.png',
    'src/main/resources/assets/liymod/textures/lolicards/altar_guide.png',
    'src/main/resources/assets/liymod/textures/lolicards/gk_head_portrait.png'
)
foreach ($relativePath in $cardResourcePaths) {
    Assert-True (Test-Path -LiteralPath (Join-Path $projectRoot $relativePath)) `
        "Card or record resource missing: $relativePath"
}
$originalCardDirectory = Join-Path $projectRoot 'src/main/resources/assets/liymod/lolicards'
$addressableCardDirectory = Join-Path $projectRoot 'src/main/resources/assets/liymod/textures/lolicards'
Assert-True ((Get-ChildItem -LiteralPath $originalCardDirectory -Filter '*.png').Count -eq 10) `
    'Original Loli card asset set must contain exactly ten PNG files'
Assert-True ((Get-ChildItem -LiteralPath $addressableCardDirectory -Filter '*.png').Count -eq 10) `
    'Modern addressable Loli card asset set must contain exactly ten PNG files'
$jukeboxSong = Get-Content -LiteralPath (
    Join-Path $projectRoot 'src/main/resources/data/liymod/jukebox_song/loli_record.json'
) -Raw | ConvertFrom-Json
Assert-True ($jukeboxSong.sound_event -eq 'liymod:lolirecord') `
    'Loli record jukebox song references the wrong sound event'
Assert-True ($jukeboxSong.length_in_seconds -gt 62.0 -and $jukeboxSong.length_in_seconds -lt 63.0) `
    'Loli record duration is outside the measured audio length'
Assert-True ($jukeboxSong.description.translate -eq 'item.record.lolirecord.desc') `
    'Legacy Loli record track description key is missing'

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
        'key.liymod.loli_container_blacklist',
        'key.liymod.loli_config',
        'key.liymod.loli_enchantment',
        'key.liymod.loli_potion',
        'key.liymod.loli_space_folding'
    )) {
        Assert-True $lang.ContainsKey($key) "Missing $language utility translation: $key"
    }
    $optionIds = [regex]::Matches(
        $finalOptionSource,
        '(?m)^\s*[A-Z0-9_]+\("([a-z0-9_]+)",\s*ValueType\.'
    ) | ForEach-Object { $_.Groups[1].Value }
    foreach ($optionId in $optionIds) {
        Assert-True $lang.ContainsKey("config.liymod.loli.$optionId") `
            "Missing $language Loli option translation: config.liymod.loli.$optionId"
    }
    foreach ($key in @(
        'item.liymod.loli_card_online.hint',
        'item.liymod.bug_entity_clear.warning',
        'gui.liymod.card.title',
        'gui.liymod.card.album_title',
        'gui.liymod.card.online_title',
        'gui.liymod.card.online_config_title',
        'gui.liymod.card.loading',
        'gui.liymod.card.load_failed',
        'gui.liymod.card.invalid_url'
    )) {
        Assert-True $lang.ContainsKey($key) "Missing $language card translation: $key"
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
        'data/liymod/loli_altar_pattern/default.json',
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
        'assets/liymod/textures/gui/container/loli_pickaxe_container_blacklist.png',
        'com/liymod/config/LoliServerConfig.class',
        'com/liymod/config/LoliItemSettings.class',
        'com/liymod/item/LoliFinalMiningEvents.class',
        'com/liymod/item/LoliFinalItemEvents.class',
        'com/liymod/item/LoliTeleportService.class',
        'com/liymod/combat/LoliLegacyExecutionPolicy.class',
        'com/liymod/command/LoliCommands.class',
        'com/liymod/client/screen/FinalConfigScreen.class',
        'com/liymod/client/screen/FinalEnchantmentScreen.class',
        'com/liymod/client/screen/FinalEffectScreen.class',
        'com/liymod/client/screen/FinalTeleportScreen.class',
        'assets/liymod/textures/gui/loli_pickaxe_config.png',
        'assets/liymod/textures/gui/loli_pickaxe_space_folding.png',
        'com/liymod/item/LoliCardItem.class',
        'com/liymod/item/LoliOnlineCardItem.class',
        'com/liymod/item/LoliDispersalItem.class',
        'com/liymod/item/BugEntityClearItem.class',
        'com/liymod/item/LoliAuxiliaryDropEvents.class',
        'com/liymod/client/card/CardClient.class',
        'com/liymod/client/card/CardViewerScreen.class',
        'com/liymod/client/card/CardOnlineConfigScreen.class',
        'com/liymod/client/card/OnlineCardImageLoader.class',
        'assets/liymod/sounds/lolirecord.ogg',
        'assets/liymod/sounds/lolisuccess.ogg',
        'assets/liymod/textures/gui/loli_card_online_config.png',
        'data/liymod/jukebox_song/loli_record.json'
    )
    foreach ($entryPath in $functionalEntries) {
        Assert-True ($null -ne $archive.GetEntry($entryPath)) `
            "JAR functional block/entity entry missing: $entryPath"
    }
} finally {
    $archive.Dispose()
}

Write-Host "VERIFY_FULL_PORT_OK items=$($itemIds.Count) blocks=$($blockIds.Count) recipes=$($recipeIds.Count)"
