package com.liymod.event;

import com.liymod.LiyMod;
import com.liymod.combat.LoliErasureService;
import com.liymod.combat.LoliLegacyExecutionPolicy;
import com.liymod.combat.LoliExecutionManager;
import com.liymod.compat.StrengthConfrontation;
import com.liymod.item.FinalLoliPickaxeItem;
import com.liymod.item.SmallLoliPickaxeItem;
import com.liymod.protection.LoliProtection;
import com.liymod.registry.ModContent;
import com.liymod.storage.LoliStorageData;
import com.liymod.config.FinalToolSettings;
import com.liymod.config.LoliServerConfig;
import com.liymod.command.LoliCommands;
import com.liymod.network.ModNetwork;
import net.minecraftforge.event.RegisterCommandsEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.stats.Stats;

public final class ForgeEvents {
    private static final UUID BLOCK_REACH_ID = UUID.fromString("80bbd27f-9aa4-4a54-a435-a61ea191c062");
    private static final UUID ENTITY_REACH_ID = UUID.fromString("c3cf8792-45e6-4d52-9f79-f3b1edb84914");
    private static final ThreadLocal<Boolean> RANGE_BREAKING = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> REFLECTING = ThreadLocal.withInitial(() -> false);
    private static final Set<UUID> FLIGHT_GRANTED = new HashSet<>();
    private static final Set<UUID> INVULNERABILITY_GRANTED = new HashSet<>();
    private static final Map<Block, Item> SPECIAL_DROPS = Map.ofEntries(
            Map.entry(Blocks.SPAWNER, Items.SPAWNER), Map.entry(Blocks.STRUCTURE_BLOCK, Items.STRUCTURE_BLOCK),
            Map.entry(Blocks.JIGSAW, Items.JIGSAW), Map.entry(Blocks.END_PORTAL_FRAME, Items.END_PORTAL_FRAME),
            Map.entry(Blocks.COMMAND_BLOCK, Items.COMMAND_BLOCK), Map.entry(Blocks.CHAIN_COMMAND_BLOCK, Items.CHAIN_COMMAND_BLOCK),
            Map.entry(Blocks.REPEATING_COMMAND_BLOCK, Items.REPEATING_COMMAND_BLOCK), Map.entry(Blocks.BEDROCK, Items.BEDROCK),
            Map.entry(Blocks.BARRIER, Items.BARRIER), Map.entry(Blocks.COAL_ORE, Items.COAL_BLOCK),
            Map.entry(Blocks.DEEPSLATE_COAL_ORE, Items.COAL_BLOCK), Map.entry(Blocks.IRON_ORE, Items.IRON_BLOCK),
            Map.entry(Blocks.DEEPSLATE_IRON_ORE, Items.IRON_BLOCK), Map.entry(Blocks.GOLD_ORE, Items.GOLD_BLOCK),
            Map.entry(Blocks.DEEPSLATE_GOLD_ORE, Items.GOLD_BLOCK), Map.entry(Blocks.REDSTONE_ORE, Items.REDSTONE_BLOCK),
            Map.entry(Blocks.DEEPSLATE_REDSTONE_ORE, Items.REDSTONE_BLOCK), Map.entry(Blocks.DIAMOND_ORE, Items.DIAMOND_BLOCK),
            Map.entry(Blocks.DEEPSLATE_DIAMOND_ORE, Items.DIAMOND_BLOCK), Map.entry(Blocks.EMERALD_ORE, Items.EMERALD_BLOCK),
            Map.entry(Blocks.DEEPSLATE_EMERALD_ORE, Items.EMERALD_BLOCK), Map.entry(Blocks.LAPIS_ORE, Items.LAPIS_BLOCK),
            Map.entry(Blocks.DEEPSLATE_LAPIS_ORE, Items.LAPIS_BLOCK), Map.entry(Blocks.COPPER_ORE, Items.COPPER_BLOCK),
            Map.entry(Blocks.DEEPSLATE_COPPER_ORE, Items.COPPER_BLOCK), Map.entry(Blocks.NETHER_QUARTZ_ORE, Items.QUARTZ_BLOCK),
            Map.entry(Blocks.ANCIENT_DEBRIS, Items.NETHERITE_BLOCK));

    @SubscribeEvent
    public void attack(AttackEntityEvent event) {
        if (LoliExecutionManager.isDeadLocked(event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        if (!event.getEntity().level().isClientSide && LoliProtection.isMainHandProtected(event.getEntity())) {
            LoliErasureService.executeAbsolute(event.getEntity(), event.getTarget());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void attacked(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player player && LoliProtection.isProtected(player)) {
            event.setCanceled(true);
            LoliProtection.retaliate(player, event.getSource());
            return;
        }
        if (event.getEntity() instanceof Player player && !REFLECTING.get()) {
            ItemStack small = findSmallPickaxe(player);
            if (!small.isEmpty()) {
                if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL) && SmallLoliPickaxeItem.canFly(small)) {
                    event.setCanceled(true);
                    return;
                }
                if (player.getRandom().nextDouble() < SmallLoliPickaxeItem.dodge(small)) {
                    event.setCanceled(true);
                    return;
                }
                net.minecraft.world.entity.Entity attacker = event.getSource().getEntity();
                if (attacker != null && attacker != player && player.getRandom().nextDouble() < SmallLoliPickaxeItem.reflect(small)) {
                    REFLECTING.set(true);
                    try {
                        attacker.hurt(player.damageSources().thorns(player), event.getAmount());
                        player.heal(event.getAmount() * 0.5F);
                    } finally {
                        REFLECTING.set(false);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player && LoliProtection.isProtected(player)) {
            event.setCanceled(true);
            player.setHealth(player.getMaxHealth());
        }
    }

    @SubscribeEvent
    public void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        Player player = event.player;
        boolean protectedNow = LoliProtection.isProtected(player);
        ItemStack small = findSmallPickaxe(player);
        boolean smallFlight = !small.isEmpty() && SmallLoliPickaxeItem.canFly(small);
        updateReach(player, LoliProtection.isMainHandProtected(player));
        if (protectedNow) {
            ItemStack finalTool = LoliProtection.protectingStack(player);
            player.setHealth(player.getMaxHealth());
            player.deathTime = 0;
            player.hurtTime = 0;
            player.invulnerableTime = 0;
            player.fallDistance = 0.0F;
            player.setTicksFrozen(0);
            player.setAirSupply(player.getMaxAirSupply());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0F);
            player.experienceLevel = 142857;
            player.clearFire();
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 260, 4, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 260, 0, false, false));
            grantInvulnerability(player);
            grantFlight(player);
            applyConfiguredEffects(player, finalTool);
            if (player.tickCount % 5 == 0 && FinalToolSettings.autoKill(finalTool)) {
                int range = FinalToolSettings.autoKillRange(finalTool);
                for (Entity target : player.level().getEntities(player, player.getBoundingBox().inflate(range),
                        entity -> entity != player && !LoliProtection.isProtected(entity)
                                && LoliLegacyExecutionPolicy.permitsAutomaticRangeTarget(finalTool, entity))) {
                    LoliErasureService.executeAbsolute(player, target);
                }
            }
        } else if (!small.isEmpty()) {
            int buff = SmallLoliPickaxeItem.buff(small);
            if (buff >= 1) player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false));
            if (buff >= 2) player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 260, 0, false, false));
            if (buff >= 3) {
                player.getFoodData().setFoodLevel(20);
                player.getFoodData().setSaturation(20.0F);
            }
            if (smallFlight) grantFlight(player); else revokeFlight(player);
            revokeInvulnerability(player);
        } else {
            revokeFlight(player);
            revokeInvulnerability(player);
        }
        if (player.tickCount % 5 == 0) absorbNearbyDrops(player);
        if (player.tickCount % 5 == 0) recallOwnedFinalPickaxes(player);
    }

    private static void grantFlight(Player player) {
        if (!player.getAbilities().mayfly) {
            FLIGHT_GRANTED.add(player.getUUID());
            player.getAbilities().mayfly = true;
            ((ServerPlayer) player).onUpdateAbilities();
        }
    }

    private static void revokeFlight(Player player) {
        if (FLIGHT_GRANTED.remove(player.getUUID()) && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            ((ServerPlayer) player).onUpdateAbilities();
        }
    }

    private static void grantInvulnerability(Player player) {
        if (!player.getAbilities().invulnerable) {
            INVULNERABILITY_GRANTED.add(player.getUUID());
            player.getAbilities().invulnerable = true;
            ((ServerPlayer) player).onUpdateAbilities();
        }
    }

    private static void revokeInvulnerability(Player player) {
        if (INVULNERABILITY_GRANTED.remove(player.getUUID()) && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().invulnerable = false;
            ((ServerPlayer) player).onUpdateAbilities();
        }
    }

    private static void recallOwnedFinalPickaxes(Player player) {
        for (ItemEntity entity : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(50.0D), item -> item.getItem().is(ModContent.LOLI_PICKAXE.get()))) {
            UUID owner = FinalLoliPickaxeItem.owner(entity.getItem());
            if (owner == null || !owner.equals(player.getUUID())) continue;
            entity.setTarget(owner); entity.setUnlimitedLifetime(); entity.setInvulnerable(true);
            net.minecraft.world.phys.Vec3 direction = player.position().add(0.0D, 0.75D, 0.0D).subtract(entity.position());
            if (direction.lengthSqr() > 1.0D) entity.setDeltaMovement(direction.normalize().scale(0.6D));
        }
    }

    private static void applyConfiguredEffects(Player player, ItemStack tool) {
        for (var entry : FinalToolSettings.effects(tool).entrySet()) {
            var effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getOptional(entry.getKey()).orElse(null);
            if (effect != null) player.addEffect(new MobEffectInstance(effect, 60, Math.max(0, entry.getValue() - 1), false, false));
        }
    }

    private static void absorbNearbyDrops(Player player) {
        ItemStack storage = findAutoStorage(player);
        if (storage.isEmpty()) return;
        AABB area = player.getBoundingBox().inflate(4.0D);
        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, area, Entity::isAlive)) {
            LoliStorageData.absorb(player, storage, item);
        }
    }

    private static ItemStack findAutoStorage(Player player) {
        ItemStack main = player.getMainHandItem();
        if (LoliStorageData.autoAccept(main)) return main;
        ItemStack off = player.getOffhandItem();
        if (LoliStorageData.autoAccept(off)) return off;
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public void itemToss(ItemTossEvent event) {
        LoliStorageData.markEjected(event.getEntity(), event.getPlayer().level().getGameTime() + 100L);
    }

    @SubscribeEvent public void itemExpire(ItemExpireEvent event) {
        if (event.getEntity().getItem().is(ModContent.LOLI_PICKAXE.get())) { event.getEntity().setUnlimitedLifetime(); event.setCanceled(true); }
    }

    @SubscribeEvent public void itemPickup(EntityItemPickupEvent event) {
        ItemStack stack = event.getItem().getItem(); if (!stack.is(ModContent.LOLI_PICKAXE.get())) return;
        UUID owner = FinalLoliPickaxeItem.owner(stack); if (owner != null && !owner.equals(event.getEntity().getUUID())) event.setCanceled(true);
    }

    private static ItemStack findSmallPickaxe(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModContent.SMALL_LOLI_PICKAXE.get())) return stack;
        }
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public void auxiliaryDrops(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        if (level.random.nextDouble() < LoliServerConfig.number("loli_card_drop_chance")) addDrop(event, new ItemStack(ModContent.LOLI_CARD.get()));
        if (level.random.nextDouble() < LoliServerConfig.number("loli_card_album_drop_chance")) addDrop(event, new ItemStack(ModContent.LOLI_CARD_ALBUM.get()));
        if (event.getEntity() instanceof Creeper && level.random.nextDouble() < LoliServerConfig.number("loli_record_drop_chance")) addDrop(event, new ItemStack(ModContent.LOLI_RECORD.get()));
        if (level.random.nextDouble() < LoliServerConfig.number("entity_soul_drop_chance")) addDrop(event, new ItemStack(ModContent.LOLI_ENTITY_SOUL_ADDON.get()));
    }

    private static void addDrop(LivingDropsEvent event, ItemStack stack) {
        event.getDrops().add(new ItemEntity(event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), stack));
    }

    private static void updateReach(Player player, boolean enabled) {
        updateModifier(player.getAttribute(ForgeMod.BLOCK_REACH.get()), BLOCK_REACH_ID, "Loli block reach", enabled);
        updateModifier(player.getAttribute(ForgeMod.ENTITY_REACH.get()), ENTITY_REACH_ID, "Loli entity reach", enabled);
    }

    private static void updateModifier(AttributeInstance attribute, UUID id, String name, boolean enabled) {
        if (attribute == null) return;
        AttributeModifier existing = attribute.getModifier(id);
        if (existing != null) attribute.removeModifier(id);
        if (enabled) attribute.addTransientModifier(new AttributeModifier(id, name,
                FinalLoliPickaxeItem.REACH - attribute.getValue(), AttributeModifier.Operation.ADDITION));
    }

    @SubscribeEvent
    public void leftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (LoliExecutionManager.isDeadLocked(event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player) || RANGE_BREAKING.get()) return;
        ItemStack tool = player.getMainHandItem();
        int radius;
        if (tool.is(ModContent.LOLI_PICKAXE.get())) radius = FinalToolSettings.radius(tool);
        else if (tool.is(ModContent.SMALL_LOLI_PICKAXE.get())) radius = SmallLoliPickaxeItem.radius(tool);
        else return;
        event.setCanceled(true);
        RANGE_BREAKING.set(true);
        try {
            BlockPos origin = event.getPos();
            List<BlockPos> changedPositions = new ArrayList<>();
            int[] experience = {0};
            for (int x = -radius; x <= radius; x++) for (int y = -radius; y <= radius; y++) for (int z = -radius; z <= radius; z++) {
                BlockPos pos = origin.offset(x, y, z);
                if (!event.getLevel().isLoaded(pos) || event.getLevel().getBlockState(pos).isAir()) continue;
                if (breakTool((ServerLevel) event.getLevel(), player, tool, pos, experience)) changedPositions.add(pos.immutable());
            }
            ServerLevel level = (ServerLevel) event.getLevel();
            ModNetwork.sendRangeMining(player, level, changedPositions);
            if (!changedPositions.isEmpty()) {
                level.playSound(null, origin, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (experience[0] > 0) ExperienceOrb.award(level, net.minecraft.world.phys.Vec3.atCenterOf(origin), experience[0]);
            }
        } finally {
            RANGE_BREAKING.set(false);
        }
    }

    @SubscribeEvent
    public void rightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (LoliExecutionManager.isDeadLocked(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (LoliExecutionManager.isDeadLocked(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void interactEntity(PlayerInteractEvent.EntityInteract event) {
        if (LoliExecutionManager.isDeadLocked(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void interactEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (LoliExecutionManager.isDeadLocked(event.getEntity())) event.setCanceled(true);
    }

    private static boolean breakTool(ServerLevel level, ServerPlayer player, ItemStack tool, BlockPos pos, int[] experience) {
        boolean finalTool = tool.is(ModContent.LOLI_PICKAXE.get());
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !player.mayInteract(level, pos) || player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())
                || (finalTool && FinalToolSettings.stopOnLiquid(tool) && !state.getFluidState().isEmpty())
                || (!finalTool && !((SmallLoliPickaxeItem) tool.getItem()).isCorrectToolForDrops(tool, state))) return false;
        if (!tool.getItem().canAttackBlock(state, level, pos, player) || tool.onBlockStartBreak(pos, player)) return false;
        BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(level, pos, state, player);
        MinecraftForge.EVENT_BUS.post(breakEvent);
        if (breakEvent.isCanceled()) return false;
        int eventExperience = breakEvent.getExpToDrop();
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        boolean creative = player.isCreative();
        boolean canHarvest = !creative && state.canHarvestBlock(level, pos, player);
        List<ItemStack> drops = canHarvest
                ? new ArrayList<>(Block.getDrops(state, level, pos, blockEntity, player, tool))
                : new ArrayList<>();
        if (canHarvest) {
            Item special = SPECIAL_DROPS.get(state.getBlock());
            if (special != null) drops.add(new ItemStack(special));
            if (drops.isEmpty() && finalTool && FinalToolSettings.mandatoryDrop(tool) && state.getBlock().asItem() != Items.AIR)
                drops.add(new ItemStack(state.getBlock().asItem()));
        }
        if (!creative) tool.mineBlock(level, state, pos, player);
        state.getBlock().playerWillDestroy(level, pos, state, player);
        BlockState replacement = level.getFluidState(pos).createLegacyBlock();
        if (!level.setBlock(pos, replacement, Block.UPDATE_NEIGHBORS, 512)) return false;
        state.getBlock().destroy(level, pos, state);
        if (creative) return true;
        if (canHarvest) {
            player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
            player.causeFoodExhaustion(0.005F);
            experience[0] += Math.max(0, eventExperience);
            state.spawnAfterBreak(level, pos, tool, false);
        }
        if (finalTool ? FinalToolSettings.autoFurnace(tool) : SmallLoliPickaxeItem.autoFurnace(tool)) drops = smelt(level, drops);
        boolean autoAccept = LoliStorageData.autoAccept(tool);
        for (ItemStack drop : drops) {
            ItemStack remaining = autoAccept ? LoliStorageData.insert(tool, drop) : drop.copy();
            if (!remaining.isEmpty() && autoAccept) player.getInventory().add(remaining);
            if (!remaining.isEmpty()) {
                ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, remaining);
                entity.setTarget(player.getUUID()); level.addFreshEntity(entity);
            }
        }
        return true;
    }

    private static List<ItemStack> smelt(ServerLevel level, List<ItemStack> drops) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack drop : drops) {
            SimpleContainer input = new SimpleContainer(drop.copy());
            SmeltingRecipe recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level).orElse(null);
            if (recipe == null) { result.add(drop); continue; }
            ItemStack output = recipe.getResultItem(level.registryAccess()).copy();
            if (output.isEmpty()) { result.add(drop); continue; }
            long count = (long) output.getCount() * drop.getCount();
            while (count > 0L) { ItemStack part = output.copy(); int amount = (int) Math.min(part.getMaxStackSize(), count); part.setCount(amount); result.add(part); count -= amount; }
        }
        return result;
    }

    @SubscribeEvent
    public void entityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity item && item.getItem().is(ModContent.LOLI_PICKAXE.get())) {
            UUID owner = FinalLoliPickaxeItem.owner(item.getItem()); if (owner != null) item.setTarget(owner);
            item.setInvulnerable(true); item.setUnlimitedLifetime();
        }
        if (!event.getLevel().isClientSide && StrengthConfrontation.suppressJoin(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            LoliExecutionManager.tick(event.getServer());
            StrengthConfrontation.serverTick(event.getServer());
        }
    }

    @SubscribeEvent
    public void serverStopped(ServerStoppedEvent event) {
        FLIGHT_GRANTED.clear();
        INVULNERABILITY_GRANTED.clear();
        StrengthConfrontation.reset(event.getServer());
        LiyMod.LOGGER.info("Stopped LoliPickaxe confrontation runtime");
    }
    @SubscribeEvent public void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) LoliLegacyExecutionPolicy.applyPersistentPlayerStates(player);
    }
    @SubscribeEvent public void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) LoliLegacyExecutionPolicy.applyPersistentPlayerStates(player);
    }
    @SubscribeEvent public void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FLIGHT_GRANTED.remove(player.getUUID());
            INVULNERABILITY_GRANTED.remove(player.getUUID());
            LoliExecutionManager.completeDisconnect(player);
        }
    }
    @SubscribeEvent public void registerCommands(RegisterCommandsEvent event) { LoliCommands.register(event); }
}
