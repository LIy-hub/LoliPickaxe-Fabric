package com.liymod.registry;

import com.liymod.LiyMod;
import com.liymod.item.LoliToolTier;
import com.liymod.item.FinalLoliPickaxeItem;
import com.liymod.item.SmallLoliPickaxeItem;
import com.liymod.item.UpgradeItem;
import com.liymod.item.BugEntityClearItem;
import com.liymod.item.LoliDispersalItem;
import com.liymod.item.LoliCardItem;
import com.liymod.item.LoliOnlineCardItem;
import com.liymod.entity.LoliEntity;
import com.liymod.entity.SafePrimedTntEntity;
import com.liymod.block.LoliAltarBlock;
import com.liymod.block.SafeTntBlock;
import com.liymod.block.PasswordWorkbenchBlock;
import com.liymod.safe.SafeEffect;
import com.liymod.enchantment.AutoFurnaceEnchantment;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModContent {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, LiyMod.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LiyMod.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, LiyMod.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LiyMod.MOD_ID);
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, LiyMod.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LiyMod.MOD_ID);

    public static final RegistryObject<Block> LOLI_BLUE_SCREEN_TNT = block("loli_blue_screen_tnt", () -> new SafeTntBlock(SafeEffect.BLUE_SCREEN, BlockBehaviour.Properties.copy(Blocks.TNT)));
    public static final RegistryObject<Block> LOLI_EXIT_TNT = block("loli_exit_tnt", () -> new SafeTntBlock(SafeEffect.EXIT, BlockBehaviour.Properties.copy(Blocks.TNT)));
    public static final RegistryObject<Block> LOLI_FAIL_RESPOND_TNT = block("loli_fail_respond_tnt", () -> new SafeTntBlock(SafeEffect.FAIL_RESPOND, BlockBehaviour.Properties.copy(Blocks.TNT)));
    public static final RegistryObject<Block> LOLI_ALTAR = block("loli_altar", () -> new LoliAltarBlock(BlockBehaviour.Properties.of().strength(5.0F, 1200.0F).sound(SoundType.STONE)));
    public static final RegistryObject<Block> PASSWORD_WORK_BENCH = block("password_work_bench", () -> new PasswordWorkbenchBlock(BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD)));

    public static final RegistryObject<Item> LOLI = item("loli", () -> new Item(new Item.Properties().fireResistant()));
    public static final RegistryObject<SoundEvent> LOLI_SUCCESS = sound("lolisuccess");
    public static final RegistryObject<SoundEvent> LOLI_IMMUNITY_FIRST = sound("loli_immunity_first");
    public static final RegistryObject<SoundEvent> LOLI_IMMUNITY_SECOND = sound("loli_immunity_second");
    public static final RegistryObject<SoundEvent> LOLI_RECORD_SOUND = sound("lolirecord");

    public static final RegistryObject<Item> LOLI_PICKAXE = item("loli_pickaxe", () -> new FinalLoliPickaxeItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Enchantment> LOLI_AUTO_FURNACE = ENCHANTMENTS.register("loli_auto_furnace", AutoFurnaceEnchantment::new);
    public static final RegistryObject<Item> SMALL_LOLI_PICKAXE = item("small_loli_pickaxe", () -> new SmallLoliPickaxeItem(new Item.Properties().stacksTo(1).fireResistant()));

    public static final RegistryObject<Item> LOLI_COAL_ADDON = upgrade("loli_coal_addon", UpgradeItem.Type.COAL);
    public static final RegistryObject<Item> LOLI_IRON_ADDON = upgrade("loli_iron_addon", UpgradeItem.Type.IRON);
    public static final RegistryObject<Item> LOLI_GOLD_ADDON = upgrade("loli_gold_addon", UpgradeItem.Type.GOLD);
    public static final RegistryObject<Item> LOLI_REDSTONE_ADDON = upgrade("loli_redstone_addon", UpgradeItem.Type.REDSTONE);
    public static final RegistryObject<Item> LOLI_LAPIS_ADDON = upgrade("loli_lapis_addon", UpgradeItem.Type.LAPIS);
    public static final RegistryObject<Item> LOLI_DIAMOND_ADDON = upgrade("loli_diamond_addon", UpgradeItem.Type.DIAMOND);
    public static final RegistryObject<Item> LOLI_EMERALD_ADDON = upgrade("loli_emerald_addon", UpgradeItem.Type.EMERALD);
    public static final RegistryObject<Item> LOLI_OBSIDIAN_ADDON = upgrade("loli_obsidian_addon", UpgradeItem.Type.OBSIDIAN);
    public static final RegistryObject<Item> LOLI_GLOW_ADDON = upgrade("loli_glow_addon", UpgradeItem.Type.GLOW);
    public static final RegistryObject<Item> LOLI_QUARTZ_ADDON = upgrade("loli_quartz_addon", UpgradeItem.Type.QUARTZ);
    public static final RegistryObject<Item> LOLI_NETHER_STAR_ADDON = upgrade("loli_nether_star_addon", UpgradeItem.Type.NETHER_STAR);
    public static final RegistryObject<Item> LOLI_AUTO_FURNACE_ADDON = upgrade("loli_auto_furnace_addon", UpgradeItem.Type.AUTO_FURNACE);
    public static final RegistryObject<Item> LOLI_FLY_ADDON = upgrade("loli_fly_addon", UpgradeItem.Type.FLY);
    public static final RegistryObject<Item> LOLI_ENTITY_SOUL_ADDON = upgrade("loli_entity_soul_addon", UpgradeItem.Type.ENTITY_SOUL);

    public static final RegistryObject<EntityType<LoliEntity>> LOLI_ENTITY = ENTITIES.register("loli", () -> EntityType.Builder.of(LoliEntity::new, MobCategory.MISC).sized(0.6F, 1.5F).clientTrackingRange(64).updateInterval(1).build("liymod:loli"));
    public static final RegistryObject<EntityType<SafePrimedTntEntity>> SAFE_PRIMED_TNT = ENTITIES.register("loli_buff_attack_tnt", () -> EntityType.Builder.<SafePrimedTntEntity>of(SafePrimedTntEntity::new, MobCategory.MISC).sized(0.98F, 0.98F).clientTrackingRange(10).updateInterval(10).build("liymod:loli_buff_attack_tnt"));

    public static final RegistryObject<Item> LOLI_DISPERSAL = item("loli_dispersal", () -> new LoliDispersalItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BUG_ENTITY_CLEAR = item("bug_entity_clear", () -> new BugEntityClearItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> LOLI_CARD = item("loli_card", () -> new LoliCardItem(false, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> LOLI_CARD_ALBUM = item("loli_card_album", () -> new LoliCardItem(true, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> LOLI_CARD_ONLINE = item("loli_card_online", () -> new LoliOnlineCardItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> LOLI_RECORD = item("loli_record", () -> new RecordItem(15, LOLI_RECORD_SOUND, new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE), 1243));

    public static final RegistryObject<Item> LOLI_BLUE_SCREEN_TNT_ITEM = blockItem("loli_blue_screen_tnt", LOLI_BLUE_SCREEN_TNT);
    public static final RegistryObject<Item> LOLI_EXIT_TNT_ITEM = blockItem("loli_exit_tnt", LOLI_EXIT_TNT);
    public static final RegistryObject<Item> LOLI_FAIL_RESPOND_TNT_ITEM = blockItem("loli_fail_respond_tnt", LOLI_FAIL_RESPOND_TNT);
    public static final RegistryObject<Item> LOLI_ALTAR_ITEM = blockItem("loli_altar", LOLI_ALTAR);
    public static final RegistryObject<Item> PASSWORD_WORK_BENCH_ITEM = blockItem("password_work_bench", PASSWORD_WORK_BENCH);

    public static final List<RegistryObject<Item>> DISPLAY_ITEMS = List.of(
            LOLI_PICKAXE, LOLI, SMALL_LOLI_PICKAXE,
            LOLI_COAL_ADDON, LOLI_IRON_ADDON, LOLI_GOLD_ADDON, LOLI_REDSTONE_ADDON,
            LOLI_LAPIS_ADDON, LOLI_DIAMOND_ADDON, LOLI_EMERALD_ADDON, LOLI_OBSIDIAN_ADDON,
            LOLI_GLOW_ADDON, LOLI_QUARTZ_ADDON, LOLI_NETHER_STAR_ADDON, LOLI_AUTO_FURNACE_ADDON,
            LOLI_FLY_ADDON, LOLI_ENTITY_SOUL_ADDON, LOLI_DISPERSAL, BUG_ENTITY_CLEAR,
            LOLI_CARD, LOLI_CARD_ALBUM, LOLI_CARD_ONLINE, LOLI_RECORD,
            LOLI_BLUE_SCREEN_TNT_ITEM, LOLI_EXIT_TNT_ITEM, LOLI_FAIL_RESPOND_TNT_ITEM,
            LOLI_ALTAR_ITEM, PASSWORD_WORK_BENCH_ITEM
    );

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.liymod.main"))
            .icon(() -> new ItemStack(LOLI_PICKAXE.get()))
            .displayItems((parameters, output) -> {
                for (RegistryObject<Item> entry : DISPLAY_ITEMS) {
                    Item item = entry.get();
                    if (item instanceof UpgradeItem upgrade) {
                        for (int tier = 0; tier < upgrade.type().tierCount(); tier++) output.accept(upgrade.createStack(tier));
                    } else {
                        output.accept(item);
                        if (item instanceof SmallLoliPickaxeItem) output.accept(SmallLoliPickaxeItem.fullyUpgraded(item));
                    }
                }
            })
            .build());

    private ModContent() { }

    private static RegistryObject<Block> block(String id, Supplier<Block> factory) {
        return BLOCKS.register(id, factory);
    }

    private static RegistryObject<Item> item(String id, Supplier<Item> factory) {
        return ITEMS.register(id, factory);
    }

    private static RegistryObject<Item> simple(String id) {
        return item(id, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Item> upgrade(String id, UpgradeItem.Type type) {
        return item(id, () -> new UpgradeItem(type, new Item.Properties()));
    }

    private static RegistryObject<SoundEvent> sound(String id) {
        ResourceLocation location = new ResourceLocation(LiyMod.MOD_ID, id);
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(location));
    }

    private static RegistryObject<Item> single(String id) {
        return item(id, () -> new Item(new Item.Properties().stacksTo(1)));
    }

    private static RegistryObject<Item> blockItem(String id, RegistryObject<Block> block) {
        return item(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        SOUNDS.register(bus);
        ENTITIES.register(bus);
        ENCHANTMENTS.register(bus);
        TABS.register(bus);
    }
}
