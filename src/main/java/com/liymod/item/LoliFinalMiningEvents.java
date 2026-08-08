package com.liymod.item;

import com.liymod.LiyMod;
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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Immediate single/range mining for the final pickaxe with bounded modern drops. */
public final class LoliFinalMiningEvents {
    private static final Set<UUID> ACTIVE_MINERS = new HashSet<>();
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
            Map.entry(Blocks.COPPER_ORE, Items.COPPER_BLOCK.weathering().unaffected()),
            Map.entry(Blocks.DEEPSLATE_COPPER_ORE, Items.COPPER_BLOCK.weathering().unaffected()),
            Map.entry(Blocks.NETHER_QUARTZ_ORE, Items.QUARTZ_BLOCK),
            Map.entry(Blocks.ANCIENT_DEBRIS, Items.NETHERITE_BLOCK)
    );

    private LoliFinalMiningEvents() {
    }

    public static void registerEvents() {
        LiyMod.LOGGER.info("Registering final Loli Pickaxe mining events for {}", LiyMod.MOD_ID);
        AttackBlockCallback.EVENT.register(LoliFinalMiningEvents::attackBlock);
    }

    private static InteractionResult attackBlock(
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.level.Level level,
            InteractionHand hand,
            BlockPos origin,
            Direction direction
    ) {
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)
                || hand != InteractionHand.MAIN_HAND
                || !LoliItemSettings.isFinalPickaxe(serverPlayer.getMainHandItem())) {
            return InteractionResult.PASS;
        }
        if (!ACTIVE_MINERS.add(serverPlayer.getUUID())) {
            return InteractionResult.SUCCESS_SERVER;
        }

        ItemStack tool = serverPlayer.getMainHandItem();
        try {
            LoliPickaxeItem.refreshEnchantments(tool, serverLevel);
            int radius = LoliItemSettings.getMiningRadius(tool);
            boolean brokeAny = false;
            List<BlockPos> changedPositions = new ArrayList<>();
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos target = origin.offset(x, y, z);
                        if (!serverLevel.hasChunkAt(target)) {
                            continue;
                        }
                        if (breakOne(serverLevel, serverPlayer, tool, target)) {
                            brokeAny = true;
                            changedPositions.add(target.immutable());
                        }
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
            ACTIVE_MINERS.remove(serverPlayer.getUUID());
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private static boolean breakOne(
            ServerLevel level,
            ServerPlayer player,
            ItemStack tool,
            BlockPos pos
    ) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()
                || !player.mayInteract(level, pos)
                || player.blockActionRestricted(level, pos, player.gameMode())
                || (LoliItemSettings.getBoolean(tool, LoliConfigOption.STOP_ON_LIQUID)
                && !state.getFluidState().isEmpty())) {
            return false;
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
        if (drops.isEmpty() && LoliItemSettings.getBoolean(tool, LoliConfigOption.MANDATORY_DROP)) {
            Item fallback = state.getBlock().asItem();
            if (fallback != Items.AIR && fallback != ModItems.LOLI_PICKAXE) {
                drops.add(new ItemStack(fallback));
            }
        }

        if (!level.destroyBlock(pos, false, player)) {
            return false;
        }
        state.spawnAfterBreak(level, pos, tool, true);

        if (LoliItemSettings.getBoolean(tool, LoliConfigOption.AUTO_FURNACE)) {
            drops = smeltDrops(level, player, drops);
        }
        deliverDrops(level, player, tool, pos, drops);
        return true;
    }

    private static List<ItemStack> smeltDrops(
            ServerLevel level,
            ServerPlayer player,
            List<ItemStack> drops
    ) {
        List<ItemStack> transformed = new ArrayList<>();
        for (ItemStack drop : drops) {
            SingleRecipeInput input = new SingleRecipeInput(drop);
            RecipeHolder<SmeltingRecipe> recipe = level.getServer()
                    .getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, input, level)
                    .orElse(null);
            if (recipe == null) {
                transformed.add(drop);
                continue;
            }
            ItemStack result = recipe.value().assemble(input);
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
            int experience = (int) (recipe.value().experience() * drop.getCount() * fortunePower);
            if (experience > 0) {
                ExperienceOrb.award(level, player.position(), experience);
            }
        }
        return transformed;
    }

    private static void deliverDrops(
            ServerLevel level,
            ServerPlayer player,
            ItemStack tool,
            BlockPos origin,
            List<ItemStack> drops
    ) {
        LoliStorageData storage = LoliStorageData.open(tool);
        boolean autoAccept = LoliItemSettings.getBoolean(tool, LoliConfigOption.AUTO_ACCEPT);
        for (ItemStack drop : drops) {
            boolean blacklisted = storage.isBlacklisted(drop);
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
