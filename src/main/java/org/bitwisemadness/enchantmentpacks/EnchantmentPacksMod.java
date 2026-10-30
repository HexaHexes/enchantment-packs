package org.bitwisemadness.enchantmentpacks;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.bitwisemadness.enchantmentpacks.common.events.ModCommonEvents;
import org.bitwisemadness.enchantmentpacks.common.init.MainEnchantments;
import org.bitwisemadness.enchantmentpacks.common.packs.EnchantmentPackManager;

@Mod(EnchantmentPacksMod.MOD_ID)
public class EnchantmentPacksMod {
	public static final String MOD_ID = "enchantment_packs";

	public EnchantmentPacksMod() {
		LogUtil.info(Env.COMMON, "Initializing Enchantment Packs...");
		EnchantmentPackManager.initialize();
		var bus = FMLJavaModLoadingContext.get().getModEventBus();
		MainEnchantments.register(bus);
		bus.addListener(ModCommonEvents::commonSetup);
		LogUtil.info(Env.COMMON, "Enchantment Packs initialized");
	}
}
