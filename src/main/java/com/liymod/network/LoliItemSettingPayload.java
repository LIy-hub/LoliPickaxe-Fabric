package com.liymod.network;

import com.liymod.LiyMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LoliItemSettingPayload(String optionId, String encodedValue) implements CustomPacketPayload {
    public static final int MAX_OPTION_ID_LENGTH = 64;
    public static final int MAX_VALUE_LENGTH = 256;
    public static final Type<LoliItemSettingPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "loli_item_setting")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, LoliItemSettingPayload> CODEC =
            CustomPacketPayload.codec(LoliItemSettingPayload::write, LoliItemSettingPayload::read);

    public LoliItemSettingPayload {
        optionId = limit(optionId, MAX_OPTION_ID_LENGTH);
        encodedValue = limit(encodedValue, MAX_VALUE_LENGTH);
    }

    private static LoliItemSettingPayload read(RegistryFriendlyByteBuf buffer) {
        return new LoliItemSettingPayload(
                buffer.readUtf(MAX_OPTION_ID_LENGTH),
                buffer.readUtf(MAX_VALUE_LENGTH)
        );
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(optionId, MAX_OPTION_ID_LENGTH);
        buffer.writeUtf(encodedValue, MAX_VALUE_LENGTH);
    }

    private static String limit(String value, int maximumCodeUnits) {
        if (value == null) {
            return "";
        }
        return value.length() <= maximumCodeUnits ? value : value.substring(0, maximumCodeUnits);
    }

    @Override
    public Type<LoliItemSettingPayload> type() {
        return TYPE;
    }
}
