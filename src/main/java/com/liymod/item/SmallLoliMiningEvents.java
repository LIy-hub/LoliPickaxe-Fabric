package com.liymod.item;

import com.liymod.LiyMod;
import com.liymod.storage.LoliStorageData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.ListIterator;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** Range mining and drop transformation for the ordinary Small Loli Pickaxe. */
public final class SmallLoliMiningEvents {
    private static final Set<UUID> RANGE_MINING = new HashSet<>();

    private SmallLoliMiningEvents() {
    }

    public static void registerEvents() {
        LiyMod.LOGGER.info("Registering Small Loli Pickaxe mining events for {}", LiyMod.MOD_ID);
        PlayerBlockBreakEvents.AFTER.register(SmallLoliMiningEvents::afterBlockBreak);
        LootTableEvents.MODIFY_DROPS.register(SmallLoliMiningEvents::modifyDrops);
    }

    private static void afterBlockBreak(
            net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player,
            BlockPos origin,
            BlockState brokenState,
            net.minecraft.world.level.block.entity.BlockEntity blockEntity
    ) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack tool = serverPlayer.getMainHandItem();
        if (!(tool.getItem() instanceof SmallLoliPickaxeItem pickaxe)) {
            return;
        }
        int radius = SmallLoliPickaxeItem.getCurrentMiningRadius(tool);
        if (radius <= 0 || !RANGE_MINING.add(serverPlayer.getUUID())) {
            return;
        }

        try {
            SmallLoliPickaxeItem.refreshEnchantments(tool, serverLevel);
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (x == 0 && y == 0 && z == 0) {
                            continue;
                        }
                        BlockPos target = origin.offset(x, y, z);
                        BlockState state = serverLevel.getBlockState(target);
                        if (state.isAir()
                                || state.getDestroySpeed(serverLevel, target) <= 0.0F
                                || !pickaxe.isCorrectToolForDrops(tool, state)
                                || !serverPlayer.mayInteract(serverLevel, target)
                                || serverPlayer.blockActionRestricted(serverLevel, target, serverPlayer.gameMode())) {
                            continue;
                        }
                        serverPlayer.gameMode.destroyBlock(target);
                    }
                }
            }
        } finally {
            RANGE_MINING.remove(serverPlayer.getUUID());
        }
    }

    private static void modifyDrops(
            Holder<LootTable> table,
            LootContext context,
            List<ItemStack> drops
    ) {
        if (!context.hasParameter(LootContextParams.BLOCK_STATE)) {
            return;
        }
        ItemInstance toolInstance = context.getOptionalParameter(LootContextParams.TOOL);
        if (!(toolInstance instanceof ItemStack tool)
                || !(tool.getItem() instanceof SmallLoliPickaxeItem)) {
            return;
        }

        ServerLevel level = context.getLevel();
        if (!SmallLoliPickaxeItem.hasAutoFurnace(tool)) {
            collectDropsIntoStorage(context, drops);
            return;
        }
        List<ItemStack> transformed = new ArrayList<>();
        float experience = 0.0F;
        boolean changed = false;

        for (ItemStack drop : drops) {
            SingleRecipeInput input = new SingleRecipeInput(drop);
            RecipeHolder<SmeltingRecipe> recipe = level.getServer()
                    .getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, input, level)
                    .orElse(null);
            if (recipe == null) {
                transformed.add(drop);
                continue;
            }

            ItemStack result = recipe.value().assemble(input);
            if (result.isEmpty()) {
                transformed.add(drop);
                continue;
            }

            int power = 1;
            int fortune = SmallLoliPickaxeItem.getFortuneLevel(tool);
            if (fortune > 0) {
                power = context.getRandom().nextInt(fortune + 2);
                if (power == 0) {
                    power = 1;
                }
            }
            long resultCount = (long) result.getCount() * drop.getCount() * power;
            appendSplitStacks(transformed, result, resultCount);
            experience += recipe.value().experience() * drop.getCount() * power;
            changed = true;
        }

        if (!changed) {
            collectDropsIntoStorage(context, drops);
            return;
        }
        drops.clear();
        drops.addAll(transformed);

        Vec3 origin = context.getOptionalParameter(LootContextParams.ORIGIN);
        int experiencePoints = randomizedExperience(context, experience);
        if (origin != null && experiencePoints > 0) {
            ExperienceOrb.award(level, origin, experiencePoints);
        }
        collectDropsIntoStorage(context, drops);
    }

    private static void collectDropsIntoStorage(LootContext context, List<ItemStack> drops) {
        Entity source = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (!(source instanceof ServerPlayer player)) {
            return;
        }
        ItemStack actualTool = player.getMainHandItem();
        if (!(actualTool.getItem() instanceof SmallLoliPickaxeItem)
                || !LoliStorageData.hasStorage(actualTool)) {
            return;
        }
        LoliStorageData storage = LoliStorageData.open(actualTool);
        ListIterator<ItemStack> iterator = drops.listIterator();
        while (iterator.hasNext()) {
            ItemStack drop = iterator.next();
            ItemStack remaining = storage.insert(drop);
            if (remaining.isEmpty()) {
                iterator.remove();
            } else if (remaining.getCount() != drop.getCount()) {
                iterator.set(remaining);
            }
        }
    }

    private static void appendSplitStacks(List<ItemStack> output, ItemStack template, long count) {
        int maximum = template.getMaxStackSize();
        while (count > 0L) {
            ItemStack split = template.copy();
            int splitCount = (int) Math.min(maximum, count);
            split.setCount(splitCount);
            output.add(split);
            count -= splitCount;
        }
    }

    private static int randomizedExperience(LootContext context, float experience) {
        int whole = (int) experience;
        float fraction = experience - whole;
        return whole + (context.getRandom().nextFloat() < fraction ? 1 : 0);
    }
}
