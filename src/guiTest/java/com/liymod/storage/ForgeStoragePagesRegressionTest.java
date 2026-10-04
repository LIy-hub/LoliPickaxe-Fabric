package com.liymod.storage;

import java.util.BitSet;

/** Exercises the production page calculation without requiring Forge's class transformers. */
public final class ForgeStoragePagesRegressionTest {
    public static void main(String[] args) {
        BitSet occupied = new BitSet(8100);
        require(count(occupied,100)==1,"Empty storage");
        occupied.set(0);
        require(count(occupied,100)==1,"One occupied slot");
        occupied.set(0,81);
        require(count(occupied,100)==2,"Full tail exposes an insertion page");
        occupied.set(81);
        require(count(occupied,100)==2,"Partial tail");
        occupied.clear(80,82);
        require(count(occupied,100)==1,"Tail shrink");
        occupied.set(8099);
        require(count(occupied,100)==100,"Sparse legacy slots remain reachable");
        occupied.set(8019,8100);
        require(count(occupied,100)==100,"Never expose page 101");
        occupied.clear();
        require(count(occupied,100)==1,"Drop-all reset");
        occupied.set(0,162);
        require(count(occupied,2)==2,"Small tool capacity");
        require(StoragePages.visibleCount(1,81,100,slot->slot==0)==1,"Short loaded list");
        System.out.println("FORGE_STORAGE_PAGES_OK empty growth shrink sparse capacity dropAll=PASS");
    }
    private static int count(BitSet slots,int capacity) { return StoragePages.visibleCount(capacity*81,81,capacity,slots::get); }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}
