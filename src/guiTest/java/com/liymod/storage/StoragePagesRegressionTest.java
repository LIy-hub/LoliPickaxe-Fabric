package com.liymod.storage;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import com.liymod.item.LoliFinalEffects;
import com.liymod.compat.DataComponents;
import com.liymod.compat.CustomData;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Exercises real ItemStacks and storage NBT, including sparse legacy pages and page-count shrinkage. */
public final class StoragePagesRegressionTest {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
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
        verifyIndexedInsertion();
        verifyLargeStorageNetwork();
        System.out.println("STORAGE_PAGES_OK empty partial full growth shrink sparse reopen capacity dropAll batch nested exceptionalExit indexedInsertion inPlaceUpdates cacheInvalidation copyIsolation clientMenuNoDecode network100Pages boundedDecode=PASS");
    }

    private static void verifyIndexedInsertion() throws Exception {
        ItemStack owner = new ItemStack(Items.NETHERITE_PICKAXE);
        LoliStorageData storage = fixture(owner);
        ItemStack variant = new ItemStack(Items.STONE, 62);
        CompoundTag variantData = new CompoundTag();
        variantData.putInt("Variant", 1);
        com.liymod.compat.LegacyComponents.set(variant, DataComponents.CUSTOM_DATA, CustomData.of(variantData));
        try (var batch = storage.beginBatch()) {
            storage.setItem(0, new ItemStack(Items.STONE, 63));
            storage.setItem(1, variant);
            storage.setCurrentPage(99);
            storage.setItem(79, new ItemStack(Items.STONE, 10));
            require(storage.insert(new ItemStack(Items.STONE, 64)).isEmpty(), "Indexed insertion lost ordinary items");
        }
        require(storage.getItem(79).getCount() == 64, "Far partial stacks must merge before earlier empty slots");
        storage.setCurrentPage(0);
        require(storage.getItem(0).getCount() == 64 && storage.getItem(1).getCount() == 62
                        && storage.getItem(2).getCount() == 9,
                "Indexing must preserve component variants and the original merge/empty-slot order");
        storage.removeItemNoUpdate(0);
        require(storage.insert(new ItemStack(Items.STONE, 63)).isEmpty()
                        && storage.getItem(0).getCount() == 8 && storage.getItem(2).getCount() == 64,
                "Removed slots must immediately re-enter the empty-slot index");
        storage.getItem(0).setCount(61); // Reproduce a vanilla menu's in-place transfer.
        storage.setChanged();
        require(storage.insert(new ItemStack(Items.STONE, 5)).isEmpty()
                        && storage.getItem(0).getCount() == 64 && storage.getItem(3).getCount() == 2,
                "Menu mutations must update both partial-slot and saved-entry caches");
        storage.getItem(1).setCount(0);
        storage.setChanged();
        require(storage.insert(new ItemStack(Items.DIRT, 2)).isEmpty() && storage.getItem(1).is(Items.DIRT),
                "An in-place emptied slot remained missing from the index");
        LoliStorageData reopened = fixture(owner.copy());
        require(reopened.getItem(0).getCount() == 64 && reopened.getItem(1).is(Items.DIRT)
                        && reopened.getItem(3).getCount() == 2,
                "Cached encoding saved stale counts or stale component variants");
    }

    private static void verifyCachedMenu(ItemStack fullOwner) throws Exception {
        ItemStack owner = fullOwner.copy();
        long coldStart = System.nanoTime();
        LoliStorageData cached = LoliStorageData.cached(owner, 100);
        long coldNanos = System.nanoTime() - coldStart;
        long warmStart = System.nanoTime();
        for (int attempt = 0; attempt < 100; attempt++) {
            require(LoliStorageData.cached(owner, 100) == cached, "An unchanged stack was decoded again");
        }
        long warmNanos = System.nanoTime() - warmStart;
        CompoundTag original = owner.getTag();
        long menuStart = System.nanoTime();
        LoliStorageData menu = LoliStorageData.clientMenu(owner, 100);
        long menuNanos = System.nanoTime() - menuStart;
        require(menu.isEmpty(), "Opening a client menu must not decode the owner's 100 saved pages");
        menu.setCurrentPageFromNetwork(99);
        menu.setItem(80, new ItemStack(Items.DIAMOND, 7));
        menu.setBlacklistItem(0, new ItemStack(Items.GOLD_INGOT));
        menu.getItem(80).shrink(2);
        menu.setChanged();
        require(menu.getItem(80).getCount() == 5 && owner.getTag() == original,
                "Receiving or predicting client slots must never rewrite the owner's authoritative snapshot");
        ItemStack oldSlot = cached.getItem(80);
        CustomData.update(DataComponents.CUSTOM_DATA, owner, root -> root.putString("OtherSetting", "preserved"));
        require(LoliStorageData.cached(owner, 100) == cached && cached.getItem(80) == oldSlot,
                "Unrelated setting changes must preserve the decoded storage and slot objects");
        LoliFinalEffects.set(owner, Map.of(new ResourceLocation("night_vision"), 1));
        require(LoliFinalEffects.get(owner).get(new ResourceLocation("night_vision")) == 1,
                "A large storage must not disable bounded effect settings");
        require(LoliStorageData.cached(owner, 100) == cached && cached.getItem(80) == oldSlot,
                "Effect changes unnecessarily decoded every storage page");
        CompoundTag liveStorage = owner.getTag().getCompound("LoliStorage");
        liveStorage.putInt("CurrentPage", 1);
        require(LoliStorageData.cached(owner, 100) == cached && cached.getCurrentPage() == 1,
                "Legacy in-place storage mutation must invalidate the cache");
        cached.setCurrentPage(99);
        ItemStack copy = owner.copy();
        LoliStorageData copied = LoliStorageData.cached(copy, 100);
        require(copied != cached, "Copied stacks must not share mutable decoded storage");
        copied.setCurrentPage(0);
        copied.removeItemNoUpdate(0);
        copied.setItem(1, new ItemStack(Items.DIRT, 3));
        cached.setCurrentPageFromNetwork(0);
        require(cached.getItem(0).getCount() == 64 && cached.getItem(1).is(Items.STONE),
                "Mutating a copied pickaxe changed the original cache");
        owner.setTag(copy.getTag().copy());
        require(LoliStorageData.cached(owner, 100) == cached && cached.getItem(0).isEmpty()
                        && cached.getItem(1).is(Items.DIRT) && cached.getItem(1).getCount() == 3,
                "Externally replaced storage must refresh the existing menu's shared cache");
        require(cached.insert(new ItemStack(Items.STONE, 64)).isEmpty() && cached.getItem(0).getCount() == 64,
                "Cache invalidation did not rebuild insertion indexes");
        long fullStart = System.nanoTime();
        require(cached.insert(new ItemStack(Items.DIRT, 1)).isEmpty(), "Partial full-storage insertion lost an item");
        long fullNanos = System.nanoTime() - fullStart;
        System.out.printf("STORAGE_OPEN_SAMPLE slots=8100 coldDecodeMs=%.3f cached100OpensMs=%.3f clientMenuCreateMs=%.3f indexedFullInsertMs=%.3f%n",
                coldNanos / 1_000_000.0D, warmNanos / 1_000_000.0D, menuNanos / 1_000_000.0D,
                fullNanos / 1_000_000.0D);
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
        CompoundTag original = owner.getTag();
        require(original.sizeInBytes() > 2 * 1024 * 1024,
                "Full storage must reproduce the native network NBT quota failure");
        var vanillaBuffer = new FriendlyByteBuf(Unpooled.buffer());
        boolean nativeQuotaFailed = false;
        try {
            vanillaBuffer.writeNbt(original);
            vanillaBuffer.readNbt();
        } catch (RuntimeException expected) {
            nativeQuotaFailed = true;
        } finally {
            vanillaBuffer.release();
        }
        require(nativeQuotaFailed, "Vanilla decoding unexpectedly accepted oversized storage");
        for (int direction = 0; direction < 2; direction++) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                buffer.writeNbt(LoliStorageNetworkCodec.compact(original));
                int wireBytes = buffer.readableBytes();
                CompoundTag decoded = LoliStorageNetworkCodec.expand(buffer.readNbt());
                require(decoded.equals(original) && buffer.readableBytes() == 0,
                        "Network compaction must restore every slot, count, blacklist and unrelated key");
                ItemStack received = owner.copy();
                received.setTag(decoded.copy());
                LoliStorageData reopened = fixture(received);
                require(reopened.getVisiblePageCount() == 100 && reopened.getCurrentPage() == 99
                                && reopened.getItem(80).is(Items.DIAMOND) && reopened.getItem(80).getCount() == 7
                                && reopened.getBlacklistItem(0).is(Items.GOLD_INGOT),
                        "Full storage was corrupted by network decoding");
                reopened.setCurrentPageFromNetwork(0);
                require(reopened.getItem(0).is(Items.STONE) && reopened.getItem(0).getCount() == 64,
                        "Network compaction lost the first page");
                System.out.printf("STORAGE_NETWORK_SAMPLE slots=8100 nbtAccountedBytes=%d wireBytes=%d%n",
                        original.sizeInBytes(), wireBytes);
            } finally {
                buffer.release();
            }
        }
        require(owner.getTag() == original,
                "Network encoding must not migrate or mutate saved storage");
        verifyCachedMenu(owner);
        CompoundTag unrelated = new CompoundTag();
        unrelated.putString("OtherMod", "unchanged");
        CompoundTag ordinary = unrelated;
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
            LoliStorageNetworkCodec.expand(root);
        } catch (DecoderException expected) {
            excessiveRejected = true;
        }
        require(excessiveRejected, "Compressed payloads must retain the 4 MiB decoded storage budget");
        packed.putByteArray("Compressed", new byte[]{1, 2, 3});
        boolean malformedRejected = false;
        try {
            LoliStorageNetworkCodec.expand(root);
        } catch (DecoderException expected) {
            malformedRejected = true;
        }
        require(malformedRejected, "Malformed compressed storage must fail without silently losing items");
    }

    private static void verifyBatchInsertion() throws ReflectiveOperationException {
        ItemStack batchedOwner = new ItemStack(Items.NETHERITE_PICKAXE);
        LoliStorageData batched = fixture(batchedOwner);
        batched.setBlacklistItem(0, new ItemStack(Items.DIAMOND));
        var before = batchedOwner.getTag();
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
                require(batchedOwner.getTag() == before,
                        "Mining must not serialize the complete storage for each block");
            }
            try (var nested = batched.beginBatch()) {
                require(batched.insert(new ItemStack(Items.DIAMOND, 3)).getCount() == 3,
                        "Batching must preserve blacklisted remainders");
                require(batched.insert(new ItemStack(Items.DIRT, 2)).isEmpty(), "Nested batch lost a drop");
            }
            require(batchedOwner.getTag() == before,
                    "Closing an inner batch must not encode before the outer mining action completes");
            require(batched.getItem(0).getCount() == 64 && batched.getItem(3).getCount() == 64,
                    "Accepted batched drops must already obey ordinary stack limits");
        }
        long batchNanos = System.nanoTime() - batchStart;
        outer.close(); // Resource cleanup is safe to repeat.
        LoliStorageData ordinary = fixture(ordinaryOwner);
        ordinary.insert(new ItemStack(Items.DIRT, 2));
        require(batchedOwner.getTag().equals(ordinaryOwner.getTag()),
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
