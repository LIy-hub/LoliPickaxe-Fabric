package com.liymod.block;

import com.liymod.LiyMod;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import static com.liymod.LiyMod.MOD_ID;

public final class ModBlocks {
    public static final Block LOLI_BLUE_SCREEN_TNT = registerBlock(
            "loli_blue_screen_tnt",
            Block::new,
            BlockBehaviour.Properties.of().strength(0.0F).sound(SoundType.GRASS)
    );
    public static final Block LOLI_EXIT_TNT = registerBlock(
            "loli_exit_tnt",
            Block::new,
            BlockBehaviour.Properties.of().strength(0.0F).sound(SoundType.GRASS)
    );
    public static final Block LOLI_FAIL_RESPOND_TNT = registerBlock(
            "loli_fail_respond_tnt",
            Block::new,
            BlockBehaviour.Properties.of().strength(0.0F).sound(SoundType.GRASS)
    );
    public static final Block LOLI_ALTAR = registerBlock(
            "loli_altar",
            Block::new,
            BlockBehaviour.Properties.of().strength(5.0F, 1_200.0F).sound(SoundType.STONE)
    );
    public static final Block PASSWORD_WORK_BENCH = registerBlock(
            "password_work_bench",
            Block::new,
            BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD)
    );

    private ModBlocks() {
    }

    private static Block registerBlock(
            String name,
            Function<BlockBehaviour.Properties, Block> factory,
            BlockBehaviour.Properties properties) {
        Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, name);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }

    public static void registerModBlocks() {
        LiyMod.LOGGER.info("Registering blocks for {}", MOD_ID);
    }
}
