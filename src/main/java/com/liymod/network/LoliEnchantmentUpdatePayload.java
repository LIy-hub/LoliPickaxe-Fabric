package com.liymod.network;

import com.liymod.LiyMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LoliEnchantmentUpdatePayload(String enchantmentId, int level) implements CustomPacketPayload {
    public static final int MAX_ID_LENGTH = 128;
    public static final Type<LoliEnchantmentUpdatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "loli_enchantment_update")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, LoliEnchantmentUpdatePayload> CODEC =
            CustomPacketPayload.codec(LoliEnchantmentUpdatePayload::write, LoliEnchantmentUpdatePayload::read);

    public LoliEnchantmentUpdatePayload {
        enchantmentId = limit(enchantmentId);
    }

    private static LoliEnchantmentUpdatePayload read(RegistryFriendlyByteBuf buffer) {
        return new LoliEnchantmentUpdatePayload(buffer.readUtf(MAX_ID_LENGTH), buffer.readVarInt());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(enchantmentId, MAX_ID_LENGTH);
        buffer.writeVarInt(level);
    }

    private static String limit(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= MAX_ID_LENGTH ? value : value.substring(0, MAX_ID_LENGTH);
    }

    @Override
    public Type<LoliEnchantmentUpdatePayload> type() {
        return TYPE;
    }
}
