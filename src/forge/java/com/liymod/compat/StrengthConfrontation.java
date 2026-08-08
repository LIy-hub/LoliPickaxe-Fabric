package com.liymod.compat;

import com.liymod.LiyMod;
import com.liymod.combat.LoliErasureService;
import com.liymod.protection.LoliProtection;
import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

public final class StrengthConfrontation {
    public static final ResourceLocation FOREVER_LOVE_SWORD = id("forever_love_sword", "forever_love_sword");
    public static final ResourceLocation ENTITY_ERASER = id("entityeraser_re", "entity_eraser");
    public static final ResourceLocation ENTITY_ERASER_KILL_SELF = id("entityeraser_re", "kill_self");
    public static final ResourceLocation PIG2 = id("pig2mod", "pig2");
    private static final int SUPPRESSION_TICKS = 20 * 60;
    private static final Map<MinecraftServer, Long> PIG2_UNTIL = new IdentityHashMap<>();
    private static final Map<UUID, Long> UUID_UNTIL = new HashMap<>();
    private static final Map<ResourceLocation, Long> TYPE_UNTIL = new HashMap<>();
    private static final Map<UUID, GameProfile> ERASER_PROFILES = new HashMap<>();
    private static final Set<String> WARNED = new HashSet<>();
    private static Method pig2Permit;
    private static Field foreverNames;
    private static Field foreverDeathNames;
    private static Field foreverDeathPlayers;
    private static Field eraserDead;
    private static Field eraserProtected;
    private static boolean pig2Resolved, foreverResolved, eraserResolved;

    private StrengthConfrontation() { }

    public static void prepareAbsoluteExecution(Entity target) {
        // Both mods retain static defense records after the item leaves a hand
        // or inventory. Clear only this target's record whenever the mod is
        // loaded; never call either mod's destructive kill entry points.
        clearForeverDefense(target);
        clearForeverDeath(target);
        if (target instanceof Player player) disableEraserDefense(player);
    }

    public static void armSuppression(Entity target, boolean absolute) {
        if (!absolute) return;
        MinecraftServer server = target.getServer();
        if (server == null) return;
        long until = (long) server.getTickCount() + SUPPRESSION_TICKS;
        UUID_UNTIL.put(target.getUUID(), until);
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (PIG2.equals(type)) {
            TYPE_UNTIL.put(type, until);
            PIG2_UNTIL.put(server, until);
        }
    }

    public static boolean suppressJoin(Entity entity) {
        MinecraftServer server = entity.getServer();
        if (server == null || LoliProtection.isProtected(entity)) return false;
        long now = server.getTickCount();
        long uuidUntil = UUID_UNTIL.getOrDefault(entity.getUUID(), Long.MIN_VALUE);
        long typeUntil = TYPE_UNTIL.getOrDefault(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()), Long.MIN_VALUE);
        return now <= uuidUntil || now <= typeUntil;
    }

    public static void serverTick(MinecraftServer server) {
        long now = server.getTickCount();
        UUID_UNTIL.values().removeIf(until -> now > until);
        TYPE_UNTIL.values().removeIf(until -> now > until);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (LoliProtection.isProtected(player)) {
                player.revive();
                player.setInvulnerable(false);
                if (player.getHealth() <= 0.0F) player.setHealth(player.getMaxHealth());
                clearForeverDeath(player);
                permitPig2(player);
                clearEraserDead(player);
                ensureEraserDefense(player);
                recoverIndex(player);
            } else {
                releaseEraserDefense(player);
            }
        }
        long pigUntil = PIG2_UNTIL.getOrDefault(server, Long.MIN_VALUE);
        if (now <= pigUntil) {
            for (ServerLevel level : server.getAllLevels()) {
                for (Entity entity : level.getAllEntities()) {
                    if (PIG2.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))) {
                        LoliErasureService.executeAbsolute(null, entity);
                    }
                }
            }
        } else {
            PIG2_UNTIL.remove(server);
        }
    }

    public static void reset(MinecraftServer server) {
        PIG2_UNTIL.remove(server);
        UUID_UNTIL.clear();
        TYPE_UNTIL.clear();
        Object value = staticValue(eraserProtected, "eraser-protected-reset");
        if (value instanceof Set<?> set) {
            for (GameProfile profile : ERASER_PROFILES.values()) set.remove(profile);
        }
        ERASER_PROFILES.clear();
    }

    private static void recoverIndex(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level) || level.getEntity(entity.getUUID()) == entity) return;
        if (level.getEntity(entity.getUUID()) != null) return;
        if (entity.getRemovalReason() != null) entity.revive();
        if (entity instanceof ServerPlayer player) level.addRespawnedPlayer(player); else level.addWithUUID(entity);
    }

    private static boolean hasItem(Entity entity, ResourceLocation wanted) {
        if (entity instanceof Player player) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (wanted.equals(BuiltInRegistries.ITEM.getKey(player.getInventory().getItem(i).getItem()))) return true;
            }
        } else if (entity instanceof LivingEntity living) {
            return wanted.equals(BuiltInRegistries.ITEM.getKey(living.getMainHandItem().getItem()))
                    || wanted.equals(BuiltInRegistries.ITEM.getKey(living.getOffhandItem().getItem()));
        }
        return false;
    }

    private static void permitPig2(Entity entity) {
        if (!ModList.get().isLoaded("pig2mod")) return;
        if (!pig2Resolved) {
            pig2Resolved = true;
            try { pig2Permit = Class.forName("kakiku.pig2mod.entity.Pig2", false, StrengthConfrontation.class.getClassLoader()).getMethod("permitEntity", Entity.class); }
            catch (ReflectiveOperationException | LinkageError ex) { warn("pig2-resolve", ex); }
        }
        invoke(pig2Permit, entity, "pig2-permit");
    }

    @SuppressWarnings("unchecked")
    private static void clearForeverDefense(Entity entity) {
        if (!ModList.get().isLoaded("forever_love_sword")) return;
        if (!foreverResolved) {
            foreverResolved = true;
            try {
                Class<?> type = Class.forName("net.wzz.forever_love_sword.util.ForeverUtils", false, StrengthConfrontation.class.getClassLoader());
                foreverNames = field(type, "name", Set.class);
                Class<?> deathList = Class.forName("net.wzz.forever_love_sword.list.DeathList", false, StrengthConfrontation.class.getClassLoader());
                foreverDeathNames = field(deathList, "name", Set.class);
                foreverDeathPlayers = field(deathList, "players", Set.class);
            }
            catch (ReflectiveOperationException | LinkageError ex) { warn("forever-resolve", ex); }
        }
        Object value = staticValue(foreverNames, "forever-names");
        if (value instanceof Set<?> set) ((Set<Object>) set).remove(entity.getStringUUID());
    }

    @SuppressWarnings("unchecked")
    private static void clearForeverDeath(Entity entity) {
        if (!ModList.get().isLoaded("forever_love_sword")) return;
        if (!foreverResolved) clearForeverDefense(entity);
        Object names = staticValue(foreverDeathNames, "forever-death-names");
        if (names instanceof Set<?> set) ((Set<Object>) set).remove(entity.getClass().getName());
        Object players = staticValue(foreverDeathPlayers, "forever-death-players");
        if (players instanceof Set<?> set) ((Set<Object>) set).remove(entity.getStringUUID());
    }

    private static void clearEraserDead(Entity entity) {
        resolveEraser();
        Object value = staticValue(eraserDead, "eraser-dead");
        if (value instanceof Map<?, ?> map) map.remove(entity);
    }

    @SuppressWarnings("unchecked")
    private static void ensureEraserDefense(Player player) {
        if (!ModList.get().isLoaded("entityeraser_re") || ERASER_PROFILES.containsKey(player.getUUID())) return;
        resolveEraser();
        Object value = staticValue(eraserProtected, "eraser-protected");
        if (value instanceof Set<?> set && ((Set<Object>) set).add(player.getGameProfile())) ERASER_PROFILES.put(player.getUUID(), player.getGameProfile());
    }

    private static void releaseEraserDefense(Player player) {
        GameProfile profile = ERASER_PROFILES.remove(player.getUUID());
        if (profile == null) return;
        Object value = staticValue(eraserProtected, "eraser-protected");
        if (value instanceof Set<?> set) set.remove(profile);
    }

    private static void disableEraserDefense(Player player) {
        resolveEraser();
        ERASER_PROFILES.remove(player.getUUID());
        Object value = staticValue(eraserProtected, "eraser-protected");
        if (value instanceof Set<?> set) set.remove(player.getGameProfile());
    }

    private static void resolveEraser() {
        if (eraserResolved || !ModList.get().isLoaded("entityeraser_re")) return;
        eraserResolved = true;
        try {
            Class<?> type = Class.forName("net.apphhzp.entityeraser_re.util.EntityUtil", false, StrengthConfrontation.class.getClassLoader());
            eraserDead = field(type, "deadEntity", Map.class);
            eraserProtected = field(type, "protectedPlayer", Set.class);
        } catch (ReflectiveOperationException | LinkageError ex) { warn("eraser-resolve", ex); }
    }

    private static Field field(Class<?> owner, String preferred, Class<?> wanted) throws ReflectiveOperationException {
        for (Field field : owner.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && wanted.isAssignableFrom(field.getType()) && (field.getName().equals(preferred) || preferred.isEmpty())) {
                field.setAccessible(true); return field;
            }
        }
        for (Field field : owner.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && wanted.isAssignableFrom(field.getType())) { field.setAccessible(true); return field; }
        }
        throw new NoSuchFieldException(preferred);
    }

    private static Object staticValue(Field field, String key) {
        if (field == null) return null;
        try { return field.get(null); } catch (ReflectiveOperationException | RuntimeException ex) { warn(key, ex); return null; }
    }

    private static void invoke(Method method, Entity argument, String key) {
        if (method == null) return;
        try { method.invoke(null, argument); } catch (ReflectiveOperationException | RuntimeException ex) { warn(key, ex); }
    }

    private static void warn(String key, Throwable throwable) {
        if (WARNED.add(key)) LiyMod.LOGGER.warn("Optional confrontation hook {} unavailable", key, throwable);
    }

    private static ResourceLocation id(String namespace, String path) { return new ResourceLocation(namespace, path); }
}
