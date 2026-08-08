package com.liymod.network;

import com.liymod.LiyMod;
import com.liymod.menu.StorageMenu;
import com.liymod.menu.PasswordWorkbenchMenu;
import com.liymod.menu.FinalToolMenu;
import com.liymod.menu.ModMenus;
import com.liymod.config.FinalToolSettings;
import com.liymod.config.LogicalEnchantments;
import com.liymod.storage.LoliStorageData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.liymod.item.LoliCardData;
import com.liymod.registry.ModContent;
import com.liymod.config.LoliServerConfig;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(LiyMod.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ModNetwork() { }

    public static void register() {
        CHANNEL.registerMessage(0, StorageActionPacket.class, StorageActionPacket::encode, StorageActionPacket::decode, StorageActionPacket::handle);
        CHANNEL.registerMessage(1, PasswordUpdatePacket.class, PasswordUpdatePacket::encode, PasswordUpdatePacket::decode, PasswordUpdatePacket::handle);
        CHANNEL.registerMessage(2, OpenFinalMenuPacket.class, OpenFinalMenuPacket::encode, OpenFinalMenuPacket::decode, OpenFinalMenuPacket::handle);
        CHANNEL.registerMessage(3, SettingPacket.class, SettingPacket::encode, SettingPacket::decode, SettingPacket::handle);
        CHANNEL.registerMessage(4, EnchantmentPacket.class, EnchantmentPacket::encode, EnchantmentPacket::decode, EnchantmentPacket::handle);
        CHANNEL.registerMessage(5, EffectPacket.class, EffectPacket::encode, EffectPacket::decode, EffectPacket::handle);
        CHANNEL.registerMessage(6, TeleportPacket.class, TeleportPacket::encode, TeleportPacket::decode, TeleportPacket::handle);
        CHANNEL.registerMessage(7, CardOpenPacket.class, CardOpenPacket::encode, CardOpenPacket::decode, CardOpenPacket::handle);
        CHANNEL.registerMessage(8, CardUpdatePacket.class, CardUpdatePacket::encode, CardUpdatePacket::decode, CardUpdatePacket::handle);
        CHANNEL.registerMessage(9, BlacklistPacket.class, BlacklistPacket::encode, BlacklistPacket::decode, BlacklistPacket::handle);
        CHANNEL.registerMessage(10, RangeMiningPacket.class, RangeMiningPacket::encode, RangeMiningPacket::decode, RangeMiningPacket::handle);
    }

    public static String sanitizePassword(String input) {
        if (input == null) return "";
        String value = input.codePoints().limit(64).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
        while (value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 256 && !value.isEmpty()) value = value.substring(0, value.offsetByCodePoints(0, value.codePointCount(0, value.length()) - 1));
        return value;
    }

    public record PasswordUpdatePacket(String password) {
        static void encode(PasswordUpdatePacket packet, FriendlyByteBuf buf) { buf.writeUtf(sanitizePassword(packet.password), 256); }
        static PasswordUpdatePacket decode(FriendlyByteBuf buf) { return new PasswordUpdatePacket(sanitizePassword(buf.readUtf(256))); }
        static void handle(PasswordUpdatePacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get(); ServerPlayer player = context.getSender();
            context.enqueueWork(() -> { if (player != null && player.containerMenu instanceof PasswordWorkbenchMenu menu) menu.setPassword(packet.password); });
            context.setPacketHandled(true);
        }
    }

    public record StorageActionPacket(Action action) {
        public enum Action { OPEN, DROP_ALL }
        static void encode(StorageActionPacket packet, FriendlyByteBuf buf) { buf.writeEnum(packet.action); }
        static StorageActionPacket decode(FriendlyByteBuf buf) { return new StorageActionPacket(buf.readEnum(Action.class)); }
        static void handle(StorageActionPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            context.enqueueWork(() -> {
                if (player == null) return;
                InteractionHand hand = storageHand(player);
                if (hand == null) return;
                if (packet.action == Action.OPEN) open(player, hand); else dropAll(player, player.getItemInHand(hand));
            });
            context.setPacketHandled(true);
        }
    }

    public record OpenFinalMenuPacket(FinalToolMenu.Mode mode) {
        static void encode(OpenFinalMenuPacket packet, FriendlyByteBuf buf) { buf.writeEnum(packet.mode); }
        static OpenFinalMenuPacket decode(FriendlyByteBuf buf) { return new OpenFinalMenuPacket(buf.readEnum(FinalToolMenu.Mode.class)); }
        static void handle(OpenFinalMenuPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get(); ServerPlayer player = context.getSender();
            context.enqueueWork(() -> { if (player != null) openFinal(player, packet.mode); });
            context.setPacketHandled(true);
        }
    }

    public record SettingPacket(String key, String value) {
        static void encode(SettingPacket packet, FriendlyByteBuf buf) { buf.writeUtf(limit(packet.key, 64), 64); buf.writeUtf(limit(packet.value, 192), 192); }
        static SettingPacket decode(FriendlyByteBuf buf) { return new SettingPacket(buf.readUtf(64), buf.readUtf(192)); }
        static void handle(SettingPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get(); ServerPlayer player = context.getSender();
            context.enqueueWork(() -> { if (valid(player, FinalToolMenu.Mode.CONFIG)) FinalToolSettings.set(((FinalToolMenu) player.containerMenu).tool(), packet.key, packet.value); });
            context.setPacketHandled(true);
        }
    }

    public record EnchantmentPacket(String id, int level) {
        static void encode(EnchantmentPacket packet, FriendlyByteBuf buf) { buf.writeUtf(limit(packet.id, 128), 128); buf.writeVarInt(packet.level); }
        static EnchantmentPacket decode(FriendlyByteBuf buf) { return new EnchantmentPacket(buf.readUtf(128), buf.readVarInt()); }
        static void handle(EnchantmentPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get(); ServerPlayer player = context.getSender();
            context.enqueueWork(() -> {
                if (!valid(player, FinalToolMenu.Mode.ENCHANTMENT)) return;
                ResourceLocation id = ResourceLocation.tryParse(packet.id);
                Enchantment enchantment = id == null ? null : BuiltInRegistries.ENCHANTMENT.get(id);
                if (enchantment == null || !BuiltInRegistries.ENCHANTMENT.containsKey(id)) return;
                int level = Math.max(0, Math.min(32768, packet.level));
                ItemStack tool = ((FinalToolMenu) player.containerMenu).tool();
                LogicalEnchantments.setLevel(tool, enchantment, level);
                FinalToolSettings.setMapValue(tool, FinalToolSettings.ENCHANTMENTS, id, level, 32768);
            });
            context.setPacketHandled(true);
        }
    }

    public record EffectPacket(String id, int level) {
        static void encode(EffectPacket packet, FriendlyByteBuf buf) { buf.writeUtf(limit(packet.id, 128), 128); buf.writeVarInt(packet.level); }
        static EffectPacket decode(FriendlyByteBuf buf) { return new EffectPacket(buf.readUtf(128), buf.readVarInt()); }
        static void handle(EffectPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get(); ServerPlayer player = context.getSender();
            context.enqueueWork(() -> {
                if (!valid(player, FinalToolMenu.Mode.EFFECT)) return;
                ResourceLocation id = ResourceLocation.tryParse(packet.id);
                MobEffect effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.get(id);
                if (effect == null || !BuiltInRegistries.MOB_EFFECT.containsKey(id)) return;
                FinalToolSettings.setMapValue(((FinalToolMenu) player.containerMenu).tool(), FinalToolSettings.EFFECTS, id,
                        Math.max(0, Math.min(32, packet.level)), 32);
            });
            context.setPacketHandled(true);
        }
    }

    public record TeleportPacket(String dimension, double x, double y, double z) {
        static void encode(TeleportPacket packet, FriendlyByteBuf buf) { buf.writeUtf(limit(packet.dimension, 128), 128); buf.writeDouble(packet.x); buf.writeDouble(packet.y); buf.writeDouble(packet.z); }
        static TeleportPacket decode(FriendlyByteBuf buf) { return new TeleportPacket(buf.readUtf(128), buf.readDouble(), buf.readDouble(), buf.readDouble()); }
        static void handle(TeleportPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get(); ServerPlayer player = context.getSender();
            context.enqueueWork(() -> teleport(player, packet));
            context.setPacketHandled(true);
        }
    }

    public enum CardMode { CARD, ALBUM, ONLINE_VIEW, ONLINE_CONFIG }
    public record CardOpenPacket(CardMode mode, String value) {
        static void encode(CardOpenPacket packet, FriendlyByteBuf buf) { buf.writeEnum(packet.mode); buf.writeUtf(limit(packet.value, 1024), 1024); }
        static CardOpenPacket decode(FriendlyByteBuf buf) { return new CardOpenPacket(buf.readEnum(CardMode.class), buf.readUtf(1024)); }
        static void handle(CardOpenPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.liymod.client.card.CardClient.open(packet)));
            context.setPacketHandled(true);
        }
    }

    public record CardUpdatePacket(InteractionHand hand, String url) {
        static void encode(CardUpdatePacket packet, FriendlyByteBuf buf) { buf.writeEnum(packet.hand); buf.writeUtf(limit(packet.url, LoliCardData.MAX_URL_LENGTH), LoliCardData.MAX_URL_LENGTH); }
        static CardUpdatePacket decode(FriendlyByteBuf buf) { return new CardUpdatePacket(buf.readEnum(InteractionHand.class), buf.readUtf(LoliCardData.MAX_URL_LENGTH)); }
        static void handle(CardUpdatePacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get(); ServerPlayer player = context.getSender();
            context.enqueueWork(() -> { if (player != null && player.getItemInHand(packet.hand).is(ModContent.LOLI_CARD_ONLINE.get())) LoliCardData.url(player.getItemInHand(packet.hand), packet.url); });
            context.setPacketHandled(true);
        }
    }

    public static void sendCard(ServerPlayer player, CardOpenPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /** One bounded S2C transaction for the complete visual result of a range mine. */
    public record RangeMiningPacket(List<BlockChange> changes) {
        public static final int MAX_BLOCKS = 4096;

        public RangeMiningPacket {
            if (changes.size() > MAX_BLOCKS) throw new IllegalArgumentException("Too many range-mining changes");
            changes = List.copyOf(changes);
        }

        static void encode(RangeMiningPacket packet, FriendlyByteBuf buf) {
            buf.writeVarInt(packet.changes.size());
            for (BlockChange change : packet.changes) {
                buf.writeBlockPos(change.pos);
                buf.writeVarInt(Block.getId(change.state));
            }
        }

        static RangeMiningPacket decode(FriendlyByteBuf buf) {
            int count = buf.readVarInt();
            if (count < 0 || count > MAX_BLOCKS) throw new IllegalArgumentException("Invalid range-mining change count");
            List<BlockChange> changes = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                changes.add(new BlockChange(buf.readBlockPos(), Block.stateById(buf.readVarInt())));
            }
            return new RangeMiningPacket(changes);
        }

        static void handle(RangeMiningPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.liymod.client.ClientBootstrap.applyRangeMining(packet.changes)));
            context.setPacketHandled(true);
        }
    }

    public record BlockChange(BlockPos pos, BlockState state) { }

    public static void sendRangeMining(ServerPlayer initiator, ServerLevel level, List<BlockPos> changedPositions) {
        if (changedPositions.isEmpty()) return;
        List<BlockChange> changes = new ArrayList<>(changedPositions.size());
        for (BlockPos pos : changedPositions) changes.add(new BlockChange(pos.immutable(), level.getBlockState(pos)));
        RangeMiningPacket packet = new RangeMiningPacket(changes);
        // The initiating player may mine from the full 1024-block reach and therefore must
        // receive the transaction even when they are nowhere near the changed chunk. Sending
        // once per player in the same dimension also keeps every possible chunk observer in
        // sync without duplicating the packet for the initiator.
        LinkedHashSet<ServerPlayer> recipients = new LinkedHashSet<>(level.players());
        recipients.add(initiator);
        for (ServerPlayer recipient : recipients) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> recipient), packet);
        }
    }

    public record BlacklistPacket(String id, boolean add) {
        static void encode(BlacklistPacket packet, FriendlyByteBuf buf) { buf.writeUtf(limit(packet.id, 128), 128); buf.writeBoolean(packet.add); }
        static BlacklistPacket decode(FriendlyByteBuf buf) { return new BlacklistPacket(buf.readUtf(128), buf.readBoolean()); }
        static void handle(BlacklistPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get(); ServerPlayer player = context.getSender();
            context.enqueueWork(() -> {
                if (player == null) return; InteractionHand hand = storageHand(player); if (hand == null) return;
                ResourceLocation id = ResourceLocation.tryParse(packet.id); if (id != null) LoliStorageData.setBlacklisted(player.getItemInHand(hand), id, packet.add);
            });
            context.setPacketHandled(true);
        }
    }

    private static void openFinal(ServerPlayer player, FinalToolMenu.Mode mode) {
        InteractionHand hand = FinalToolSettings.isFinal(player.getMainHandItem()) ? InteractionHand.MAIN_HAND
                : FinalToolSettings.isFinal(player.getOffhandItem()) ? InteractionHand.OFF_HAND : null;
        if (hand == null) return;
        var type = switch (mode) {
            case CONFIG -> ModMenus.FINAL_CONFIG;
            case ENCHANTMENT -> ModMenus.FINAL_ENCHANTMENT;
            case EFFECT -> ModMenus.FINAL_EFFECT;
            case TELEPORT -> ModMenus.FINAL_TELEPORT;
        };
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override public Component getDisplayName() { return Component.translatable("container.liymod.loli_" + mode.name().toLowerCase(java.util.Locale.ROOT)); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player ignored) { return new FinalToolMenu(mode, id, inventory, hand); }
        }, buf -> buf.writeEnum(hand));
    }

    private static boolean valid(ServerPlayer player, FinalToolMenu.Mode mode) {
        return player != null && player.containerMenu instanceof FinalToolMenu menu && menu.mode() == mode && menu.stillValid(player);
    }

    private static void teleport(ServerPlayer player, TeleportPacket packet) {
        if (!valid(player, FinalToolMenu.Mode.TELEPORT) || !Double.isFinite(packet.x) || !Double.isFinite(packet.y) || !Double.isFinite(packet.z)) return;
        Vec3 offset = new Vec3(packet.x, packet.y, packet.z);
        double maximum = LoliServerConfig.number("max_teleport_distance");
        if (offset.length() > maximum) offset = offset.normalize().scale(maximum);
        ResourceLocation id = ResourceLocation.tryParse(packet.dimension);
        if (id == null) return;
        for (String blocked : LoliServerConfig.get("dimension_blacklist").split(",", -1)) {
            if (id.toString().equals(blocked.trim())) return;
        }
        ServerLevel target = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        if (target == null) return;
        double x = player.getX() + offset.x, y = player.getY() + offset.y, z = player.getZ() + offset.z;
        net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(x, y, z);
        if (!target.getWorldBorder().isWithinBounds(pos) || y < target.getMinBuildHeight() || y >= target.getMaxBuildHeight() || !target.hasChunkAt(pos)) return;
        if (!target.noCollision(player, player.getBoundingBox().move(x - player.getX(), y - player.getY(), z - player.getZ()))) return;
        player.stopRiding();
        player.teleportTo(target, x, y, z, player.getYRot(), player.getXRot());
        player.fallDistance = 0.0F;
    }

    private static String limit(String value, int maximum) {
        if (value == null) return "";
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }

    private static InteractionHand storageHand(Player player) {
        if (LoliStorageData.supports(player.getMainHandItem())) return InteractionHand.MAIN_HAND;
        if (LoliStorageData.supports(player.getOffhandItem())) return InteractionHand.OFF_HAND;
        return null;
    }

    private static void open(ServerPlayer player, InteractionHand hand) {
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override public Component getDisplayName() { return Component.translatable("container.liymod.storage"); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player ignored) { return new StorageMenu(id, inventory, hand); }
        }, buf -> buf.writeEnum(hand));
    }

    private static void dropAll(ServerPlayer player, ItemStack tool) {
        var items = LoliStorageData.load(tool);
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getY() + 0.5D, player.getZ(), stack.copy());
            entity.setTarget(player.getUUID());
            entity.setPickUpDelay(20);
            LoliStorageData.markEjected(entity, player.level().getGameTime() + 200L);
            player.level().addFreshEntity(entity);
            stack.setCount(0);
        }
        LoliStorageData.save(tool, items);
    }
}
