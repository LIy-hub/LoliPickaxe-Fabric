package com.liymod;


import com.liymod.block.ModBlocks;
import com.liymod.combat.LoliExecutionManager;
import com.liymod.damage_type.ModDamageTypes;
import com.liymod.event.AttackBlockEvents;
import com.liymod.event.AttackEntityEvents;
import com.liymod.item.ModItemGroup;
import com.liymod.item.ModItems;
import com.liymod.item.SmallLoliGameplayEvents;
import com.liymod.item.SmallLoliMiningEvents;
import com.liymod.loliability.LoliAbilityEvents;
import com.liymod.protection.LoliProtection;
import com.liymod.recipe.ModRecipes;
import com.liymod.sound.ModSounds;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiyMod implements ModInitializer {
	public static final String MOD_ID = "liymod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Welcome to LoliPickaxe");
		ModSounds.registerSoundEvents();
		ModBlocks.registerModBlocks();
		ModItems.registerModItems();
		ModRecipes.registerRecipes();
		ModItemGroup.registerModItemGroup();
		SmallLoliGameplayEvents.registerEvents();
		SmallLoliMiningEvents.registerEvents();
		ModDamageTypes.registerDamageTypes();
		LoliProtection.registerProtection();
		LoliExecutionManager.registerEvents();
		AttackEntityEvents.registerEvents();
		AttackBlockEvents.registerEvents();
		LoliAbilityEvents.registerEvents();
	}
}
