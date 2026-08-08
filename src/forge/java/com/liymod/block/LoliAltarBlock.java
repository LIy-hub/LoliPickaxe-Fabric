package com.liymod.block;

import com.liymod.entity.LoliEntity;
import com.liymod.registry.ModContent;
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
    public static final int SIZE = 63;
    private static final int RADIUS = 31;
    private static final int ALTAR_COUNT = 1169;
    private static final String BITS = "d3d3d3d3d/cRERERERERcQcHBwcHBweXAQEBAQEBAVN3AHcAdwB3nREAEQARADFHBwAHAAcAF4EBAAEAAQADUXd3AAB3d8WZEREAABExc0UHBwAABxeVgQEBAAABA1NQdwAAAHcFHZgRAAAAMQMHRAcAAAAXAQGAAQAAAAMAAVF3d3d3RcCBmRERETEzcEFFBwcHFxWUgYEBAQEDA1NRUHcAdwVFnRmYEQAxAzNHBUQHABcBFYEBgAEAAwADUQBRd3dFAMUZgJkRMTMAcwVARQcXFQCVAYCBAQMDAFMAUFB3BQUAHQAYmDEDAwAHAAREFwEBAAEAAIADAAAAAQAB0UVAAMABgIEZMzAAcAFAQd0VFACUAYCBAQMDAFMBUNHBRQVAnQGYGREzAzBHAUTd3RUBFIEBgAEAAwADUQHRAcBFQMWZgRkBEDMwc0VB3QHcFRSVgYEBAQEDA1NQ0cHBwUUFHZgZERERMwMHRN3d3d0VAQGAAQAAAAMAAdEBAADARcCBGQEAABAzcEHdAQAA3BWUgQEBAAABA1PRwQEAwMFFnRkRAQAQETNH3d0BANzdFYEBAAEAAQAD0QHAAcABwMUZARABEAEQc90B3AHcAdyVAQEBAQEBAdPBwcHBwcHBHRERERERERHf3d3d3d3d3QE=";
    private static final boolean[][] STRUCTURE = decode();
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    public LoliAltarBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModContent.LOLI_PICKAXE.get()) || held.is(ModContent.SMALL_LOLI_PICKAXE.get())) {
            if (level instanceof ServerLevel server) activate(server, pos);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (held.isEmpty() && player.isShiftKeyDown() && player.getAbilities().instabuild && level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            creativeBuild(server, pos, serverPlayer);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private void creativeBuild(ServerLevel level, BlockPos center, ServerPlayer player) {
        if (!canBuild(level, center)) { PENDING.remove(player.getUUID()); player.displayClientMessage(Component.literal("Loli altar preflight failed: the 63x63 area is obstructed."), true); return; }
        long now = level.getGameTime();
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null || !pending.dimension.equals(level.dimension()) || !pending.center.equals(center) || now > pending.expires) {
            PENDING.put(player.getUUID(), new Pending(level.dimension(), center.immutable(), now + 200));
            player.displayClientMessage(Component.literal("Preflight passed. Sneak-use empty hand again within 10 seconds to build."), true);
            return;
        }
        PENDING.remove(player.getUUID());
        visit(center, (cursor, altar) -> level.setBlock(cursor, altar ? defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
    }

    private boolean canBuild(Level level, BlockPos center) {
        final boolean[] ok = {true};
        visit(center, (cursor, altar) -> { if (!level.hasChunkAt(cursor)) ok[0] = false; else { BlockState found = level.getBlockState(cursor); if (!found.is(this) && !found.isAir() && !found.canBeReplaced()) ok[0] = false; } });
        return ok[0];
    }

    private boolean matches(Level level, BlockPos center) {
        final boolean[] ok = {true};
        visit(center, (cursor, altar) -> { if (!level.hasChunkAt(cursor)) ok[0] = false; else if (altar ? !level.getBlockState(cursor).is(this) : !level.getBlockState(cursor).isAir()) ok[0] = false; });
        return ok[0];
    }

    private void activate(ServerLevel level, BlockPos center) {
        if (!matches(level, center)) return;
        visit(center, (cursor, altar) -> level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
        LoliEntity loli = ModContent.LOLI_ENTITY.get().create(level);
        if (loli != null) { loli.moveTo(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D); level.addFreshEntity(loli); }
    }

    private static void visit(BlockPos center, Visitor visitor) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            cursor.set(center.getX() + dx, center.getY(), center.getZ() + dz);
            visitor.accept(cursor, STRUCTURE[dx + RADIUS][dz + RADIUS]);
        }
    }

    private static boolean[][] decode() {
        byte[] packed = Base64.getDecoder().decode(BITS);
        boolean[][] value = new boolean[SIZE][SIZE]; int count = 0;
        for (int i = 0; i < SIZE * SIZE; i++) { value[i / SIZE][i % SIZE] = (packed[i / 8] & (1 << (i % 8))) != 0; if (value[i / SIZE][i % SIZE]) count++; }
        if (count != ALTAR_COUNT) throw new IllegalStateException("Invalid Loli altar pattern: " + count);
        return value;
    }

    @FunctionalInterface private interface Visitor { void accept(BlockPos.MutableBlockPos pos, boolean altar); }
    private record Pending(net.minecraft.resources.ResourceKey<Level> dimension, BlockPos center, long expires) { }
}
