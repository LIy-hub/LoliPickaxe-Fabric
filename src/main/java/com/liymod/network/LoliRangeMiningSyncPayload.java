package com.liymod.network;

import com.liymod.LiyMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** One bounded client update for all blocks changed by a range-mining action. */
public record LoliRangeMiningSyncPayload(List<BlockPos> positions) implements CustomPacketPayload {
    public static final int MAX_BLOCKS = 4096;
    public static final Type<LoliRangeMiningSyncPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "range_mining_sync")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, LoliRangeMiningSyncPayload> CODEC =
            CustomPacketPayload.codec(LoliRangeMiningSyncPayload::write, LoliRangeMiningSyncPayload::read);

    public LoliRangeMiningSyncPayload {
        if (positions.size() > MAX_BLOCKS) {
            throw new IllegalArgumentException("Too many range-mining positions: " + positions.size());
        }
        positions = List.copyOf(positions);
    }

    private static LoliRangeMiningSyncPayload read(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_BLOCKS) {
            throw new IllegalArgumentException("Invalid range-mining position count: " + count);
        }
        List<BlockPos> positions = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            positions.add(buffer.readBlockPos());
        }
        return new LoliRangeMiningSyncPayload(positions);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(positions.size());
        for (BlockPos position : positions) {
            buffer.writeBlockPos(position);
        }
    }

    @Override
    public Type<LoliRangeMiningSyncPayload> type() {
        return TYPE;
    }
}
