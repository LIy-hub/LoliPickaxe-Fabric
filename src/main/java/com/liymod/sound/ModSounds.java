package com.liymod.sound;

import com.liymod.LiyMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
    public static final Identifier LOLI_IMMUNITY_FIRST_ID = Identifier.of(
            LiyMod.MOD_ID,
            "loli_immunity_first"
    );
    public static final Identifier LOLI_IMMUNITY_SECOND_ID = Identifier.of(
            LiyMod.MOD_ID,
            "loli_immunity_second"
    );

    public static final SoundEvent LOLI_IMMUNITY_FIRST = SoundEvent.of(
            LOLI_IMMUNITY_FIRST_ID
    );
    public static final SoundEvent LOLI_IMMUNITY_SECOND = SoundEvent.of(
            LOLI_IMMUNITY_SECOND_ID
    );

    private ModSounds() {
    }

    public static void registerSoundEvents() {
        Registry.register(
                Registries.SOUND_EVENT,
                LOLI_IMMUNITY_FIRST_ID,
                LOLI_IMMUNITY_FIRST
        );
        Registry.register(
                Registries.SOUND_EVENT,
                LOLI_IMMUNITY_SECOND_ID,
                LOLI_IMMUNITY_SECOND
        );
        LiyMod.LOGGER.info("Registered alternating Loli immunity sounds");
    }
}
