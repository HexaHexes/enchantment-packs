package org.bitwisemadness.enchantmentpacks.common.init;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.bitwisemadness.enchantmentpacks.EnchantmentPacksMod;
import org.bitwisemadness.enchantmentpacks.common.enchantments.PackAttributeEnchantment;
import org.bitwisemadness.enchantmentpacks.common.packs.EnchantmentPackManager;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MainEnchantments {
	public static final DeferredRegister<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, EnchantmentPacksMod.MOD_ID);
	private static final Map<String, RegistryObject<net.minecraft.world.item.enchantment.Enchantment>> PACK_ENCHANTMENTS = new LinkedHashMap<>();
	private static boolean registered = false;

	private MainEnchantments() {}

	public static void register(IEventBus eventBus) {
		ensureRegistered();
		ENCHANTMENTS.register(eventBus);
	}

	private static void ensureRegistered() {
		if (registered) {
			return;
		}
		registered = true;
		for (EnchantmentPackManager.ResolvedEnchantmentDefinition definition : EnchantmentPackManager.getResolvedEnchantments().values()) {
			PACK_ENCHANTMENTS.put(definition.getDefinition().id, ENCHANTMENTS.register(definition.getRegistryName(), () -> new PackAttributeEnchantment(definition)));
		}
	}

	public static RegistryObject<net.minecraft.world.item.enchantment.Enchantment> getPackEnchantment(String id) {
		return PACK_ENCHANTMENTS.get(id);
	}
}
