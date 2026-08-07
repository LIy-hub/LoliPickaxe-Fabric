package com.liymod.item;


import com.liymod.LiyMod;
import com.liymod.block.ModBlocks;
import com.liymod.tool.ModToolMaterials;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import static com.liymod.LiyMod.MOD_ID;

public final class ModItems {
    private ModItems() {
    }

    private static Item registerItem(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item item = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static final Item LOLI = registerItem(
            "loli",
            Item::new,
            new Item.Properties().fireResistant()
    );
    public static final Item LOLI_PICKAXE = registerItem(
            "loli_pickaxe",
            LoliPickaxeItem::new,
            new Item.Properties()
                    .fireResistant()
                    .stacksTo(1)
                    .pickaxe(ModToolMaterials.LOLI, Integer.MAX_VALUE, Float.POSITIVE_INFINITY)
                    .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
    );
    public static final Item SMALL_LOLI_PICKAXE = registerItem(
            "small_loli_pickaxe",
            SmallLoliPickaxeItem::new,
            new Item.Properties().fireResistant().stacksTo(1).component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
    );

    public static final Item LOLI_COAL_ADDON = registerUpgrade("loli_coal_addon", UpgradeItem.Type.COAL);
    public static final Item LOLI_IRON_ADDON = registerUpgrade("loli_iron_addon", UpgradeItem.Type.IRON);
    public static final Item LOLI_GOLD_ADDON = registerUpgrade("loli_gold_addon", UpgradeItem.Type.GOLD);
    public static final Item LOLI_REDSTONE_ADDON = registerUpgrade("loli_redstone_addon", UpgradeItem.Type.REDSTONE);
    public static final Item LOLI_LAPIS_ADDON = registerUpgrade("loli_lapis_addon", UpgradeItem.Type.LAPIS);
    public static final Item LOLI_DIAMOND_ADDON = registerUpgrade("loli_diamond_addon", UpgradeItem.Type.DIAMOND);
    public static final Item LOLI_EMERALD_ADDON = registerUpgrade("loli_emerald_addon", UpgradeItem.Type.EMERALD);
    public static final Item LOLI_OBSIDIAN_ADDON = registerUpgrade("loli_obsidian_addon", UpgradeItem.Type.OBSIDIAN);
    public static final Item LOLI_GLOW_ADDON = registerUpgrade("loli_glow_addon", UpgradeItem.Type.GLOW);
    public static final Item LOLI_QUARTZ_ADDON = registerUpgrade("loli_quartz_addon", UpgradeItem.Type.QUARTZ);
    public static final Item LOLI_NETHER_STAR_ADDON = registerUpgrade("loli_nether_star_addon", UpgradeItem.Type.NETHER_STAR);
    public static final Item LOLI_AUTO_FURNACE_ADDON = registerUpgrade("loli_auto_furnace_addon", UpgradeItem.Type.AUTO_FURNACE);
    public static final Item LOLI_FLY_ADDON = registerUpgrade("loli_fly_addon", UpgradeItem.Type.FLY);
    public static final Item LOLI_ENTITY_SOUL_ADDON = registerUpgrade("loli_entity_soul_addon", UpgradeItem.Type.ENTITY_SOUL);

    public static final Item LOLI_DISPERSAL = registerItem("loli_dispersal", Item::new, new Item.Properties());
    public static final Item BUG_ENTITY_CLEAR = registerItem("bug_entity_clear", Item::new, new Item.Properties());
    public static final Item LOLI_CARD = registerItem("loli_card", Item::new, new Item.Properties());
    public static final Item LOLI_CARD_ALBUM = registerItem("loli_card_album", Item::new, new Item.Properties());
    public static final Item LOLI_CARD_ONLINE = registerItem("loli_card_online", Item::new, new Item.Properties());
    public static final Item LOLI_RECORD = registerItem("loli_record", Item::new, new Item.Properties());

    public static final Item LOLI_BLUE_SCREEN_TNT = registerBlockItem("loli_blue_screen_tnt", ModBlocks.LOLI_BLUE_SCREEN_TNT);
    public static final Item LOLI_EXIT_TNT = registerBlockItem("loli_exit_tnt", ModBlocks.LOLI_EXIT_TNT);
    public static final Item LOLI_FAIL_RESPOND_TNT = registerBlockItem("loli_fail_respond_tnt", ModBlocks.LOLI_FAIL_RESPOND_TNT);
    public static final Item LOLI_ALTAR = registerBlockItem("loli_altar", ModBlocks.LOLI_ALTAR);
    public static final Item PASSWORD_WORK_BENCH = registerBlockItem("password_work_bench", ModBlocks.PASSWORD_WORK_BENCH);

    public static final List<Item> CREATIVE_TAB_ITEMS = List.of(
            LOLI_PICKAXE,
            LOLI,
            SMALL_LOLI_PICKAXE,
            LOLI_COAL_ADDON,
            LOLI_IRON_ADDON,
            LOLI_GOLD_ADDON,
            LOLI_REDSTONE_ADDON,
            LOLI_LAPIS_ADDON,
            LOLI_DIAMOND_ADDON,
            LOLI_EMERALD_ADDON,
            LOLI_OBSIDIAN_ADDON,
            LOLI_GLOW_ADDON,
            LOLI_QUARTZ_ADDON,
            LOLI_NETHER_STAR_ADDON,
            LOLI_AUTO_FURNACE_ADDON,
            LOLI_FLY_ADDON,
            LOLI_ENTITY_SOUL_ADDON,
            LOLI_DISPERSAL,
            BUG_ENTITY_CLEAR,
            LOLI_CARD,
            LOLI_CARD_ALBUM,
            LOLI_CARD_ONLINE,
            LOLI_RECORD,
            LOLI_BLUE_SCREEN_TNT,
            LOLI_EXIT_TNT,
            LOLI_FAIL_RESPOND_TNT,
            LOLI_ALTAR,
            PASSWORD_WORK_BENCH
    );

    private static Item registerBlockItem(String name, Block block) {
        return registerItem(
                name,
                properties -> new BlockItem(block, properties.useBlockDescriptionPrefix()),
                new Item.Properties()
        );
    }

    private static Item registerUpgrade(String name, UpgradeItem.Type type) {
        return registerItem(name, properties -> new UpgradeItem(type, properties), new Item.Properties());
    }

    public static void addCreativeTabItems(CreativeModeTab.Output entries) {
        for (Item item : CREATIVE_TAB_ITEMS) {
            if (item instanceof UpgradeItem upgrade) {
                for (int tier = 0; tier < upgrade.tierCount(); tier++) {
                    entries.accept(upgrade.createStack(tier));
                }
            } else {
                entries.accept(item);
                if (item instanceof SmallLoliPickaxeItem) {
                    entries.accept(SmallLoliPickaxeItem.createFullyUpgraded(item));
                }
            }
        }
    }

    public static void registerModItems() {
        LiyMod.LOGGER.info("Registering items for {}", MOD_ID);
    }
}
