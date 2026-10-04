package com.liymod.storage;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Exercises real ItemStacks and storage NBT, including sparse legacy pages and page-count shrinkage. */
public final class StoragePagesRegressionTest {
    public static void main(String[] args) throws ReflectiveOperationException {
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
        System.out.println("STORAGE_PAGES_OK empty partial full growth shrink sparse reopen capacity dropAll=PASS");
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
