package org.bitwisemadness.enchantmentpacks.client.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import org.bitwisemadness.enchantmentpacks.EnchantmentPacksMod;
import org.bitwisemadness.enchantmentpacks.common.packs.EnchantmentPackManager;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

public class EnchantmentLangPackResources implements PackResources {
	private final String packId;
	private final byte[] packMcmetaBytes;
	private final byte[] enUsBytes;

	public EnchantmentLangPackResources(String packId) {
		this.packId = packId;
		JsonObject packInfo = new JsonObject();
		packInfo.addProperty("description", "Enchantment Packs Generated Translations");
		packInfo.addProperty("pack_format", 15);
		JsonObject root = new JsonObject();
		root.add("pack", packInfo);
		this.packMcmetaBytes = root.toString().getBytes(StandardCharsets.UTF_8);
		this.enUsBytes = EnchantmentPackManager.buildGeneratedEnchantmentTranslationsJsonBytes();
	}

	@Nullable
	@Override
	public IoSupplier<InputStream> getRootResource(String... elements) {
		if (elements.length > 0 && "pack.mcmeta".equals(elements[0])) {
			return () -> new ByteArrayInputStream(packMcmetaBytes);
		}
		return null;
	}

	@Nullable
	@Override
	public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
		if (type == PackType.CLIENT_RESOURCES && location.getNamespace().equals(EnchantmentPacksMod.MOD_ID) && location.getPath().equals("lang/en_us.json")) {
			return () -> new ByteArrayInputStream(enUsBytes);
		}
		return null;
	}

	@Override
	public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
		if (type == PackType.CLIENT_RESOURCES && EnchantmentPacksMod.MOD_ID.equals(namespace) && (path.isEmpty() || "lang".equals(path))) {
			output.accept(new ResourceLocation(EnchantmentPacksMod.MOD_ID, "lang/en_us.json"), () -> new ByteArrayInputStream(enUsBytes));
		}
	}

	@Override
	public Set<String> getNamespaces(PackType type) {
		return type == PackType.CLIENT_RESOURCES ? Set.of(EnchantmentPacksMod.MOD_ID) : Collections.emptySet();
	}

	@Nullable
	@Override
	public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
		try (InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(packMcmetaBytes), StandardCharsets.UTF_8)) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			String sectionName = deserializer.getMetadataSectionName();
			if (json.has(sectionName)) {
				return deserializer.fromJson(json.getAsJsonObject(sectionName));
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public String packId() {
		return packId;
	}

	@Override
	public void close() {}

	@Override
	public boolean isBuiltin() {
		return true;
	}
}
