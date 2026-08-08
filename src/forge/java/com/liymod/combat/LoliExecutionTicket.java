package com.liymod.combat;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

public final class LoliExecutionTicket {
    public enum State { PREPARE, COMMITTING, DEAD_LOCK }

    private final UUID targetId;
    @Nullable private final UUID attackerId;
    private final DamageSource damageSource;
    private final ExecutionAuthority authority;
    private Entity target;
    private State state = State.PREPARE;
    private boolean deathCommitted;
    private int lockedTicks;

    LoliExecutionTicket(Entity target, @Nullable Entity attacker, DamageSource source, ExecutionAuthority authority) {
        this.targetId = target.getUUID();
        this.attackerId = attacker == null ? null : attacker.getUUID();
        this.damageSource = source;
        this.authority = authority;
        this.target = target;
    }

    public UUID targetId() { return targetId; }
    @Nullable public UUID attackerId() { return attackerId; }
    public DamageSource damageSource() { return damageSource; }
    public ExecutionAuthority authority() { return authority; }
    public Entity target() { return target; }
    public void bind(Entity replacement) { target = replacement; }
    public State state() { return state; }
    public void beginCommit() { if (state == State.PREPARE) state = State.COMMITTING; }
    public void lock() { beginCommit(); state = State.DEAD_LOCK; }
    public boolean isTerminal() { return state != State.PREPARE; }
    public boolean isDeathCommitted() { return deathCommitted; }
    public void markDeathCommitted() { deathCommitted = true; }
    public int incrementLockedTicks() { return ++lockedTicks; }
}
