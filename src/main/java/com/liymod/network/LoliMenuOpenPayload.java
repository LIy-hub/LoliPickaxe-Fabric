package com.liymod.network;

import com.liymod.LiyMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LoliMenuOpenPayload(Mode mode) implements CustomPacketPayload {
    public enum Mode {
        CONFIG("loli_config"),
        ENCHANTMENT("loli_enchantment"),
        EFFECT("loli_effect"),
        TELEPORT("loli_teleport");

        private final String translationSuffix;

        Mode(String translationSuffix) {
            this.translationSuffix = translationSuffix;
        }

        public String translationSuffix() {
            return translationSuffix;
        }
    }

    public static final Type<LoliMenuOpenPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "loli_menu_open")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, LoliMenuOpenPayload> CODEC =
            CustomPacketPayload.codec(LoliMenuOpenPayload::write, LoliMenuOpenPayload::read);

    public LoliMenuOpenPayload {
        mode = mode == null ? Mode.CONFIG : mode;
    }

    private static LoliMenuOpenPayload read(RegistryFriendlyByteBuf buffer) {
        int ordinal = Math.clamp(buffer.readUnsignedByte(), 0, Mode.values().length - 1);
        return new LoliMenuOpenPayload(Mode.values()[ordinal]);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeByte(mode.ordinal());
    }

    @Override
    public Type<LoliMenuOpenPayload> type() {
        return TYPE;
    }
}
