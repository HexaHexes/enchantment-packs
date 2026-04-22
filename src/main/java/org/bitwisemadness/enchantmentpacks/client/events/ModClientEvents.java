package org.bitwisemadness.enchantmentpacks.client.events;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.bitwisemadness.enchantmentpacks.EnchantmentPacksMod;
import org.bitwisemadness.enchantmentpacks.client.resources.EnchantmentLangPackResources;
import org.bitwisemadness.enchantmentpacks.common.packs.EnchantmentPackManager;

@Mod.EventBusSubscriber(modid = EnchantmentPacksMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClientEvents {
	private ModClientEvents() {}

	@SubscribeEvent
	public static void onAddPackFinders(AddPackFindersEvent event) {
		if (event.getPackType() != PackType.CLIENT_RESOURCES || !EnchantmentPackManager.hasLoadedEnchantments()) {
			return;
		}

		event.addRepositorySource((packConsumer) -> {
			Pack pack = Pack.readMetaAndCreate(
				"enchantmentpacks_generated_translations",
				Component.literal("Enchantment Packs Generated Translations"),
				true,
				EnchantmentLangPackResources::new,
				PackType.CLIENT_RESOURCES,
				Pack.Position.TOP,
				PackSource.BUILT_IN
			);
			if (pack != null) {
				packConsumer.accept(pack);
			}
		});
	}
}
