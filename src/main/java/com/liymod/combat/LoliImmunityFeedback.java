package com.liymod.combat;

import com.liymod.sound.ModSounds;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
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
            ServerLevel world,
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
            ServerLevel world,
            @Nullable Entity attacker,
            Entity protectedTarget
    ) {
        MinecraftServer server = world.getServer();
        int tick = server.getTickCount();
        ImmunityEvent event = new ImmunityEvent(
                world.dimension(),
                attacker == null ? null : attacker.getUUID(),
                protectedTarget.getUUID()
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
        if (participant instanceof ServerPlayer player) {
            player.playSound(sound, 1.0F, 1.0F);
        }
    }

    private record ImmunityEvent(
            ResourceKey<Level> world,
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
