package com.liymod.compat;

import com.mojang.authlib.GameProfile;
import com.liymod.LiyMod;
import com.liymod.combat.LoliErasureService;
import com.liymod.combat.LoliExecutionManager;
import com.liymod.entity.LoliEntity;
import com.liymod.mixin.accessor.EntityAccessor;
import com.liymod.protection.LoliProtection;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Optional, loader-neutral strength compatibility for destructive third-party mods.
 *
 * <p>No third-party class is linked at compile time. Registry identifiers provide the
 * stable behavior boundary; narrowly-scoped reflection is attempted only when the
 * corresponding mod id is actually loaded.</p>
 */
public final class StrengthConfrontation {
    public static final Identifier FOREVER_LOVE_SWORD = Identifier.fromNamespaceAndPath(
            "forever_love_sword",
            "forever_love_sword"
    );
    public static final Identifier ENTITY_ERASER = Identifier.fromNamespaceAndPath(
            "entityeraser_re",
            "entity_eraser"
    );
    public static final Identifier ENTITY_ERASER_KILL_SELF = Identifier.fromNamespaceAndPath(
            "entityeraser_re",
            "kill_self"
    );
    public static final Identifier PIG2 = Identifier.fromNamespaceAndPath("pig2mod", "pig2");

    private static final int PIG2_QUIET_WINDOW_TICKS = 20 * 60;
    private static final Set<LoliEntity> TRACKED_LOLIS = Collections.newSetFromMap(
            new IdentityHashMap<>()
    );
    private static final Map<MinecraftServer, Long> PIG2_SUPPRESSION_UNTIL =
            new IdentityHashMap<>();
    private static final Map<UUID, GameProfile> ERASER_PROFILES_ADDED = new HashMap<>();
    private static final Set<String> REFLECTION_FAILURES = new java.util.HashSet<>();

    private static boolean pig2ReflectionResolved;
    private static Method pig2PermitEntity;
    private static boolean foreverReflectionResolved;
    private static Method foreverRemoveDefense;
    private static boolean eraserReflectionResolved;
    private static Field eraserDeadEntities;
    private static Field eraserProtectedPlayers;

    private StrengthConfrontation() {
    }

    public static void registerEvents() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof LoliEntity loli) {
                TRACKED_LOLIS.add(loli);
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (!(entity instanceof LoliEntity loli)) {
                return;
            }
            Entity.RemovalReason reason = loli.getRemovalReason();
            if (loli.isDispersalRemovalAllowed()
                    || (reason != null && !loli.blocksRemoval(reason))) {
                TRACKED_LOLIS.remove(loli);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(StrengthConfrontation::onServerTick);
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> releaseEntityEraserPlayerDefense(handler.player)
        );
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            TRACKED_LOLIS.removeIf(loli -> loli.level().getServer() == server);
            PIG2_SUPPRESSION_UNTIL.remove(server);
            releaseAllEntityEraserPlayerDefense();
        });
        LiyMod.LOGGER.info(
                "Registered optional strength confrontation for Forever Love Sword, EntityEraser and PIG2"
        );
    }

    /** Called only after same-item immunity has already won. */
    public static void prepareAbsoluteExecution(Entity target) {
        if (hasItem(target, FOREVER_LOVE_SWORD)) {
            clearForeverDefense(target);
        }
        if (target instanceof Player player
                && (hasItem(player, ENTITY_ERASER)
                || hasItem(player, ENTITY_ERASER_KILL_SELF))) {
            clearEntityEraserPlayerDefense(player);
        }
    }

    /** Arms clone/revival suppression only after the normal absolute dead lock succeeded. */
    public static void onAbsoluteDeadLock(Entity target) {
        if (!isPig2(target)) {
            return;
        }
        MinecraftServer server = target.level().getServer();
        if (server != null) {
            PIG2_SUPPRESSION_UNTIL.put(
                    server,
                    (long) server.getTickCount() + PIG2_QUIET_WINDOW_TICKS
            );
        }
    }

    public static boolean isAuditedWeapon(ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return FOREVER_LOVE_SWORD.equals(id)
                || ENTITY_ERASER.equals(id)
                || ENTITY_ERASER_KILL_SELF.equals(id);
    }

    public static boolean isPig2(Entity entity) {
        return PIG2.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (LoliProtection.isProtected(player)) {
                neutralizeForeignErasure(player);
                ensureEntityEraserPlayerDefense(player);
                recoverEntityIndex(player);
            } else {
                releaseEntityEraserPlayerDefense(player);
            }
        }

        for (LoliEntity loli : new ArrayList<>(TRACKED_LOLIS)) {
            if (loli.level().getServer() != server || loli.isDispersalRemovalAllowed()) {
                TRACKED_LOLIS.remove(loli);
                continue;
            }
            neutralizeForeignErasure(loli);
            recoverEntityIndex(loli);
        }

        suppressPig2Revival(server);
    }

    private static void suppressPig2Revival(MinecraftServer server) {
        long now = server.getTickCount();
        long until = PIG2_SUPPRESSION_UNTIL.getOrDefault(server, Long.MIN_VALUE);
        if (now > until) {
            PIG2_SUPPRESSION_UNTIL.remove(server);
            return;
        }

        ArrayList<Entity> revived = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (isPig2(entity) && !LoliExecutionManager.isDeadLocked(entity)) {
                    revived.add(entity);
                }
            }
        }
        for (Entity entity : revived) {
            if (LoliErasureService.executeAbsolute(null, entity)
                    == LoliErasureService.Result.EXECUTED) {
                PIG2_SUPPRESSION_UNTIL.put(
                        server,
                        (long) server.getTickCount() + PIG2_QUIET_WINDOW_TICKS
                );
            }
        }
    }

    private static void neutralizeForeignErasure(Entity entity) {
        permitEntityInPig2(entity);
        clearEntityEraserDeadState(entity);
    }

    private static void recoverEntityIndex(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        Entity indexed = level.getEntity(entity.getUUID());
        if (indexed == entity) {
            return;
        }
        if (indexed != null) {
            warnOnce(
                    "duplicate:" + entity.getUUID(),
                    "Refusing to recover strength-protected entity {} because its UUID is occupied",
                    entity.getUUID()
            );
            return;
        }

        Entity.RemovalReason reason = entity.getRemovalReason();
        if (reason != null
                && reason != Entity.RemovalReason.KILLED
                && reason != Entity.RemovalReason.DISCARDED) {
            return;
        }
        if (reason != null) {
            ((EntityAccessor) entity).lolipickaxe$unsetRemoved();
        }

        boolean recovered;
        if (entity instanceof ServerPlayer player) {
            level.addRespawnedPlayer(player);
            recovered = level.getEntity(player.getUUID()) == player;
        } else {
            recovered = level.addWithUUID(entity);
        }
        if (recovered) {
            LiyMod.LOGGER.warn(
                    "Recovered strength-protected {} after a foreign entity-index erasure",
                    entity.getUUID()
            );
        }
    }

    private static boolean hasItem(Entity entity, Identifier wanted) {
        if (entity instanceof Player player) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                if (wanted.equals(BuiltInRegistries.ITEM.getKey(
                        player.getInventory().getItem(slot).getItem()
                ))) {
                    return true;
                }
            }
            return false;
        }
        if (entity instanceof LivingEntity living) {
            return wanted.equals(BuiltInRegistries.ITEM.getKey(living.getMainHandItem().getItem()))
                    || wanted.equals(BuiltInRegistries.ITEM.getKey(living.getOffhandItem().getItem()));
        }
        return false;
    }

    private static void permitEntityInPig2(Entity entity) {
        if (!FabricLoader.getInstance().isModLoaded("pig2mod")) {
            return;
        }
        resolvePig2Reflection();
        invokeOptional(pig2PermitEntity, null, entity, "pig2-permit");
    }

    private static void clearForeverDefense(Entity target) {
        if (!FabricLoader.getInstance().isModLoaded("forever_love_sword")) {
            return;
        }
        resolveForeverReflection();
        invokeOptional(foreverRemoveDefense, null, target, "forever-defense");
    }

    private static void clearEntityEraserDeadState(Entity entity) {
        if (!FabricLoader.getInstance().isModLoaded("entityeraser_re")) {
            return;
        }
        resolveEntityEraserReflection();
        Object value = getStaticField(eraserDeadEntities, "entityeraser-dead-map");
        if (value instanceof Map<?, ?> map) {
            map.remove(entity);
        }
    }

    private static void clearEntityEraserPlayerDefense(Player player) {
        if (!FabricLoader.getInstance().isModLoaded("entityeraser_re")) {
            return;
        }
        resolveEntityEraserReflection();
        Object value = getStaticField(eraserProtectedPlayers, "entityeraser-protected-set");
        if (value instanceof Set<?> set) {
            set.remove(player.getGameProfile());
        }
        ERASER_PROFILES_ADDED.remove(player.getUUID());
    }

    @SuppressWarnings("unchecked")
    private static void ensureEntityEraserPlayerDefense(Player player) {
        if (!FabricLoader.getInstance().isModLoaded("entityeraser_re")
                || ERASER_PROFILES_ADDED.containsKey(player.getUUID())) {
            return;
        }
        resolveEntityEraserReflection();
        Object value = getStaticField(eraserProtectedPlayers, "entityeraser-protected-set");
        if (value instanceof Set<?> set
                && ((Set<Object>) set).add(player.getGameProfile())) {
            ERASER_PROFILES_ADDED.put(player.getUUID(), player.getGameProfile());
        }
    }

    private static void releaseEntityEraserPlayerDefense(Player player) {
        GameProfile profile = ERASER_PROFILES_ADDED.remove(player.getUUID());
        if (profile == null) {
            return;
        }
        Object value = getStaticField(eraserProtectedPlayers, "entityeraser-protected-set");
        if (value instanceof Set<?> set) {
            set.remove(profile);
        }
    }

    private static void releaseAllEntityEraserPlayerDefense() {
        Object value = getStaticField(eraserProtectedPlayers, "entityeraser-protected-set");
        if (value instanceof Set<?> set) {
            ERASER_PROFILES_ADDED.values().forEach(set::remove);
        }
        ERASER_PROFILES_ADDED.clear();
    }

    private static void resolvePig2Reflection() {
        if (pig2ReflectionResolved) {
            return;
        }
        pig2ReflectionResolved = true;
        try {
            Class<?> type = Class.forName(
                    "kakiku.pig2mod.entity.Pig2",
                    false,
                    StrengthConfrontation.class.getClassLoader()
            );
            pig2PermitEntity = type.getMethod("permitEntity", Entity.class);
        } catch (ReflectiveOperationException | LinkageError exception) {
            reflectionFailure("pig2-resolve", exception);
        }
    }

    private static void resolveForeverReflection() {
        if (foreverReflectionResolved) {
            return;
        }
        foreverReflectionResolved = true;
        try {
            Class<?> type = Class.forName(
                    "com.wzz.forever_love_sword.ForeverUtils",
                    false,
                    StrengthConfrontation.class.getClassLoader()
            );
            foreverRemoveDefense = type.getMethod("remove", Entity.class);
        } catch (ReflectiveOperationException | LinkageError exception) {
            reflectionFailure("forever-resolve", exception);
        }
    }

    private static void resolveEntityEraserReflection() {
        if (eraserReflectionResolved) {
            return;
        }
        eraserReflectionResolved = true;
        try {
            Class<?> type = Class.forName(
                    "net.apphhzp.entityeraser_re.util.EntityUtil",
                    false,
                    StrengthConfrontation.class.getClassLoader()
            );
            eraserDeadEntities = findStaticField(type, "deadEntity", Map.class);
            eraserProtectedPlayers = findStaticField(type, "protectedPlayer", Set.class);
        } catch (ReflectiveOperationException | LinkageError exception) {
            reflectionFailure("entityeraser-resolve", exception);
        }
    }

    private static Field findStaticField(
            Class<?> owner,
            String preferredName,
            Class<?> fieldType
    ) throws ReflectiveOperationException {
        try {
            Field preferred = owner.getDeclaredField(preferredName);
            if (Modifier.isStatic(preferred.getModifiers())
                    && fieldType.isAssignableFrom(preferred.getType())
                    && preferred.trySetAccessible()) {
                return preferred;
            }
        } catch (NoSuchFieldException ignored) {
            // Fall through to the structural lookup for renamed/obfuscated builds.
        }
        for (Field field : owner.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())
                    && fieldType.isAssignableFrom(field.getType())
                    && field.trySetAccessible()) {
                return field;
            }
        }
        throw new NoSuchFieldException(owner.getName() + "." + preferredName);
    }

    private static Object getStaticField(Field field, String key) {
        if (field == null) {
            return null;
        }
        try {
            return field.get(null);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            reflectionFailure(key, exception);
            return null;
        }
    }

    private static void invokeOptional(
            Method method,
            Object receiver,
            Object argument,
            String key
    ) {
        if (method == null) {
            return;
        }
        try {
            method.invoke(receiver, argument);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            reflectionFailure(key, exception);
        }
    }

    private static void reflectionFailure(String key, Throwable exception) {
        if (REFLECTION_FAILURES.add(key)) {
            LiyMod.LOGGER.warn(
                    "Optional strength compatibility {} is unavailable; registry-level protection remains active",
                    key,
                    exception
            );
        }
    }

    private static void warnOnce(String key, String message, Object argument) {
        if (REFLECTION_FAILURES.add(key)) {
            LiyMod.LOGGER.warn(message, argument);
        }
    }
}
