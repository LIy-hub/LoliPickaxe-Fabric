package com.liymod;


import com.liymod.block.ModBlocks;
import com.liymod.combat.LoliExecutionManager;
import com.liymod.combat.LoliLegacyExecutionPolicy;
import com.liymod.damage_type.ModDamageTypes;
import com.liymod.event.AttackBlockEvents;
import com.liymod.event.AttackEntityEvents;
import com.liymod.entity.ModEntities;
import com.liymod.item.ModItemGroup;
import com.liymod.item.ModItems;
import com.liymod.item.SmallLoliGameplayEvents;
import com.liymod.item.SmallLoliMiningEvents;
import com.liymod.loliability.LoliAbilityEvents;
import com.liymod.network.ModNetworking;
import com.liymod.menu.ModMenus;
import com.liymod.protection.LoliProtection;
import com.liymod.recipe.ModRecipes;
import com.liymod.sound.ModSounds;
import com.liymod.storage.LoliStorageEvents;
import com.liymod.config.LoliServerConfig;
import com.liymod.item.LoliFinalItemEvents;
import com.liymod.item.LoliAuxiliaryDropEvents;
import com.liymod.command.LoliCommands;
import com.liymod.compat.StrengthConfrontation;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiyMod implements ModInitializer {
	public static final String MOD_ID = "liymod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Welcome to LoliPickaxe");
		LoliServerConfig.initialize();
		LoliCommands.register();
		ModNetworking.register();
		ModMenus.registerMenus();
		ModSounds.registerSoundEvents();
		ModEntities.registerEntities();
		ModBlocks.registerModBlocks();
		ModItems.registerModItems();
		ModRecipes.registerRecipes();
		ModItemGroup.registerModItemGroup();
		SmallLoliGameplayEvents.registerEvents();
		SmallLoliMiningEvents.registerEvents();
		LoliStorageEvents.registerEvents();
		LoliFinalItemEvents.registerEvents();
		LoliAuxiliaryDropEvents.registerEvents();
		ModDamageTypes.registerDamageTypes();
		LoliProtection.registerProtection();
		LoliExecutionManager.registerEvents();
		StrengthConfrontation.registerEvents();
		LoliLegacyExecutionPolicy.registerEvents();
		AttackEntityEvents.registerEvents();
		AttackBlockEvents.registerEvents();
		LoliAbilityEvents.registerEvents();
	}
}
