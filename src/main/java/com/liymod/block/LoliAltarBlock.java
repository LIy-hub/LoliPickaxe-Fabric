package com.liymod.block;

import com.liymod.entity.LoliEntity;
import com.liymod.entity.ModEntities;
import com.liymod.item.ModItems;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class LoliAltarBlock extends Block {
    public static final int STRUCTURE_SIZE = 63;
    public static final int ALTAR_BLOCK_COUNT = 1169;
    public static final int AIR_BLOCK_COUNT = 2800;
    public static final int STRUCTURE_BLOCK_COUNT = STRUCTURE_SIZE * STRUCTURE_SIZE;

    private static final int RADIUS = STRUCTURE_SIZE / 2;
    private static final long CONFIRMATION_WINDOW_TICKS = 200L;
    private static final String STRUCTURE_BITS_BASE64 =
            "d3d3d3d3d/cRERERERERcQcHBwcHBweXAQEBAQEBAVN3AHcAdwB3nREAEQARADFHBwAHAAcAF4EBAAEAAQADUXd3AAB3d8WZEREAABExc0UHBwAABxeVgQEBAAABA1NQdwAAAHcFHZgRAAAAMQMHRAcAAAAXAQGAAQAAAAMAAVF3d3d3RcCBmRERETEzcEFFBwcHFxWUgYEBAQEDA1NRUHcAdwVFnRmYEQAxAzNHBUQHABcBFYEBgAEAAwADUQBRd3dFAMUZgJkRMTMAcwVARQcXFQCVAYCBAQMDAFMAUFB3BQUAHQAYmDEDAwAHAAREFwEBAAEAAIADAAAAAQAB0UVAAMABgIEZMzAAcAFAQd0VFACUAYCBAQMDAFMBUNHBRQVAnQGYGREzAzBHAUTd3RUBFIEBgAEAAwADUQHRAcBFQMWZgRkBEDMwc0VB3QHcFRSVgYEBAQEDA1NQ0cHBwUUFHZgZERERMwMHRN3d3d0VAQGAAQAAAAMAAdEBAADARcCBGQEAABAzcEHdAQAA3BWUgQEBAAABA1PRwQEAwMFFnRkRAQAQETNH3d0BANzdFYEBAAEAAQAD0QHAAcABwMUZARABEAEQc90B3AHcAdyVAQEBAQEBAdPBwcHBwcHBHRERERERERHf3d3d3d3d3QE=";
    private static final boolean[][] STRUCTURE = decodeStructure();
    private static final Map<UUID, PendingCreativeBuild> PENDING_CREATIVE_BUILDS = new HashMap<>();

    public LoliAltarBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)
                || !player.getAbilities().instabuild
                || !player.isShiftKeyDown()) {
            return InteractionResult.SUCCESS;
        }
        return prepareOrBuildCreativeAltar(serverLevel, pos, serverPlayer);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!stack.is(ModItems.LOLI_PICKAXE) && !stack.is(ModItems.SMALL_LOLI_PICKAXE)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (level instanceof ServerLevel serverLevel) {
            activateCompletedAltar(serverLevel, pos);
        }
        return InteractionResult.SUCCESS;
    }

    public boolean matchesExactStructure(Level level, BlockPos center) {
        int altarCount = 0;
        int airCount = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                cursor.set(center.getX() + dx, center.getY(), center.getZ() + dz);
                if (!level.hasChunkAt(cursor)) {
                    return false;
                }
                BlockState found = level.getBlockState(cursor);
                if (STRUCTURE[dx + RADIUS][dz + RADIUS]) {
                    if (!found.is(this)) {
                        return false;
                    }
                    altarCount++;
                } else {
                    if (!found.isAir()) {
                        return false;
                    }
                    airCount++;
                }
            }
        }
        return altarCount == ALTAR_BLOCK_COUNT && airCount == AIR_BLOCK_COUNT;
    }

    private InteractionResult prepareOrBuildCreativeAltar(
            ServerLevel level,
            BlockPos center,
            ServerPlayer player
    ) {
        if (!canSafelyBuild(level, center)) {
            PENDING_CREATIVE_BUILDS.remove(player.getUUID());
            player.sendOverlayMessage(Component.literal("Loli altar preflight failed: the 63x63 area is obstructed."));
            return InteractionResult.SUCCESS;
        }

        long now = level.getGameTime();
        PendingCreativeBuild pending = PENDING_CREATIVE_BUILDS.get(player.getUUID());
        if (pending == null || !pending.matches(level, center, now)) {
            PENDING_CREATIVE_BUILDS.put(
                    player.getUUID(),
                    new PendingCreativeBuild(level.dimension(), center.immutable(), now + CONFIRMATION_WINDOW_TICKS)
            );
            player.sendOverlayMessage(Component.literal(
                    "Preflight passed. Sneak-use the empty hand again within 10 seconds to build the 63x63 altar."
            ));
            return InteractionResult.SUCCESS;
        }

        PENDING_CREATIVE_BUILDS.remove(player.getUUID());
        buildCreativeAltar(level, center);
        return InteractionResult.SUCCESS;
    }

    private boolean canSafelyBuild(Level level, BlockPos center) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                cursor.set(center.getX() + dx, center.getY(), center.getZ() + dz);
                if (!level.hasChunkAt(cursor)) {
                    return false;
                }
                BlockState found = level.getBlockState(cursor);
                if (found.is(this)) {
                    continue;
                }
                if (!found.isAir() && !found.canBeReplaced()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void buildCreativeAltar(ServerLevel level, BlockPos center) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                cursor.set(center.getX() + dx, center.getY(), center.getZ() + dz);
                BlockState replacement = STRUCTURE[dx + RADIUS][dz + RADIUS]
                        ? defaultBlockState()
                        : Blocks.AIR.defaultBlockState();
                level.setBlock(cursor, replacement, Block.UPDATE_ALL);
            }
        }
    }

    private boolean activateCompletedAltar(ServerLevel level, BlockPos center) {
        if (!matchesExactStructure(level, center)) {
            return false;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                cursor.set(center.getX() + dx, center.getY(), center.getZ() + dz);
                level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        LoliEntity loli = new LoliEntity(ModEntities.LOLI, level);
        loli.setPos(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D);
        level.addFreshEntity(loli);
        return true;
    }

    private static boolean[][] decodeStructure() {
        byte[] packed = Base64.getDecoder().decode(STRUCTURE_BITS_BASE64);
        boolean[][] decoded = new boolean[STRUCTURE_SIZE][STRUCTURE_SIZE];
        int altarCount = 0;
        for (int index = 0; index < STRUCTURE_BLOCK_COUNT; index++) {
            boolean altar = (packed[index / 8] & (1 << (index % 8))) != 0;
            decoded[index / STRUCTURE_SIZE][index % STRUCTURE_SIZE] = altar;
            if (altar) {
                altarCount++;
            }
        }
        if (altarCount != ALTAR_BLOCK_COUNT
                || STRUCTURE_BLOCK_COUNT - altarCount != AIR_BLOCK_COUNT) {
            throw new IllegalStateException("Invalid 63x63 Loli altar pattern");
        }
        return decoded;
    }

    private record PendingCreativeBuild(
            net.minecraft.resources.ResourceKey<Level> dimension,
            BlockPos center,
            long expiresAt
    ) {
        private boolean matches(ServerLevel level, BlockPos candidate, long now) {
            return dimension.equals(level.dimension())
                    && center.equals(candidate)
                    && now <= expiresAt;
        }
    }
}
