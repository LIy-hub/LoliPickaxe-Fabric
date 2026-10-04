package com.liymod.item;

import com.liymod.LiyMod;
import com.liymod.combat.LoliExecutionManager;
import com.liymod.config.LoliConfigOption;
import com.liymod.config.LoliItemSettings;
import com.liymod.storage.LoliStorageData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Immediate single/range mining for the final pickaxe with bounded modern drops. */
public final class LoliFinalMiningEvents {
    private static final Set<UUID> ACTIVE_MINERS = new HashSet<>();
    private static final LoliMiningCooldown CLIENT_COOLDOWN = new LoliMiningCooldown();
    private static final LoliMiningCooldown SERVER_COOLDOWN = new LoliMiningCooldown();
    private static final Map<Block, Item> SPECIAL_DROPS = Map.ofEntries(
            Map.entry(Blocks.SPAWNER, Items.SPAWNER),
            Map.entry(Blocks.STRUCTURE_BLOCK, Items.STRUCTURE_BLOCK),
            Map.entry(Blocks.JIGSAW, Items.JIGSAW),
            Map.entry(Blocks.END_PORTAL_FRAME, Items.END_PORTAL_FRAME),
            Map.entry(Blocks.COMMAND_BLOCK, Items.COMMAND_BLOCK),
            Map.entry(Blocks.CHAIN_COMMAND_BLOCK, Items.CHAIN_COMMAND_BLOCK),
            Map.entry(Blocks.REPEATING_COMMAND_BLOCK, Items.REPEATING_COMMAND_BLOCK),
            Map.entry(Blocks.BEDROCK, Items.BEDROCK),
            Map.entry(Blocks.BARRIER, Items.BARRIER),
            Map.entry(Blocks.COAL_ORE, Items.COAL_BLOCK),
            Map.entry(Blocks.DEEPSLATE_COAL_ORE, Items.COAL_BLOCK),
            Map.entry(Blocks.IRON_ORE, Items.IRON_BLOCK),
            Map.entry(Blocks.DEEPSLATE_IRON_ORE, Items.IRON_BLOCK),
            Map.entry(Blocks.GOLD_ORE, Items.GOLD_BLOCK),
            Map.entry(Blocks.DEEPSLATE_GOLD_ORE, Items.GOLD_BLOCK),
            Map.entry(Blocks.REDSTONE_ORE, Items.REDSTONE_BLOCK),
            Map.entry(Blocks.DEEPSLATE_REDSTONE_ORE, Items.REDSTONE_BLOCK),
            Map.entry(Blocks.DIAMOND_ORE, Items.DIAMOND_BLOCK),
            Map.entry(Blocks.DEEPSLATE_DIAMOND_ORE, Items.DIAMOND_BLOCK),
            Map.entry(Blocks.EMERALD_ORE, Items.EMERALD_BLOCK),
            Map.entry(Blocks.DEEPSLATE_EMERALD_ORE, Items.EMERALD_BLOCK),
            Map.entry(Blocks.LAPIS_ORE, Items.LAPIS_BLOCK),
            Map.entry(Blocks.DEEPSLATE_LAPIS_ORE, Items.LAPIS_BLOCK),
            Map.entry(Blocks.COPPER_ORE, Items.COPPER_BLOCK),
            Map.entry(Blocks.DEEPSLATE_COPPER_ORE, Items.COPPER_BLOCK),
            Map.entry(Blocks.NETHER_QUARTZ_ORE, Items.QUARTZ_BLOCK),
            Map.entry(Blocks.ANCIENT_DEBRIS, Items.NETHERITE_BLOCK)
    );

    private LoliFinalMiningEvents() {
    }

    public static void registerEvents() {
        LiyMod.LOGGER.info("Registering final Loli Pickaxe mining events for {}", LiyMod.MOD_ID);
        AttackBlockCallback.EVENT.register(LoliFinalMiningEvents::attackBlock);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            SERVER_COOLDOWN.forget(handler.player.getUUID());
            ACTIVE_MINERS.remove(handler.player.getUUID());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            SERVER_COOLDOWN.clear();
            ACTIVE_MINERS.clear();
        });
    }

    public static void resetClientCooldown() {
        CLIENT_COOLDOWN.clear();
    }

    private static InteractionResult attackBlock(
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.level.Level level,
            InteractionHand hand,
            BlockPos origin,
            Direction direction
    ) {
        if (hand != InteractionHand.MAIN_HAND
                || !LoliItemSettings.isFinalPickaxe(player.getMainHandItem())) {
            return InteractionResult.PASS;
        }
        // Fabric sends START_DESTROY_BLOCK for a consumed client attack. Avoid
        // vanilla's hardness/progressive prediction and its competing ABORT/STOP.
        // Radius zero uses the same authoritative action for only the origin block.
        if (level.isClientSide()) {
            if (player.isSpectator()) return InteractionResult.PASS;
            var result = clientAttackResult(level.getBlockState(origin),
                    LoliFluidMining.isEnabled(player.getMainHandItem()));
            if (result == InteractionResult.SUCCESS && !CLIENT_COOLDOWN.tryAcquire(
                    player.getUUID(), LoliItemSettings.getMiningRadius(player.getMainHandItem()))) {
                return InteractionResult.FAIL;
            }
            return result;
        }
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        // Fabric invokes this callback before vanilla's spectator, reach and build checks.
        if (serverPlayer.isSpectator() || LoliExecutionManager.isDeadLocked(serverPlayer)
                || !serverLevel.isInWorldBounds(origin) || !serverLevel.hasChunkAt(origin)
                || !serverPlayer.canInteractWithBlock(origin, 1.0D)
                || !canBreakAt(serverLevel, serverPlayer, origin)
                || !LoliFluidMining.canMine(serverLevel.getBlockState(origin),
                        LoliFluidMining.isEnabled(serverPlayer.getMainHandItem()))) {
            return InteractionResult.FAIL;
        }
        ItemStack tool = serverPlayer.getMainHandItem();
        int radius = LoliItemSettings.getServerMiningRadius(tool);
        if (ACTIVE_MINERS.contains(serverPlayer.getUUID())
                || !SERVER_COOLDOWN.tryAcquire(serverPlayer.getUUID(), radius)) {
            return InteractionResult.CONSUME;
        }
        ACTIVE_MINERS.add(serverPlayer.getUUID());
        try (var experience = LoliMiningExperience.begin(serverPlayer)) {
            LoliPickaxeItem.refreshEnchantments(tool, serverLevel);
            boolean autoAccept = LoliItemSettings.getBoolean(tool, LoliConfigOption.AUTO_ACCEPT);
            boolean selectFluids = LoliFluidMining.isEnabled(tool);
            boolean autoFurnace = LoliItemSettings.getBoolean(tool, LoliConfigOption.AUTO_FURNACE);
            LoliStorageData storage = autoAccept ? LoliStorageData.open(tool) : null;
            boolean brokeAny = false;
            List<BlockPos> changedPositions = new ArrayList<>();
            try (var batch = storage == null ? null : storage.beginBatch()) {
                for (BlockPos target : LoliMiningRange.positions(origin, radius)) {
                    if (!serverLevel.hasChunkAt(target)) {
                        continue;
                    }
                    if (replaceOne(serverLevel, serverPlayer, tool, target, storage, selectFluids, autoFurnace)) {
                        brokeAny = true;
                        changedPositions.add(target.immutable());
                    }
                }
            }
            LoliRangeMiningSync.send(serverLevel, origin, changedPositions);
            if (brokeAny) {
                serverLevel.playSound(
                        null,
                        origin,
                        SoundEvents.AMETHYST_BLOCK_BREAK,
                        SoundSource.BLOCKS,
                        1.0F,
                        1.0F
                );
            }
        } finally {
            // Count the gap from completion too, so queued packets cannot repeat a slow action.
            SERVER_COOLDOWN.finished(serverPlayer.getUUID(), radius);
            ACTIVE_MINERS.remove(serverPlayer.getUUID());
        }
        return InteractionResult.CONSUME;
    }

    static InteractionResult clientAttackResult(BlockState state, boolean selectFluids) {
        return LoliFluidMining.canMine(state, selectFluids)
                ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    private static boolean replaceOne(
            ServerLevel level,
            ServerPlayer player,
            ItemStack tool,
            BlockPos pos,
            LoliStorageData storage,
            boolean selectFluids,
            boolean autoFurnace
    ) {
        BlockState state = level.getBlockState(pos);
        // Empty positions do not read settings, calculate loot, write blocks or enter the sync batch.
        if (state.isAir()) return false;
        if (!LoliFluidMining.canMine(state, selectFluids) || !canBreakAt(level, player, pos)) {
            return false;
        }

        if (LoliFluidMining.isFluidBlock(state)) {
            if (!LoliBlockReplacement.remove(level, pos, state, selectFluids)) {
                return false;
            }
            level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
            return true;
        }

        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(
                state,
                level,
                pos,
                blockEntity,
                player,
                tool
        ));
        Item special = SPECIAL_DROPS.get(state.getBlock());
        if (special != null) {
            drops.add(new ItemStack(special));
        }
        // The final pickaxe always claims a block item when vanilla's loot table refuses a drop.
        // This is an intrinsic ability, not a configurable rule.
        if (drops.isEmpty()) {
            Item fallback = state.getBlock().asItem();
            if (fallback != Items.AIR && fallback != ModItems.LOLI_PICKAXE) {
                drops.add(new ItemStack(fallback));
            }
        }

        if (!LoliBlockReplacement.remove(level, pos, state, selectFluids)) {
            return false;
        }
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
        state.spawnAfterBreak(level, pos, tool, true);

        if (autoFurnace) {
            drops = smeltDrops(level, player, drops);
        }
        deliverDrops(level, player, storage, pos, drops);
        return true;
    }

    private static boolean canBreakAt(ServerLevel level, ServerPlayer player, BlockPos pos) {
        return level.isInWorldBounds(pos) && level.getWorldBorder().isWithinBounds(pos)
                && level.mayInteract(player, pos) && player.mayInteract(level, pos)
                && !level.getServer().isUnderSpawnProtection(level, pos, player)
                && !player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer());
    }

    private static List<ItemStack> smeltDrops(
            ServerLevel level,
            ServerPlayer player,
            List<ItemStack> drops
    ) {
        List<ItemStack> transformed = new ArrayList<>();
        for (ItemStack drop : drops) {
            SimpleContainer input = new SimpleContainer(drop);
            RecipeHolder<SmeltingRecipe> recipe = level.getServer()
                    .getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, input, level)
                    .orElse(null);
            if (recipe == null) {
                transformed.add(drop);
                continue;
            }
            ItemStack result = recipe.value().assemble(input, level.registryAccess());
            if (result.isEmpty()) {
                transformed.add(drop);
                continue;
            }
            int fortunePower = player.getRandom().nextInt(LoliPickaxeItem.FORTUNE_LEVEL + 2);
            if (fortunePower == 0) {
                fortunePower = 1;
            }
            long resultCount = (long) result.getCount() * drop.getCount() * fortunePower;
            appendSplitStacks(transformed, result, resultCount);
            int experience = (int) (recipe.value().getExperience() * drop.getCount() * fortunePower);
            if (experience > 0) {
                ExperienceOrb.award(level, player.position(), experience);
            }
        }
        return transformed;
    }

    private static void deliverDrops(
            ServerLevel level,
            ServerPlayer player,
            LoliStorageData storage,
            BlockPos origin,
            List<ItemStack> drops
    ) {
        boolean autoAccept = storage != null;
        for (ItemStack drop : drops) {
            boolean blacklisted = autoAccept && storage.isBlacklisted(drop);
            ItemStack remaining = autoAccept ? storage.insert(drop) : drop.copy();
            if (autoAccept && !blacklisted && !remaining.isEmpty()) {
                // Inventory.add mutates this exact stack, leaving only the part that did not fit.
                player.getInventory().add(remaining);
            }
            if (!remaining.isEmpty()) {
                ItemEntity entity = new ItemEntity(
                        level,
                        origin.getX() + 0.5D,
                        origin.getY() + 0.5D,
                        origin.getZ() + 0.5D,
                        remaining
                );
                entity.setTarget(player.getUUID());
                level.addFreshEntity(entity);
            }
        }
    }

    private static void appendSplitStacks(List<ItemStack> output, ItemStack template, long count) {
        while (count > 0L) {
            int splitCount = (int) Math.min(template.getMaxStackSize(), count);
            output.add(template.copyWithCount(splitCount));
            count -= splitCount;
        }
    }
}
