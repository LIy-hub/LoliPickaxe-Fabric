package com.liymod.combat;

import com.liymod.registry.ModContent;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** Per-server alternating immunity audio with same-tick event deduplication. */
public final class LoliImmunityFeedback {
    private static final Map<MinecraftServer, SequenceState> SEQUENCES = new WeakHashMap<>();
    private LoliImmunityFeedback() { }

    public static void play(ServerLevel level, @Nullable Entity attacker, Entity target) {
        SoundEvent sound = select(level, attacker, target);
        if (sound == null) return;
        play(attacker, sound);
        if (target != attacker) play(target, sound);
    }

    @Nullable
    private static SoundEvent select(ServerLevel level, @Nullable Entity attacker, Entity target) {
        MinecraftServer server = level.getServer();
        int tick = server.getTickCount();
        ImmunityEvent event = new ImmunityEvent(level.dimension(), attacker == null ? null : attacker.getUUID(), target.getUUID());
        synchronized (SEQUENCES) {
            SequenceState state = SEQUENCES.computeIfAbsent(server, ignored -> new SequenceState());
            if (state.tick != tick) { state.tick = tick; state.events.clear(); }
            if (!state.events.add(event)) return null;
            SoundEvent selected = state.first ? ModContent.LOLI_IMMUNITY_FIRST.get() : ModContent.LOLI_IMMUNITY_SECOND.get();
            state.first = !state.first;
            return selected;
        }
    }

    private static void play(@Nullable Entity participant, SoundEvent sound) {
        if (participant instanceof ServerPlayer player) player.playSound(sound, 1.0F, 1.0F);
    }

    private record ImmunityEvent(ResourceKey<Level> dimension, @Nullable UUID attacker, UUID target) { }
    private static final class SequenceState {
        private final Set<ImmunityEvent> events = new HashSet<>();
        private int tick = Integer.MIN_VALUE;
        private boolean first = true;
    }
}
