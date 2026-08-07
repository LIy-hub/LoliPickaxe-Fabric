package com.liymod.network;

import com.liymod.LiyMod;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PasswordUpdatePayload(String password) implements CustomPacketPayload {
    public static final int MAX_CODE_POINTS = 64;
    public static final int MAX_UTF8_BYTES = 256;
    public static final Type<PasswordUpdatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "password_update")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, PasswordUpdatePayload> CODEC =
            CustomPacketPayload.codec(PasswordUpdatePayload::write, PasswordUpdatePayload::read);

    public PasswordUpdatePayload {
        password = sanitize(password);
    }

    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        int end = input.offsetByCodePoints(0, Math.min(MAX_CODE_POINTS, input.codePointCount(0, input.length())));
        String byCodePoints = input.substring(0, end);
        byte[] encoded = byCodePoints.getBytes(StandardCharsets.UTF_8);
        if (encoded.length <= MAX_UTF8_BYTES) {
            return byCodePoints;
        }
        int codePoints = byCodePoints.codePointCount(0, byCodePoints.length());
        while (codePoints > 0) {
            codePoints--;
            end = byCodePoints.offsetByCodePoints(0, codePoints);
            String candidate = byCodePoints.substring(0, end);
            if (candidate.getBytes(StandardCharsets.UTF_8).length <= MAX_UTF8_BYTES) {
                return candidate;
            }
        }
        return "";
    }

    private static PasswordUpdatePayload read(RegistryFriendlyByteBuf buffer) {
        byte[] encoded = buffer.readByteArray(MAX_UTF8_BYTES);
        return new PasswordUpdatePayload(new String(encoded, StandardCharsets.UTF_8));
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        byte[] encoded = password.getBytes(StandardCharsets.UTF_8);
        buffer.writeByteArray(encoded.length <= MAX_UTF8_BYTES
                ? encoded
                : Arrays.copyOf(encoded, MAX_UTF8_BYTES));
    }

    @Override
    public Type<PasswordUpdatePayload> type() {
        return TYPE;
    }
}
