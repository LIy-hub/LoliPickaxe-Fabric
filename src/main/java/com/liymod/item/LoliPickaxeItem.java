package com.liymod.item;

import com.liymod.combat.LoliErasureService;
import net.minecraft.client.item.TooltipType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.UnbreakableComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

public final class LoliPickaxeItem extends PickaxeItem {
    private static final double ABILITY_RANGE = 32.0;

    public LoliPickaxeItem(
            ToolMaterial material,
            int attackDamage,
            float attackSpeed,
            Settings settings
    ) {
        super(
                material,
                settings.attributeModifiers(
                        PickaxeItem.createAttributeModifiers(
                                material,
                                attackDamage,
                                attackSpeed
                        )
                )
        );
    }

    @Override
    public ItemStack getDefaultStack() {
        ItemStack stack = super.getDefaultStack();
        makeUnbreakable(stack);
        return stack;
    }

    @Override
    public void postProcessComponents(ItemStack stack) {
        super.postProcessComponents(stack);
        makeUnbreakable(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        makeUnbreakable(stack);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.pass(stack);
        }

        Box area = user.getBoundingBox().expand(ABILITY_RANGE);
        List<Entity> targets = world.getOtherEntities(
                user,
                area,
                entity -> !(entity instanceof LightningEntity)
        );

        for (Entity target : targets) {
            double x = target.getX();
            double y = target.getY();
            double z = target.getZ();
            if (LoliErasureService.executeAbsolute(user, target)
                    == LoliErasureService.Result.EXECUTED) {
                spawnLightning(world, x, y, z);
            }
        }

        return TypedActionResult.success(stack);
    }

    private static void makeUnbreakable(ItemStack stack) {
        stack.setDamage(0);
        stack.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(true));
    }

    private static void spawnLightning(World world, double x, double y, double z) {
        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
        lightning.setPosition(x, y, z);
        world.spawnEntity(lightning);
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            Item.TooltipContext context,
            List<Text> tooltip,
            TooltipType type
    ) {
        tooltip.add(Text.translatable("liymod.loli_pickaxe.tip").formatted(Formatting.AQUA));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
