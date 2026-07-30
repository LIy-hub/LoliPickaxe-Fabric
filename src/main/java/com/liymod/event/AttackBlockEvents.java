package com.liymod.event;

import com.liymod.LiyMod;
import com.liymod.item.LoliPickaxeItem;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import java.util.Map;

import static com.liymod.LiyMod.MOD_ID;
import static net.minecraft.world.level.block.Blocks.*;

public final class AttackBlockEvents {
    private static final Map<Block, Item> SPECIAL_DROPS = Map.ofEntries(
            Map.entry(SPAWNER, Items.SPAWNER),
            Map.entry(STRUCTURE_BLOCK, Items.STRUCTURE_BLOCK),
            Map.entry(JIGSAW, Items.JIGSAW),
            Map.entry(END_PORTAL_FRAME, Items.END_PORTAL_FRAME),
            Map.entry(COMMAND_BLOCK, Items.COMMAND_BLOCK),
            Map.entry(CHAIN_COMMAND_BLOCK, Items.CHAIN_COMMAND_BLOCK),
            Map.entry(REPEATING_COMMAND_BLOCK, Items.REPEATING_COMMAND_BLOCK),
            Map.entry(BEDROCK, Items.BEDROCK),
            Map.entry(BARRIER, Items.BARRIER),
            Map.entry(COAL_ORE, Items.COAL_BLOCK),
            Map.entry(DEEPSLATE_COAL_ORE, Items.COAL_BLOCK),
            Map.entry(IRON_ORE, Items.IRON_BLOCK),
            Map.entry(DEEPSLATE_IRON_ORE, Items.IRON_BLOCK),
            Map.entry(GOLD_ORE, Items.GOLD_BLOCK),
            Map.entry(DEEPSLATE_GOLD_ORE, Items.GOLD_BLOCK),
            Map.entry(REDSTONE_ORE, Items.REDSTONE_BLOCK),
            Map.entry(DEEPSLATE_REDSTONE_ORE, Items.REDSTONE_BLOCK),
            Map.entry(DIAMOND_ORE, Items.DIAMOND_BLOCK),
            Map.entry(DEEPSLATE_DIAMOND_ORE, Items.DIAMOND_BLOCK),
            Map.entry(EMERALD_ORE, Items.EMERALD_BLOCK),
            Map.entry(DEEPSLATE_EMERALD_ORE, Items.EMERALD_BLOCK),
            Map.entry(LAPIS_ORE, Items.LAPIS_BLOCK),
            Map.entry(DEEPSLATE_LAPIS_ORE, Items.LAPIS_BLOCK),
            Map.entry(COPPER_ORE, Items.COPPER_BLOCK.weathering().unaffected()),
            Map.entry(DEEPSLATE_COPPER_ORE, Items.COPPER_BLOCK.weathering().unaffected()),
            Map.entry(NETHER_QUARTZ_ORE, Items.QUARTZ_BLOCK),
            Map.entry(ANCIENT_DEBRIS, Items.NETHERITE_BLOCK)
    );

    private AttackBlockEvents() {
    }

    private static InteractionResult onAttackBlock(
            Player player,
            Level world,
            InteractionHand hand,
            BlockPos blockPos,
            Direction direction
    ) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof LoliPickaxeItem && !world.isClientSide()) {
            Block block = world.getBlockState(blockPos).getBlock();
            world.destroyBlock(blockPos, true, player);
            world.playSound(
                    player,
                    blockPos,
                    SoundEvents.AMETHYST_BLOCK_BREAK,
                    SoundSource.BLOCKS,
                    1.0f,
                    1.0f);

            Item specialDrop = SPECIAL_DROPS.get(block);
            if (specialDrop != null) {
                spawnItemEntity(world, blockPos, specialDrop);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static void spawnItemEntity(Level world, BlockPos blockPos, Item item) {
        ItemStack stack = new ItemStack(item, 1);
        ItemEntity itemEntity = new ItemEntity(
                world,
                blockPos.getX() + 0.5,
                blockPos.getY() + 0.5,
                blockPos.getZ() + 0.5,
                stack
        );
        world.addFreshEntity(itemEntity);
    }

    public static void registerEvents() {
        LiyMod.LOGGER.info("Registering attack block events for {}", MOD_ID);
        AttackBlockCallback.EVENT.register(AttackBlockEvents::onAttackBlock);
    }
}
