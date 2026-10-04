package com.liymod.event;

import com.liymod.LiyMod;
import com.liymod.item.LoliFinalMiningEvents;
import static com.liymod.LiyMod.MOD_ID;

public final class AttackBlockEvents {
    private AttackBlockEvents() {
    }

    public static void registerEvents() {
        LiyMod.LOGGER.info("Registering attack block events for {}", MOD_ID);
        LoliFinalMiningEvents.registerEvents();
    }
}
