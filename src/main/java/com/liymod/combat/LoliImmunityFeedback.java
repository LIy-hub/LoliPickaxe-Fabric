package com.liymod.combat;

import com.liymod.sound.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class LoliImmunityFeedback {
    private static final Map<MinecraftServer, SequenceState> SEQUENCES =
            new WeakHashMap<>();

    private LoliImmunityFeedback() {
    }

    public static void play(
            ServerWorld world,
            @Nullable Entity attacker,
            Entity protectedTarget
    ) {
        SoundEvent sound = selectNextSound(world, attacker, protectedTarget);
        if (sound == null) {
            return;
        }

        playForParticipant(attacker, sound);
        if (protectedTarget != attacker) {
            playForParticipant(protectedTarget, sound);
        }
    }

    @Nullable
    private static SoundEvent selectNextSound(
            ServerWorld world,
            @Nullable Entity attacker,
            Entity protectedTarget
    ) {
        MinecraftServer server = world.getServer();
        int tick = server.getTicks();
        ImmunityEvent event = new ImmunityEvent(
                world.getRegistryKey(),
                attacker == null ? null : attacker.getUuid(),
                protectedTarget.getUuid()
        );

        synchronized (SEQUENCES) {
            SequenceState state = SEQUENCES.computeIfAbsent(
                    server,
                    ignored -> new SequenceState()
            );
            if (state.dedupeTick != tick) {
                state.dedupeTick = tick;
                state.eventsThisTick.clear();
            }
            if (!state.eventsThisTick.add(event)) {
                return null;
            }

            SoundEvent selected = state.playFirstNext
                    ? ModSounds.LOLI_IMMUNITY_FIRST
                    : ModSounds.LOLI_IMMUNITY_SECOND;
            state.playFirstNext = !state.playFirstNext;
            return selected;
        }
    }

    private static void playForParticipant(
            @Nullable Entity participant,
            SoundEvent sound
    ) {
        if (participant instanceof ServerPlayerEntity player) {
            player.playSound(sound, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }

    private record ImmunityEvent(
            RegistryKey<World> world,
            @Nullable UUID attacker,
            UUID protectedTarget
    ) {
    }

    private static final class SequenceState {
        private final Set<ImmunityEvent> eventsThisTick = new HashSet<>();
        private int dedupeTick = Integer.MIN_VALUE;
        private boolean playFirstNext = true;
    }
}
