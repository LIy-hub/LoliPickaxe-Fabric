package com.liymod.network;

import net.minecraft.util.thread.BlockableEventLoop;

/** Keeps a received menu mutation ahead of the following close-menu packet. */
public final class LoliPacketTasks {
    private LoliPacketTasks() {
    }

    public static void execute(BlockableEventLoop<?> server, Runnable action) {
        if (server.isSameThread()) {
            action.run();
        } else {
            server.execute(action);
        }
    }
}
