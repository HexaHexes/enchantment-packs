package org.bitwisemadness.enchantmentpacks.common.packs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.bitwisemadness.enchantmentpacks.EnchantmentPacksMod;
import org.bitwisemadness.enchantmentpacks.Env;
import org.bitwisemadness.enchantmentpacks.LogUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class EnchantmentPackManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().setLenient().create();
	private static final String PACK_RELATIVE_ROOT = "data/enchantmentpacks/enchantments/";
	private static final Path INSTANCE_PACK_DIR = FMLPaths.GAMEDIR.get().resolve("enchantmentpacks");

	@Getter
	private static final Map<String, ResolvedEnchantmentDefinition> resolvedEnchantments = new LinkedHashMap<>();
	private static boolean initialized = false;

	private EnchantmentPackManager() {}

	public static void initialize() {
		if (initialized) {
			return;
		}

		resolvedEnchantments.clear();
		Map<String, EnchantmentDefinition> mergedEnchantments = new LinkedHashMap<>();
		loadExternalDefinitions(mergedEnchantments);
		resolveEnchantments(mergedEnchantments);
		initialized = true;
		LogUtil.info(Env.COMMON, "Loaded {} enchantment definitions", resolvedEnchantments.size());
	}

	public static boolean hasLoadedEnchantments() {
		return !resolvedEnchantments.isEmpty();
	}

	public static Map<String, String> buildGeneratedEnchantmentTranslations() {
		Map<String, String> translations = new LinkedHashMap<>();
		for (ResolvedEnchantmentDefinition enchantment : resolvedEnchantments.values()) {
			translations.put("enchantment." + EnchantmentPacksMod.MOD_ID + "." + enchantment.getRegistryName(), enchantment.getDisplayName());
			if (enchantment.getDescription() != null && !enchantment.getDescription().isBlank()) {
				translations.put("enchantment." + EnchantmentPacksMod.MOD_ID + "." + enchantment.getRegistryName() + ".desc", enchantment.getDescription());
			}
		}
		return translations;
	}

	public static byte[] buildGeneratedEnchantmentTranslationsJsonBytes() {
		return GSON.toJson(buildGeneratedEnchantmentTranslations()).getBytes(StandardCharsets.UTF_8);
	}

	private static void loadExternalDefinitions(Map<String, EnchantmentDefinition> mergedEnchantments) {
		try {
			Files.createDirectories(INSTANCE_PACK_DIR);
			List<Path> folderPacks = new ArrayList<>();
			List<Path> zipPacks = new ArrayList<>();

			try (var stream = Files.list(INSTANCE_PACK_DIR)) {
				stream.sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT))).forEach(path -> {
					String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
					if (Files.isDirectory(path)) {
						folderPacks.add(path);
					} else if (fileName.endsWith(".zip") || fileName.endsWith(".jar")) {
						zipPacks.add(path);
					}
				});
			}

			for (Path packDir : folderPacks) {
				loadFolderPack(packDir, mergedEnchantments);
			}
			for (Path zipPack : zipPacks) {
				loadZipPack(zipPack, mergedEnchantments);
			}
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed scanning enchantment pack directory '{}': {}", INSTANCE_PACK_DIR, e.getMessage());
		}
	}

	private static void loadFolderPack(Path packDir, Map<String, EnchantmentDefinition> mergedEnchantments) {
		Path enchantmentRoot = packDir.resolve(PACK_RELATIVE_ROOT);
		if (!Files.isDirectory(enchantmentRoot)) {
			return;
		}

		try (var walk = Files.walk(enchantmentRoot)) {
			walk.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".json"))
				.sorted(Comparator.comparing(path -> path.toString().toLowerCase(Locale.ROOT)))
				.forEach(path -> {
					try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
						EnchantmentDefinition enchantment = GSON.fromJson(reader, EnchantmentDefinition.class);
						mergeDefinition(mergedEnchantments, enchantment, "folder:" + packDir.getFileName() + "/" + enchantmentRoot.relativize(path));
					} catch (Exception e) {
						LogUtil.error(Env.COMMON, "Failed to parse enchantment definition '{}': {}", path, e.getMessage());
					}
				});
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed reading enchantment pack folder '{}': {}", packDir, e.getMessage());
		}
	}

	private static void loadZipPack(Path zipPath, Map<String, EnchantmentDefinition> mergedEnchantments) {
		try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
			List<? extends ZipEntry> entries = Collections.list(zipFile.entries());
			entries.stream()
				.filter(entry -> !entry.isDirectory() && entry.getName().startsWith(PACK_RELATIVE_ROOT) && entry.getName().endsWith(".json"))
				.sorted(Comparator.comparing(ZipEntry::getName, String.CASE_INSENSITIVE_ORDER))
				.forEach(entry -> {
					try (InputStream inputStream = zipFile.getInputStream(entry)) {
						EnchantmentDefinition enchantment = GSON.fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), EnchantmentDefinition.class);
						mergeDefinition(mergedEnchantments, enchantment, "zip:" + zipPath.getFileName() + "!/" + entry.getName());
					} catch (Exception e) {
						LogUtil.error(Env.COMMON, "Failed to parse enchantment definition '{}' in '{}': {}", entry.getName(), zipPath.getFileName(), e.getMessage());
					}
				});
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed reading enchantment pack zip '{}': {}", zipPath, e.getMessage());
		}
	}

	private static void mergeDefinition(Map<String, EnchantmentDefinition> mergedEnchantments, EnchantmentDefinition incoming, String source) {
		if (incoming == null || incoming.id == null || incoming.id.isBlank()) {
			LogUtil.warn(Env.COMMON, "Skipping enchantment definition from '{}' because it has no id", source);
			return;
		}

		if (!matchesLoadRequirements(incoming)) {
			LogUtil.info(Env.COMMON, "Skipping enchantment override '{}' from '{}' because its mod conditions were not met", incoming.id, source);
			return;
		}

		String enchantmentId = normalizeId(incoming.id);
		incoming.id = enchantmentId;
		EnchantmentDefinition existing = mergedEnchantments.get(enchantmentId);
		if (existing == null || Boolean.TRUE.equals(incoming.replace)) {
			mergedEnchantments.put(enchantmentId, incoming.copy());
			return;
		}

		existing.mergeFrom(incoming);
	}

	private static void resolveEnchantments(Map<String, EnchantmentDefinition> mergedEnchantments) {
		for (EnchantmentDefinition enchantment : mergedEnchantments.values()) {
			if (enchantment == null || enchantment.id == null) {
				continue;
			}
			if (Boolean.FALSE.equals(enchantment.enabled) || !matchesLoadRequirements(enchantment)) {
				continue;
			}

			EnchantmentCategory category = parseCategory(enchantment.category);
			if (category == null) {
				LogUtil.warn(Env.COMMON, "Skipping enchantment '{}' because category '{}' is invalid", enchantment.id, enchantment.category);
				continue;
			}

			EquipmentSlot[] slots = parseSlots(enchantment.slots);
			if (slots.length == 0) {
				LogUtil.warn(Env.COMMON, "Skipping enchantment '{}' because it has no valid slots", enchantment.id);
				continue;
			}

			List<ResolvedAttributeModifier> modifiers = new ArrayList<>();
			for (int i = 0; i < enchantment.modifiers.size(); i++) {
				AttributeModifierDefinition modifier = enchantment.modifiers.get(i);
				if (modifier == null || Boolean.FALSE.equals(modifier.enabled)) {
					continue;
				}
				ResourceLocation attributeId = parseId(modifier.attribute);
				if (attributeId == null) {
					LogUtil.warn(Env.COMMON, "Skipping attribute modifier {} on enchantment '{}' because attribute '{}' is invalid", i, enchantment.id, modifier.attribute);
					continue;
				}
				if (!canReferenceRegistryNamespace(attributeId)) {
					LogUtil.warn(Env.COMMON, "Skipping attribute modifier {} on enchantment '{}' because attribute namespace '{}' is unavailable", i, enchantment.id, attributeId.getNamespace());
					continue;
				}
				if (!ForgeRegistries.ATTRIBUTES.containsKey(attributeId) && "minecraft".equals(attributeId.getNamespace())) {
					LogUtil.warn(Env.COMMON, "Skipping attribute modifier {} on enchantment '{}' because vanilla attribute '{}' does not exist", i, enchantment.id, attributeId);
					continue;
				}
				ResolvedOperation operation = ResolvedOperation.from(modifier.operation);
				if (operation == null) {
					LogUtil.warn(Env.COMMON, "Skipping attribute modifier {} on enchantment '{}' because operation '{}' is invalid", i, enchantment.id, modifier.operation);
					continue;
				}
				List<EquipmentSlot> modifierSlots = parseSlotsList(modifier.slots);
				modifiers.add(new ResolvedAttributeModifier(i, attributeId, operation, modifier.copy(), modifierSlots));
			}

			if (modifiers.isEmpty()) {
				LogUtil.warn(Env.COMMON, "Skipping enchantment '{}' because it has no valid attribute modifiers", enchantment.id);
				continue;
			}

			String registryName = sanitizeRegistryPath(enchantment.id);
			resolvedEnchantments.put(enchantment.id, new ResolvedEnchantmentDefinition(enchantment.copy(), registryName, category, slots, modifiers));
		}
	}

	private static boolean matchesLoadRequirements(EnchantmentDefinition enchantment) {
		if (enchantment == null) {
			return false;
		}
		if (enchantment.requiredMods != null && enchantment.requiredMods.stream().anyMatch(modId -> modId != null && !modId.isBlank() && !ModList.get().isLoaded(modId))) {
			return false;
		}
		if (enchantment.loadCondition == null || enchantment.loadCondition.type == null || enchantment.loadCondition.type.isBlank()) {
			return true;
		}
		return enchantment.loadCondition.matches();
	}

	private static String normalizeId(String id) {
		return id == null ? null : id.trim().toLowerCase(Locale.ROOT);
	}

	private static String sanitizeRegistryPath(String value) {
		return normalizeId(value).replace(':', '_').replace('/', '_').replace('-', '_').replace(' ', '_');
	}

	private static ResourceLocation parseId(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		return ResourceLocation.tryParse(raw.trim().toLowerCase(Locale.ROOT));
	}

	private static boolean canReferenceRegistryNamespace(ResourceLocation id) {
		if (id == null) {
			return false;
		}
		String namespace = id.getNamespace();
		return "minecraft".equals(namespace) || EnchantmentPacksMod.MOD_ID.equals(namespace) || ModList.get().isLoaded(namespace);
	}

	private static EnchantmentCategory parseCategory(String raw) {
		String value = raw == null ? "breakable" : raw.trim().toLowerCase(Locale.ROOT);
		return switch (value) {
			case "armor" -> EnchantmentCategory.ARMOR;
			case "armor_feet", "boots" -> EnchantmentCategory.ARMOR_FEET;
			case "armor_legs", "leggings" -> EnchantmentCategory.ARMOR_LEGS;
			case "armor_chest", "chestplate" -> EnchantmentCategory.ARMOR_CHEST;
			case "armor_head", "helmet" -> EnchantmentCategory.ARMOR_HEAD;
			case "weapon" -> EnchantmentCategory.WEAPON;
			case "digger", "tool" -> EnchantmentCategory.DIGGER;
			case "fishing_rod" -> EnchantmentCategory.FISHING_ROD;
			case "trident" -> EnchantmentCategory.TRIDENT;
			case "bow" -> EnchantmentCategory.BOW;
			case "wearable" -> EnchantmentCategory.WEARABLE;
			case "crossbow" -> EnchantmentCategory.CROSSBOW;
			case "vanishable" -> EnchantmentCategory.VANISHABLE;
			case "breakable" -> EnchantmentCategory.BREAKABLE;
			default -> null;
		};
	}

	private static EquipmentSlot[] parseSlots(List<String> rawSlots) {
		List<EquipmentSlot> slots = parseSlotsList(rawSlots);
		return slots.toArray(new EquipmentSlot[0]);
	}

	private static List<EquipmentSlot> parseSlotsList(List<String> rawSlots) {
		List<EquipmentSlot> slots = new ArrayList<>();
		if (rawSlots == null) {
			return slots;
		}
		for (String rawSlot : rawSlots) {
			EquipmentSlot slot = parseSlot(rawSlot);
			if (slot != null && !slots.contains(slot)) {
				slots.add(slot);
			}
		}
		return slots;
	}

	private static EquipmentSlot parseSlot(String rawSlot) {
		if (rawSlot == null || rawSlot.isBlank()) {
			return null;
		}
		String value = rawSlot.trim().toLowerCase(Locale.ROOT);
		return switch (value) {
			case "mainhand", "main_hand", "hand" -> EquipmentSlot.MAINHAND;
			case "offhand", "off_hand" -> EquipmentSlot.OFFHAND;
			case "feet", "boots" -> EquipmentSlot.FEET;
			case "legs", "leggings" -> EquipmentSlot.LEGS;
			case "chest", "chestplate" -> EquipmentSlot.CHEST;
			case "head", "helmet" -> EquipmentSlot.HEAD;
			default -> null;
		};
	}

	public static class EnchantmentDefinition {
		public Boolean replace = false;
		public String id;
		public Boolean enabled = true;
		@SerializedName(value = "display_name", alternate = {"displayName"})
		public String displayName;
		public String description;
		public String category = "breakable";
		public List<String> slots = new ArrayList<>();
		@SerializedName(value = "max_level", alternate = {"maxLevel"})
		public Integer maxLevel = 1;
		public String rarity = "common";
		@SerializedName(value = "treasure_only", alternate = {"treasureOnly"})
		public Boolean treasureOnly = false;
		public Boolean curse = false;
		public Boolean discoverable = true;
		public Boolean tradeable = true;
		@SerializedName(value = "allowed_on_books", alternate = {"allowedOnBooks"})
		public Boolean allowedOnBooks = true;
		@SerializedName(value = "can_apply_at_enchanting_table", alternate = {"canApplyAtEnchantingTable"})
		public Boolean canApplyAtEnchantingTable = true;
		@SerializedName(value = "min_cost", alternate = {"minCost"})
		public Integer minCost = 1;
		@SerializedName(value = "min_cost_per_level", alternate = {"minCostPerLevel"})
		public Integer minCostPerLevel = 10;
		@SerializedName(value = "max_cost_spread", alternate = {"maxCostSpread"})
		public Integer maxCostSpread = 15;
		@SerializedName(value = "required_mods", alternate = {"requiredMods"})
		public List<String> requiredMods = new ArrayList<>();
		@SerializedName(value = "load_condition", alternate = {"loadCondition"})
		public LoadConditionDefinition loadCondition;
		@SerializedName(value = "exclusive_with", alternate = {"exclusiveWith"})
		public List<String> exclusiveWith = new ArrayList<>();
		public List<AttributeModifierDefinition> modifiers = new ArrayList<>();

		public EnchantmentDefinition copy() {
			EnchantmentDefinition copy = new EnchantmentDefinition();
			copy.replace = this.replace;
			copy.id = this.id;
			copy.enabled = this.enabled;
			copy.displayName = this.displayName;
			copy.description = this.description;
			copy.category = this.category;
			copy.slots = this.slots == null ? new ArrayList<>() : new ArrayList<>(this.slots);
			copy.maxLevel = this.maxLevel;
			copy.rarity = this.rarity;
			copy.treasureOnly = this.treasureOnly;
			copy.curse = this.curse;
			copy.discoverable = this.discoverable;
			copy.tradeable = this.tradeable;
			copy.allowedOnBooks = this.allowedOnBooks;
			copy.canApplyAtEnchantingTable = this.canApplyAtEnchantingTable;
			copy.minCost = this.minCost;
			copy.minCostPerLevel = this.minCostPerLevel;
			copy.maxCostSpread = this.maxCostSpread;
			copy.requiredMods = this.requiredMods == null ? new ArrayList<>() : new ArrayList<>(this.requiredMods);
			copy.loadCondition = this.loadCondition == null ? null : this.loadCondition.copy();
			copy.exclusiveWith = this.exclusiveWith == null ? new ArrayList<>() : new ArrayList<>(this.exclusiveWith);
			for (AttributeModifierDefinition modifier : this.modifiers) {
				copy.modifiers.add(modifier.copy());
			}
			return copy;
		}

		public void mergeFrom(EnchantmentDefinition incoming) {
			if (incoming.enabled != null) this.enabled = incoming.enabled;
			if (incoming.displayName != null) this.displayName = incoming.displayName;
			if (incoming.description != null) this.description = incoming.description;
			if (incoming.category != null) this.category = incoming.category;
			if (incoming.slots != null && !incoming.slots.isEmpty()) this.slots = new ArrayList<>(incoming.slots);
			if (incoming.maxLevel != null) this.maxLevel = incoming.maxLevel;
			if (incoming.rarity != null) this.rarity = incoming.rarity;
			if (incoming.treasureOnly != null) this.treasureOnly = incoming.treasureOnly;
			if (incoming.curse != null) this.curse = incoming.curse;
			if (incoming.discoverable != null) this.discoverable = incoming.discoverable;
			if (incoming.tradeable != null) this.tradeable = incoming.tradeable;
			if (incoming.allowedOnBooks != null) this.allowedOnBooks = incoming.allowedOnBooks;
			if (incoming.canApplyAtEnchantingTable != null) this.canApplyAtEnchantingTable = incoming.canApplyAtEnchantingTable;
			if (incoming.minCost != null) this.minCost = incoming.minCost;
			if (incoming.minCostPerLevel != null) this.minCostPerLevel = incoming.minCostPerLevel;
			if (incoming.maxCostSpread != null) this.maxCostSpread = incoming.maxCostSpread;
			if (incoming.requiredMods != null && !incoming.requiredMods.isEmpty()) this.requiredMods = new ArrayList<>(incoming.requiredMods);
			if (incoming.loadCondition != null) this.loadCondition = incoming.loadCondition.copy();
			if (incoming.exclusiveWith != null && !incoming.exclusiveWith.isEmpty()) this.exclusiveWith = new ArrayList<>(incoming.exclusiveWith);
			mergeModifiers(incoming.modifiers);
		}

		private void mergeModifiers(List<AttributeModifierDefinition> incomingModifiers) {
			if (incomingModifiers == null) {
				return;
			}
			Map<String, AttributeModifierDefinition> merged = new LinkedHashMap<>();
			for (AttributeModifierDefinition modifier : this.modifiers) {
				if (modifier != null) {
					merged.put(modifier.key(), modifier.copy());
				}
			}
			for (AttributeModifierDefinition modifier : incomingModifiers) {
				if (modifier == null) {
					continue;
				}
				AttributeModifierDefinition existing = merged.get(modifier.key());
				if (existing == null || Boolean.TRUE.equals(modifier.replace)) {
					merged.put(modifier.key(), modifier.copy());
				} else {
					existing.mergeFrom(modifier);
				}
			}
			this.modifiers = new ArrayList<>(merged.values());
		}
	}

	public static class LoadConditionDefinition {
		public String type;
		public String modid;
		public List<String> mods = new ArrayList<>();

		public boolean matches() {
			String normalizedType = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
			return switch (normalizedType) {
				case "mod_loaded" -> modid != null && !modid.isBlank() && ModList.get().isLoaded(modid);
				case "all_mods_loaded" -> mods != null && !mods.isEmpty() && mods.stream().allMatch(mod -> mod != null && !mod.isBlank() && ModList.get().isLoaded(mod));
				case "any_mod_loaded" -> mods != null && mods.stream().anyMatch(mod -> mod != null && !mod.isBlank() && ModList.get().isLoaded(mod));
				default -> true;
			};
		}

		public LoadConditionDefinition copy() {
			LoadConditionDefinition copy = new LoadConditionDefinition();
			copy.type = this.type;
			copy.modid = this.modid;
			copy.mods = this.mods == null ? new ArrayList<>() : new ArrayList<>(this.mods);
			return copy;
		}
	}

	public static class AttributeModifierDefinition {
		public Boolean replace = false;
		public Boolean enabled = true;
		public String attribute;
		public String operation = "add_value";
		public Double amount = 0.0D;
		@SerializedName(value = "amount_per_level", alternate = {"amountPerLevel"})
		public Double amountPerLevel = 0.0D;
		public List<String> slots = new ArrayList<>();

		public AttributeModifierDefinition copy() {
			AttributeModifierDefinition copy = new AttributeModifierDefinition();
			copy.replace = this.replace;
			copy.enabled = this.enabled;
			copy.attribute = this.attribute;
			copy.operation = this.operation;
			copy.amount = this.amount;
			copy.amountPerLevel = this.amountPerLevel;
			copy.slots = this.slots == null ? new ArrayList<>() : new ArrayList<>(this.slots);
			return copy;
		}

		public void mergeFrom(AttributeModifierDefinition incoming) {
			if (incoming.enabled != null) this.enabled = incoming.enabled;
			if (incoming.attribute != null) this.attribute = incoming.attribute;
			if (incoming.operation != null) this.operation = incoming.operation;
			if (incoming.amount != null) this.amount = incoming.amount;
			if (incoming.amountPerLevel != null) this.amountPerLevel = incoming.amountPerLevel;
			if (incoming.slots != null && !incoming.slots.isEmpty()) this.slots = new ArrayList<>(incoming.slots);
		}

		public String key() {
			return normalizeId(attribute) + ":" + normalizeId(operation) + ":" + String.join(",", slots == null ? List.of() : slots.stream().map(slot -> slot.toLowerCase(Locale.ROOT)).toList());
		}
	}

	@Getter
	public static class ResolvedEnchantmentDefinition {
		private final EnchantmentDefinition definition;
		private final String registryName;
		private final EnchantmentCategory category;
		private final EquipmentSlot[] slots;
		private final List<ResolvedAttributeModifier> modifiers;

		public ResolvedEnchantmentDefinition(EnchantmentDefinition definition, String registryName, EnchantmentCategory category, EquipmentSlot[] slots, List<ResolvedAttributeModifier> modifiers) {
			this.definition = definition;
			this.registryName = registryName;
			this.category = category;
			this.slots = slots;
			this.modifiers = modifiers;
		}

		public String getDisplayName() {
			if (definition.displayName != null && !definition.displayName.isBlank()) {
				return definition.displayName;
			}
			String raw = definition.id == null ? "Enchantment" : definition.id.replace('_', ' ').replace('-', ' ');
			StringBuilder builder = new StringBuilder();
			for (String part : raw.split(" ")) {
				if (part.isBlank()) {
					continue;
				}
				if (!builder.isEmpty()) {
					builder.append(' ');
				}
				builder.append(Character.toUpperCase(part.charAt(0)));
				if (part.length() > 1) {
					builder.append(part.substring(1));
				}
			}
			return !builder.isEmpty() ? builder.toString() : "Enchantment";
		}

		public String getDescription() {
			return definition.description;
		}

		public Enchantment.Rarity getRarity() {
			String raw = definition.rarity == null ? "common" : definition.rarity.trim().toLowerCase(Locale.ROOT);
			return switch (raw) {
				case "uncommon" -> Enchantment.Rarity.UNCOMMON;
				case "rare" -> Enchantment.Rarity.RARE;
				case "very_rare", "veryrare", "epic" -> Enchantment.Rarity.VERY_RARE;
				default -> Enchantment.Rarity.COMMON;
			};
		}

		public int getMaxLevel() {
			return definition.maxLevel == null ? 1 : Math.max(1, definition.maxLevel);
		}

		public int getMinCost(int level) {
			int minCost = definition.minCost == null ? 1 : definition.minCost;
			int perLevel = definition.minCostPerLevel == null ? 10 : definition.minCostPerLevel;
			return Math.max(1, minCost + Math.max(0, level - 1) * perLevel);
		}

		public int getMaxCost(int level) {
			int spread = definition.maxCostSpread == null ? 15 : definition.maxCostSpread;
			return getMinCost(level) + Math.max(0, spread);
		}
	}

	@Getter
	public static class ResolvedAttributeModifier {
		private final int index;
		private final ResourceLocation attributeId;
		private final ResolvedOperation operation;
		private final AttributeModifierDefinition definition;
		private final List<EquipmentSlot> slots;

		public ResolvedAttributeModifier(int index, ResourceLocation attributeId, ResolvedOperation operation, AttributeModifierDefinition definition, List<EquipmentSlot> slots) {
			this.index = index;
			this.attributeId = attributeId;
			this.operation = operation;
			this.definition = definition;
			this.slots = slots;
		}

		public Attribute resolveAttribute() {
			return ForgeRegistries.ATTRIBUTES.getValue(attributeId);
		}

		public boolean appliesToSlot(EquipmentSlot slot) {
			return slots == null || slots.isEmpty() || slots.contains(slot);
		}

		public double resolveAmount(int level) {
			double baseAmount = definition.amount == null ? 0.0D : definition.amount;
			double perLevel = definition.amountPerLevel == null ? 0.0D : definition.amountPerLevel;
			double value = baseAmount + (Math.max(1, level) - 1) * perLevel;
			return operation.convertConfiguredAmount(value);
		}
	}

	@Getter
	public enum ResolvedOperation {
		ADD_VALUE(net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION, false),
		SUBTRACT_VALUE(net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION, false),
		MULTIPLY_BASE(net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE, false),
		MULTIPLY_TOTAL(net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL, false),
		ADD_PERCENTAGE_BASE(net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE, true),
		SUBTRACT_PERCENTAGE_BASE(net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE, true),
		ADD_PERCENTAGE_TOTAL(net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL, true),
		SUBTRACT_PERCENTAGE_TOTAL(net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL, true);

		private final net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation attributeOperation;
		private final boolean percentage;

		ResolvedOperation(net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation attributeOperation, boolean percentage) {
			this.attributeOperation = attributeOperation;
			this.percentage = percentage;
		}

		public double convertConfiguredAmount(double amount) {
			double converted = percentage ? amount / 100.0D : amount;
			return switch (this) {
				case SUBTRACT_VALUE, SUBTRACT_PERCENTAGE_BASE, SUBTRACT_PERCENTAGE_TOTAL -> -Math.abs(converted);
				default -> converted;
			};
		}

		public static ResolvedOperation from(String raw) {
			if (raw == null || raw.isBlank()) {
				return ADD_VALUE;
			}
			String value = raw.trim().toLowerCase(Locale.ROOT);
			return switch (value) {
				case "add_value", "fixed", "addition" -> ADD_VALUE;
				case "subtract_value", "remove_value", "subtraction" -> SUBTRACT_VALUE;
				case "multiply_base" -> MULTIPLY_BASE;
				case "multiply_total" -> MULTIPLY_TOTAL;
				case "add_percentage_base", "percent_base_add", "percentage_base_add" -> ADD_PERCENTAGE_BASE;
				case "subtract_percentage_base", "percent_base_subtract", "percentage_base_subtract" -> SUBTRACT_PERCENTAGE_BASE;
				case "add_percentage_total", "percent_total_add", "percentage_total_add" -> ADD_PERCENTAGE_TOTAL;
				case "subtract_percentage_total", "percent_total_subtract", "percentage_total_subtract" -> SUBTRACT_PERCENTAGE_TOTAL;
				default -> null;
			};
		}
	}
}
