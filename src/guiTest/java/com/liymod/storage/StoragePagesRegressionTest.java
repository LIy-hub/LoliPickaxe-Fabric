package com.liymod.storage;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.io.ByteArrayOutputStream;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/** Exercises real ItemStacks and storage NBT, including sparse legacy pages and page-count shrinkage. */
public final class StoragePagesRegressionTest {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // 26.2 binds default item components during data loading, after Bootstrap.
        // Only these isolated fixture items need stack-size defaults for storage/codec checks.
        for (Item fixtureItem : new Item[]{Items.NETHERITE_PICKAXE, Items.STONE, Items.DIRT,
                Items.GOLD_INGOT, Items.DIAMOND}) {
            fixtureItem.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                    .set(DataComponents.MAX_STACK_SIZE, fixtureItem == Items.NETHERITE_PICKAXE ? 1 : 64).build());
        }
        // Vanilla bootstrap has frozen item registration. Exercise the real storage implementation
        // with a bounded owner fixture instead of mutating registries or starting a user's world.
        ItemStack owner = new ItemStack(Items.NETHERITE_PICKAXE);
        LoliStorageData storage = fixture(owner);
        require(storage.getPageCount() == 100 && storage.getVisiblePageCount() == 1, "Empty storage must show one page");
        storage.setItem(0, new ItemStack(Items.STONE, 64));
        require(storage.getVisiblePageCount() == 1, "Item quantity must not be confused with occupied slots");
        for (int slot = 1; slot < 81; slot++) {
            storage.setItem(slot, new ItemStack(Items.DIRT));
        }
        require(storage.getVisiblePageCount() == 2, "Full page must expose the next insertion page");
        storage.setCurrentPage(1);
        storage.setItem(0, new ItemStack(Items.GOLD_INGOT, 3));
        require(storage.getVisiblePageCount() == 2, "Partly filled tail page must not add unused pages");
        storage.removeItem(0, 3);
        require(storage.getVisiblePageCount() == 2, "Keep the insertion page while the previous page is full");
        storage.setCurrentPage(0);
        storage.removeItem(80, 1);
        require(storage.getVisiblePageCount() == 1, "Removing the full-page boundary must shrink the page count");
        storage.setCurrentPage(99);
        storage.setItem(80, new ItemStack(Items.DIAMOND, 7));
        require(storage.getVisiblePageCount() == 100, "Sparse legacy page 100 must remain reachable");
        LoliStorageData reopened = fixture(owner.copy());
        require(reopened.getVisiblePageCount() == 100 && reopened.getCurrentPage() == 99,
                "Page metadata does not survive reopening");
        require(reopened.getItem(80).is(Items.DIAMOND) && reopened.getItem(80).getCount() == 7,
                "Sparse saved items moved or disappeared");
        reopened.removeItem(80, 7);
        require(reopened.getVisiblePageCount() == 1 && reopened.getCurrentPage() == 99,
                "Tail removal must shrink pages without changing the slot mapping mid-transaction");
        reopened.setItem(80, new ItemStack(Items.DIAMOND, 7));
        for (int slot = 0; slot < 81; slot++) {
            reopened.setItem(slot, new ItemStack(Items.DIAMOND));
        }
        require(reopened.getVisiblePageCount() == 100, "Full capacity must never expose page 101");
        reopened.setCurrentPage(0);
        require(reopened.getItem(0).getCount() == 64 && reopened.getItem(79).is(Items.DIRT)
                && reopened.getItem(80).isEmpty(), "Changing pages corrupted the original slots");
        reopened.removeAllStoredItems();
        require(reopened.getVisiblePageCount() == 1 && reopened.isEmpty(), "Drop-all must reset visible pages");
        require(fixture(reopened.getOwnerStack()).getVisiblePageCount() == 1,
                "Empty storage does not survive reopening");
        verifyBatchInsertion();
        verifyLargeStorageNetwork();
        System.out.println("STORAGE_PAGES_OK empty partial full growth shrink sparse reopen capacity dropAll batch nested exceptionalExit network100Pages boundedDecode=PASS");
    }

    private static void verifyLargeStorageNetwork() throws Exception {
        ItemStack owner = new ItemStack(Items.NETHERITE_PICKAXE);
        LoliStorageData storage = fixture(owner);
        try (var batch = storage.beginBatch()) {
            for (int page = 0; page < 100; page++) {
                storage.setCurrentPage(page);
                for (int slot = 0; slot < 81; slot++) {
                    storage.setItem(slot, new ItemStack(Items.STONE, 64));
                }
            }
            storage.setItem(80, new ItemStack(Items.DIAMOND, 7));
            storage.setBlacklistItem(0, new ItemStack(Items.GOLD_INGOT));
        }
        CustomData.update(DataComponents.CUSTOM_DATA, owner, root -> root.putString("OwnerFixture", "unchanged"));
        CustomData original = owner.get(DataComponents.CUSTOM_DATA);
        require(original.copyTag().sizeInBytes() > 2 * 1024 * 1024,
                "Full storage must reproduce the native network NBT quota failure");
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var vanillaCodec = DataComponents.CUSTOM_DATA.streamCodec();
        var vanillaBuffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        boolean nativeQuotaFailed = false;
        try {
            vanillaCodec.encode(vanillaBuffer, original);
            vanillaCodec.decode(vanillaBuffer);
        } catch (RuntimeException expected) {
            nativeQuotaFailed = true;
        } finally {
            vanillaBuffer.release();
        }
        require(nativeQuotaFailed, "Vanilla decoding unexpectedly accepted oversized storage");
        var configured = LoliStorageNetworkCodec.configure(
                DataComponentType.<CustomData>builder().persistent(CustomData.CODEC)).build();
        require(configured.codec() == CustomData.CODEC,
                "The network fix must preserve the native persistent codec");
        var codec = configured.streamCodec();
        for (int direction = 0; direction < 2; direction++) {
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
            try {
                codec.encode(buffer, original);
                int wireBytes = buffer.readableBytes();
                CustomData decoded = codec.decode(buffer);
                require(decoded.equals(original) && buffer.readableBytes() == 0,
                        "Network compaction must restore every slot, count, blacklist and unrelated key");
                ItemStack received = owner.copy();
                received.set(DataComponents.CUSTOM_DATA, decoded);
                LoliStorageData reopened = fixture(received);
                require(reopened.getVisiblePageCount() == 100 && reopened.getCurrentPage() == 99
                                && reopened.getItem(80).is(Items.DIAMOND) && reopened.getItem(80).getCount() == 7
                                && reopened.getBlacklistItem(0).is(Items.GOLD_INGOT),
                        "Full storage was corrupted by network decoding");
                reopened.setCurrentPageFromNetwork(0);
                require(reopened.getItem(0).is(Items.STONE) && reopened.getItem(0).getCount() == 64,
                        "Network compaction lost the first page");
                System.out.printf("STORAGE_NETWORK_SAMPLE slots=8100 nbtAccountedBytes=%d wireBytes=%d%n",
                        original.copyTag().sizeInBytes(), wireBytes);
            } finally {
                buffer.release();
            }
        }
        require(owner.get(DataComponents.CUSTOM_DATA) == original,
                "Network encoding must not migrate or mutate saved storage");
        CompoundTag unrelated = new CompoundTag();
        unrelated.putString("OtherMod", "unchanged");
        CustomData ordinary = CustomData.of(unrelated);
        require(LoliStorageNetworkCodec.compact(ordinary) == ordinary
                        && LoliStorageNetworkCodec.expand(ordinary) == ordinary,
                "Other custom data must retain the native encoding path");

        CompoundTag excessive = new CompoundTag();
        excessive.putByteArray("Oversized", new byte[4 * 1024 * 1024]);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        NbtIo.writeCompressed(excessive, compressed);
        CompoundTag packed = new CompoundTag();
        packed.putInt("NetworkVersion", 1);
        packed.putByteArray("Compressed", compressed.toByteArray());
        CompoundTag root = new CompoundTag();
        root.put("LoliStorage", packed);
        boolean excessiveRejected = false;
        try {
            LoliStorageNetworkCodec.expand(CustomData.of(root));
        } catch (DecoderException expected) {
            excessiveRejected = true;
        }
        require(excessiveRejected, "Compressed payloads must retain the 4 MiB decoded storage budget");
        packed.putByteArray("Compressed", new byte[]{1, 2, 3});
        boolean malformedRejected = false;
        try {
            LoliStorageNetworkCodec.expand(CustomData.of(root));
        } catch (DecoderException expected) {
            malformedRejected = true;
        }
        require(malformedRejected, "Malformed compressed storage must fail without silently losing items");
    }

    private static void verifyBatchInsertion() throws ReflectiveOperationException {
        ItemStack batchedOwner = new ItemStack(Items.NETHERITE_PICKAXE);
        LoliStorageData batched = fixture(batchedOwner);
        batched.setBlacklistItem(0, new ItemStack(Items.DIAMOND));
        var before = batchedOwner.get(DataComponents.CUSTOM_DATA);
        ItemStack ordinaryOwner = batchedOwner.copy();
        long ordinaryStart = System.nanoTime();
        // Reproduce the previous mining path: decode and encode the storage for every block.
        for (int block = 0; block < 256; block++) {
            require(fixture(ordinaryOwner).insert(new ItemStack(Items.STONE)).isEmpty(),
                    "Baseline insertion unexpectedly rejected an ordinary drop");
        }
        long ordinaryNanos = System.nanoTime() - ordinaryStart;
        long batchStart = System.nanoTime();
        var outer = batched.beginBatch();
        try (outer) {
            for (int block = 0; block < 256; block++) {
                require(batched.insert(new ItemStack(Items.STONE)).isEmpty(), "Batched insertion lost a drop");
                require(batchedOwner.get(DataComponents.CUSTOM_DATA) == before,
                        "Mining must not serialize the complete storage for each block");
            }
            try (var nested = batched.beginBatch()) {
                require(batched.insert(new ItemStack(Items.DIAMOND, 3)).getCount() == 3,
                        "Batching must preserve blacklisted remainders");
                require(batched.insert(new ItemStack(Items.DIRT, 2)).isEmpty(), "Nested batch lost a drop");
            }
            require(batchedOwner.get(DataComponents.CUSTOM_DATA) == before,
                    "Closing an inner batch must not encode before the outer mining action completes");
            require(batched.getItem(0).getCount() == 64 && batched.getItem(3).getCount() == 64,
                    "Accepted batched drops must already obey ordinary stack limits");
        }
        long batchNanos = System.nanoTime() - batchStart;
        outer.close(); // Resource cleanup is safe to repeat.
        LoliStorageData ordinary = fixture(ordinaryOwner);
        ordinary.insert(new ItemStack(Items.DIRT, 2));
        require(batchedOwner.get(DataComponents.CUSTOM_DATA).equals(ordinaryOwner.get(DataComponents.CUSTOM_DATA)),
                "Batched persistence must produce exactly the same saved items, order, blacklist and metadata");
        LoliStorageData after = fixture(batchedOwner.copy());
        require(after.getItem(3).getCount() == 64 && after.getItem(4).getCount() == 2,
                "The completed mining batch did not survive reopening");
        boolean failed = false;
        try (var exceptionalBatch = batched.beginBatch()) {
            batched.insert(new ItemStack(Items.GOLD_INGOT, 7));
            throw new IllegalStateException("isolated mining failure fixture");
        } catch (IllegalStateException expected) {
            failed = true;
        }
        require(failed && fixture(batchedOwner.copy()).getItem(5).getCount() == 7,
                "An exceptional exit must save already accepted drops instead of losing them");
        System.out.printf("STORAGE_BATCH_SAMPLE blocks=256 previousDecodeEncodeMs=%.3f batchMs=%.3f%n",
                ordinaryNanos / 1_000_000.0D, batchNanos / 1_000_000.0D);
    }

    private static LoliStorageData fixture(ItemStack owner) throws ReflectiveOperationException {
        var constructor = LoliStorageData.class.getDeclaredConstructor(ItemStack.class, int.class);
        constructor.setAccessible(true);
        return constructor.newInstance(owner, LoliStorageData.FINAL_PAGE_COUNT);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
