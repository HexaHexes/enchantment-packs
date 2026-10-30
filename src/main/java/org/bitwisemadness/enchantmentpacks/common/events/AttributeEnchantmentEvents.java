package org.bitwisemadness.enchantmentpacks.common.events;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.bitwisemadness.enchantmentpacks.EnchantmentPacksMod;
import org.bitwisemadness.enchantmentpacks.common.init.MainEnchantments;
import org.bitwisemadness.enchantmentpacks.common.packs.EnchantmentPackManager;

@Mod.EventBusSubscriber(modid = EnchantmentPacksMod.MOD_ID)
public final class AttributeEnchantmentEvents {
	private AttributeEnchantmentEvents() {}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
			return;
		}
		if (event.player.tickCount % 20 == 0) {
			refreshPlayer(event.player);
		}
	}

	@SubscribeEvent
	public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
		if (!event.getEntity().level().isClientSide && event.getEntity() instanceof ServerPlayer player) {
			refreshPlayer(player);
		}
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			refreshPlayer(player);
		}
	}

	@SubscribeEvent
	public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			refreshPlayer(player);
		}
	}

	@SubscribeEvent
	public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			refreshPlayer(player);
		}
	}

	public static void refreshPlayer(net.minecraft.world.entity.player.Player player) {
		removeManagedModifiers(player);
		applyManagedModifiers(player);
	}

	private static void removeManagedModifiers(net.minecraft.world.entity.player.Player player) {
		for (EnchantmentPackManager.ResolvedEnchantmentDefinition definition : EnchantmentPackManager.getResolvedEnchantments().values()) {
			for (EnchantmentPackManager.ResolvedAttributeModifier modifier : definition.getModifiers()) {
				Attribute attribute = modifier.resolveAttribute();
				if (attribute == null) {
					continue;
				}
				AttributeInstance instance = player.getAttribute(attribute);
				if (instance == null) {
					continue;
				}
				for (EquipmentSlot slot : EquipmentSlot.values()) {
					UUID uuid = buildModifierUuid(definition.getDefinition().id, modifier.getIndex(), slot);
					instance.removeModifier(uuid);
				}
			}
		}
	}

	private static void applyManagedModifiers(net.minecraft.world.entity.player.Player player) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = player.getItemBySlot(slot);
			if (stack.isEmpty()) {
				continue;
			}
			for (EnchantmentPackManager.ResolvedEnchantmentDefinition definition : EnchantmentPackManager.getResolvedEnchantments().values()) {
				var registryObject = MainEnchantments.getPackEnchantment(definition.getDefinition().id);
				if (registryObject == null) {
					continue;
				}
				Enchantment enchantment = registryObject.get();
				int level = stack.getEnchantmentLevel(enchantment);
				if (level <= 0) {
					continue;
				}
				for (EnchantmentPackManager.ResolvedAttributeModifier modifier : definition.getModifiers()) {
					if (!modifier.appliesToSlot(slot)) {
						continue;
					}
					Attribute attribute = modifier.resolveAttribute();
					if (attribute == null) {
						continue;
					}
					AttributeInstance instance = player.getAttribute(attribute);
					if (instance == null) {
						continue;
					}
					double amount = modifier.resolveAmount(level);
					if (Math.abs(amount) < 1.0E-9D) {
						continue;
					}
					UUID uuid = buildModifierUuid(definition.getDefinition().id, modifier.getIndex(), slot);
					String modifierName = EnchantmentPacksMod.MOD_ID + ":" + definition.getRegistryName() + "/" + slot.getName() + "/" + modifier.getIndex();
					instance.addTransientModifier(new AttributeModifier(uuid, modifierName, amount, modifier.getOperation().getAttributeOperation()));
				}
			}
		}
	}

	private static UUID buildModifierUuid(String enchantmentId, int modifierIndex, EquipmentSlot slot) {
		String seed = EnchantmentPacksMod.MOD_ID + "|" + enchantmentId + "|" + modifierIndex + "|" + slot.getName();
		return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
	}
}
