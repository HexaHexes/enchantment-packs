package org.bitwisemadness.enchantmentpacks.common.enchantments;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;
import org.bitwisemadness.enchantmentpacks.common.packs.EnchantmentPackManager;

public class PackAttributeEnchantment extends Enchantment {
	private final EnchantmentPackManager.ResolvedEnchantmentDefinition definition;

	public PackAttributeEnchantment(EnchantmentPackManager.ResolvedEnchantmentDefinition definition) {
		super(definition.getRarity(), definition.getCategory(), definition.getSlots());
		this.definition = definition;
	}

	public EnchantmentPackManager.ResolvedEnchantmentDefinition getDefinition() {
		return definition;
	}

	@Override
	public int getMinCost(int level) {
		return definition.getMinCost(level);
	}

	@Override
	public int getMaxCost(int level) {
		return definition.getMaxCost(level);
	}

	@Override
	public int getMaxLevel() {
		return definition.getMaxLevel();
	}

	@Override
	public boolean isTreasureOnly() {
		return Boolean.TRUE.equals(definition.getDefinition().treasureOnly);
	}

	@Override
	public boolean isCurse() {
		return Boolean.TRUE.equals(definition.getDefinition().curse);
	}

	@Override
	public boolean isTradeable() {
		return !Boolean.FALSE.equals(definition.getDefinition().tradeable);
	}

	@Override
	public boolean isDiscoverable() {
		return !Boolean.FALSE.equals(definition.getDefinition().discoverable);
	}

	@Override
	public boolean isAllowedOnBooks() {
		return !Boolean.FALSE.equals(definition.getDefinition().allowedOnBooks);
	}

	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack) {
		if (Boolean.FALSE.equals(definition.getDefinition().canApplyAtEnchantingTable)) {
			return false;
		}
		return super.canApplyAtEnchantingTable(stack);
	}

	@Override
	protected boolean checkCompatibility(Enchantment other) {
		if (!super.checkCompatibility(other)) {
			return false;
		}

		ResourceLocation otherId = ForgeRegistries.ENCHANTMENTS.getKey(other);
		if (otherId == null) {
			return true;
		}

		String otherKey = otherId.toString().toLowerCase();
		for (String blocked : definition.getDefinition().exclusiveWith) {
			if (blocked != null && blocked.trim().equalsIgnoreCase(otherKey)) {
				return false;
			}
		}
		return true;
	}
}
