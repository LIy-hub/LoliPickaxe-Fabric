package com.liymod.mixin;

import com.liymod.protection.LoliProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Predicate;

@Mixin(value = ProjectileUtil.class, priority = Integer.MAX_VALUE)
public abstract class ProjectileUtilMixin {
    @ModifyVariable(
            method = {
                    "getCollision(Lnet/minecraft/entity/Entity;Ljava/util/function/Predicate;)Lnet/minecraft/util/hit/HitResult;",
                    "getCollision(Lnet/minecraft/entity/Entity;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/HitResult;",
                    "raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;",
                    "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/projectile/ProjectileEntity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Lnet/minecraft/util/hit/EntityHitResult;",
                    "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;F)Lnet/minecraft/util/hit/EntityHitResult;"
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
