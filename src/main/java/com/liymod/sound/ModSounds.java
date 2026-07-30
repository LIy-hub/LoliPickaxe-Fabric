package com.liymod.sound;

import com.liymod.LiyMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final Identifier LOLI_IMMUNITY_FIRST_ID = Identifier.fromNamespaceAndPath(
            LiyMod.MOD_ID,
            "loli_immunity_first"
    );
    public static final Identifier LOLI_IMMUNITY_SECOND_ID = Identifier.fromNamespaceAndPath(
            LiyMod.MOD_ID,
            "loli_immunity_second"
    );

    public static final SoundEvent LOLI_IMMUNITY_FIRST = SoundEvent.createVariableRangeEvent(
            LOLI_IMMUNITY_FIRST_ID
    );
    public static final SoundEvent LOLI_IMMUNITY_SECOND = SoundEvent.createVariableRangeEvent(
            LOLI_IMMUNITY_SECOND_ID
    );

    private ModSounds() {
    }

    public static void registerSoundEvents() {
        Registry.register(
                BuiltInRegistries.SOUND_EVENT,
                LOLI_IMMUNITY_FIRST_ID,
                LOLI_IMMUNITY_FIRST
        );
        Registry.register(
                BuiltInRegistries.SOUND_EVENT,
                LOLI_IMMUNITY_SECOND_ID,
                LOLI_IMMUNITY_SECOND
        );
        LiyMod.LOGGER.info("Registered alternating Loli immunity sounds");
    }
}
