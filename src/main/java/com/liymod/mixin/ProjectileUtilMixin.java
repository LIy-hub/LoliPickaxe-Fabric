package com.liymod.mixin;

import com.liymod.protection.LoliProtection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;

@Mixin(value = ProjectileUtil.class, priority = Integer.MAX_VALUE)
public abstract class ProjectileUtilMixin {
    @ModifyVariable(
            method = {
                    "getHitResultOnMoveVector(Lnet/minecraft/world/entity/Entity;Ljava/util/function/Predicate;)Lnet/minecraft/world/phys/HitResult;",
                    "getHitResultOnViewVector(Lnet/minecraft/world/entity/Entity;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/HitResult;",
                    "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;",
                    "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Lnet/minecraft/world/phys/EntityHitResult;",
                    "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;"
            },
            at = @At("HEAD"),
            argsOnly = true
    )
    private static Predicate<Entity> lolipickaxe$excludeProtectedPlayers(
            Predicate<Entity> original
    ) {
        return entity -> !LoliProtection.isUntargetable(entity) && original.test(entity);
    }
}
