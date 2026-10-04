package com.liymod;

import com.liymod.registry.ModContent;
import com.liymod.storage.LoliStorageData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;

/** Dev-run-only fixtures execute after the native Forge registry and event transformers. */
@Mod.EventBusSubscriber(modid=LiyMod.MOD_ID,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class ForgeNativeRegressionEvents {
    private static boolean verified;
    @SubscribeEvent public static void setup(TickEvent.ClientTickEvent event) {
        if(event.phase==TickEvent.Phase.END && !verified) {
            verified=true;
            com.liymod.item.ForgeFluidMiningRegressionTest.main(new String[0]);
            storage();
        }
    }
    private static void storage() {
        ItemStack owner=new ItemStack(ModContent.LOLI_PICKAXE.get());
        try(var batch=LoliStorageData.beginBatch(owner)) {
            require(LoliStorageData.insert(owner,new ItemStack(Items.DIAMOND,40)).isEmpty(),"First insertion failed");
            require(LoliStorageData.insert(owner,new ItemStack(Items.DIAMOND,40)).isEmpty(),"Second insertion failed");
            require(!owner.getOrCreateTag().contains(LoliStorageData.STORAGE_KEY),"Batch wrote before completion");
        }
        var loaded=LoliStorageData.load(owner);
        require(loaded.get(0).getCount()==64 && loaded.get(1).getCount()==16,"Batched count split changed rewards");
        loaded.get(0).setCount(1);
        require(LoliStorageData.load(owner).get(0).getCount()==64,"Public loaded items mutated owning cache");
        ItemStack copied=owner.copy();
        LoliStorageData.insert(copied,new ItemStack(Items.EMERALD,3));
        require(LoliStorageData.load(owner).get(2).isEmpty() && LoliStorageData.load(copied).get(2).is(Items.EMERALD),"Copied owning stacks shared storage");
        owner.getOrCreateTag().getList(LoliStorageData.STORAGE_KEY,Tag.TAG_COMPOUND).getCompound(0).getCompound("Stack").putByte("Count",(byte)7);
        require(LoliStorageData.load(owner).get(0).getCount()==7,"In-place legacy NBT edit left a stale cache");
        require(LoliStorageData.setBlacklisted(owner,net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(Items.EMERALD),true),"Forge blacklist did not retain IDs");
        require(LoliStorageData.insert(owner,new ItemStack(Items.EMERALD,1)).getCount()==1,"Blacklisted item entered storage");
        require(LoliStorageData.insert(owner,copied).getCount()==1,"Nested storage tool accepted");
        try(var batch=LoliStorageData.beginBatch(owner)) { LoliStorageData.insert(owner,new ItemStack(Items.GOLD_INGOT,5));throw new IllegalStateException("fixture"); }
        catch(IllegalStateException expected) { }
        require(LoliStorageData.load(owner).get(2).getCount()==5,"Exceptional batch lost accepted drops");
        LoliStorageData.setAutoAccept(owner,false);require(!LoliStorageData.autoAccept(owner),"Forge auto-accept setting changed");
        System.out.println("FORGE_NATIVE_STORAGE_OK batchedPersistence splitCounts copyIsolation mutableNBT blacklist nestedTools exceptionRecovery autoAccept=PASS");
    }
    private static void require(boolean value,String message) { if(!value) throw new AssertionError(message); }
}
