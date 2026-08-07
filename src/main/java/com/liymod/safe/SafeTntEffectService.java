package com.liymod.safe;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Server-authoritative dispatcher for safe TNT effects.
 *
 * <p>Configuration is deliberately memory-only. All three effects are disabled by default and no
 * setting is read from or written to disk.</p>
 */
public final class SafeTntEffectService {
    public static final Settings DEFAULT_SETTINGS = new Settings(false, false, false);

    private static final int FAIL_RESPOND_SLOWNESS_AMPLIFIER = 4;
    private static final int FAIL_RESPOND_MINING_FATIGUE_AMPLIFIER = 3;
    private static final AtomicReference<Settings> SETTINGS = new AtomicReference<>(DEFAULT_SETTINGS);

    private SafeTntEffectService() {
    }

    public static Settings settings() {
        return SETTINGS.get();
    }

    public static void configure(Settings settings) {
        SETTINGS.set(Objects.requireNonNull(settings, "settings"));
    }

    public static void setEnabled(SafeTntEffect effect, boolean enabled) {
        Objects.requireNonNull(effect, "effect");
        SETTINGS.updateAndGet(current -> current.withEnabled(effect, enabled));
    }

    public static boolean isEnabled(SafeTntEffect effect) {
        return SETTINGS.get().isEnabled(Objects.requireNonNull(effect, "effect"));
    }

    public static void resetToDefaults() {
        SETTINGS.set(DEFAULT_SETTINGS);
    }

    /** Applies one configured safe effect to one affected player. */
    public static boolean apply(ServerPlayer player, SafeTntEffect effect) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(effect, "effect");
        if (!isEnabled(effect)) {
            return false;
        }

        return switch (effect) {
            case BLUE_SCREEN -> sendPresentation(player, effect);
            case EXIT -> {
                player.connection.disconnect(Component.translatable("disconnect.liymod.safe_tnt_exit"));
                yield true;
            }
            case FAIL_RESPOND -> {
                int duration = effect.durationTicks();
                player.addEffect(new MobEffectInstance(
                        MobEffects.SLOWNESS,
                        duration,
                        FAIL_RESPOND_SLOWNESS_AMPLIFIER,
                        false,
                        false,
                        true));
                player.addEffect(new MobEffectInstance(
                        MobEffects.MINING_FATIGUE,
                        duration,
                        FAIL_RESPOND_MINING_FATIGUE_AMPLIFIER,
                        false,
                        false,
                        true));
                sendPresentation(player, effect);
                yield true;
            }
        };
    }

    private static boolean sendPresentation(ServerPlayer player, SafeTntEffect effect) {
        if (!ServerPlayNetworking.canSend(player, SafeTntEffectPayload.TYPE)) {
            return false;
        }
        ServerPlayNetworking.send(player, new SafeTntEffectPayload(effect));
        return true;
    }

    /** Immutable, memory-only switches; all fields default to false through {@link #DEFAULT_SETTINGS}. */
    public record Settings(boolean blueScreenEnabled, boolean exitEnabled, boolean failRespondEnabled) {
        public boolean isEnabled(SafeTntEffect effect) {
            return switch (effect) {
                case BLUE_SCREEN -> blueScreenEnabled;
                case EXIT -> exitEnabled;
                case FAIL_RESPOND -> failRespondEnabled;
            };
        }

        public Settings withEnabled(SafeTntEffect effect, boolean enabled) {
            return switch (effect) {
                case BLUE_SCREEN -> new Settings(enabled, exitEnabled, failRespondEnabled);
                case EXIT -> new Settings(blueScreenEnabled, enabled, failRespondEnabled);
                case FAIL_RESPOND -> new Settings(blueScreenEnabled, exitEnabled, enabled);
            };
        }
    }
}
