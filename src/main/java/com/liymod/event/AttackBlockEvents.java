package com.liymod.event;

import com.liymod.LiyMod;
import com.liymod.item.LoliPickaxeItem;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Map;

import static com.liymod.LiyMod.MOD_ID;
import static net.minecraft.block.Blocks.*;

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
            Map.entry(COPPER_ORE, Items.COPPER_BLOCK),
            Map.entry(DEEPSLATE_COPPER_ORE, Items.COPPER_BLOCK),
            Map.entry(NETHER_QUARTZ_ORE, Items.QUARTZ_BLOCK),
            Map.entry(ANCIENT_DEBRIS, Items.NETHERITE_BLOCK)
    );

    private AttackBlockEvents() {
    }

    private static ActionResult onAttackBlock(
            PlayerEntity player,
            World world,
            Hand hand,
            BlockPos blockPos,
            Direction direction
    ) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.getItem() instanceof LoliPickaxeItem && !world.isClient()) {
            Block block = world.getBlockState(blockPos).getBlock();
            world.breakBlock(blockPos, true, player);
            world.playSound(
                    player,
                    blockPos,
                    SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK,
                    SoundCategory.BLOCKS,
                    1.0f,
                    1.0f);

            Item specialDrop = SPECIAL_DROPS.get(block);
            if (specialDrop != null) {
                spawnItemEntity(world, blockPos, specialDrop);
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    private static void spawnItemEntity(World world, BlockPos blockPos, Item item) {
        ItemStack stack = new ItemStack(item, 1);
        ItemEntity itemEntity = new ItemEntity(
                world,
                blockPos.getX() + 0.5,
                blockPos.getY() + 0.5,
                blockPos.getZ() + 0.5,
                stack
        );
        world.spawnEntity(itemEntity);
    }

    public static void registerEvents() {
        LiyMod.LOGGER.info("Registering attack block events for {}", MOD_ID);
        AttackBlockCallback.EVENT.register(AttackBlockEvents::onAttackBlock);
    }
}
