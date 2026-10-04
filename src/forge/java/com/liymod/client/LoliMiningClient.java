package com.liymod.client;

import com.liymod.config.FinalToolSettings;
import com.liymod.item.LoliFluidMining;
import com.liymod.item.LoliMiningCooldown;
import com.liymod.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Client requests never predict or mutate blocks; accepted server batches carry the result. */
public final class LoliMiningClient {
    private static final LoliMiningCooldown COOLDOWN=new LoliMiningCooldown();
    private LoliMiningClient() { }
    public static void reset() { COOLDOWN.clear(); }
    public static void attack(BlockPos origin) {
        Minecraft client=Minecraft.getInstance();
        if(client.level==null || client.player==null) return;
        var tool=client.player.getMainHandItem();int radius=FinalToolSettings.radius(tool);
        if(!LoliFluidMining.canMine(client.level.getBlockState(origin),LoliFluidMining.isEnabled(tool)) || !COOLDOWN.tryAcquire(client.player.getUUID(),radius)) return;
        try { ModNetwork.CHANNEL.sendToServer(new ModNetwork.MiningRequestPacket(origin.immutable())); }
        finally { COOLDOWN.finished(client.player.getUUID(),radius); }
    }
}
