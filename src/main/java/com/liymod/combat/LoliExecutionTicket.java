package com.liymod.combat;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

public final class LoliExecutionTicket {
    public enum State {
        PREPARE,
        COMMITTING,
        DEAD_LOCK
    }

    private final UUID targetId;
    private final int originalEntityId;
    @Nullable
    private final UUID attackerId;
    private final DamageSource damageSource;
    private final ExecutionAuthority authority;
    private Entity target;
    private State state = State.PREPARE;
    private boolean deathCommitted;
    private int lockedTicks;

    LoliExecutionTicket(
            Entity target,
            @Nullable Entity attacker,
            DamageSource damageSource,
            ExecutionAuthority authority
    ) {
        this.targetId = target.getUUID();
        this.originalEntityId = target.getId();
        this.attackerId = attacker == null ? null : attacker.getUUID();
        this.damageSource = damageSource;
        this.authority = authority;
        this.target = target;
    }

    public UUID targetId() {
        return targetId;
    }

    public int originalEntityId() {
        return originalEntityId;
    }

    @Nullable
    public UUID attackerId() {
        return attackerId;
    }

    public DamageSource damageSource() {
        return damageSource;
    }

    public ExecutionAuthority authority() {
        return authority;
    }

    public Entity target() {
        return target;
    }

    public void bind(Entity replacement) {
        this.target = replacement;
    }

    public State state() {
        return state;
    }

    public void beginCommit() {
        if (state == State.PREPARE) {
            state = State.COMMITTING;
        }
    }

    public void lock() {
        if (state == State.PREPARE) {
            state = State.COMMITTING;
        }
        state = State.DEAD_LOCK;
    }

    public boolean isTerminal() {
        return state == State.COMMITTING || state == State.DEAD_LOCK;
    }

    public boolean isDeathCommitted() {
        return deathCommitted;
    }

    public void markDeathCommitted() {
        this.deathCommitted = true;
    }

    public int incrementLockedTicks() {
        return ++lockedTicks;
    }

    public int lockedTicks() {
        return lockedTicks;
    }
}
