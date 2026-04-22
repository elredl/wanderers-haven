package com.wanderershaven.item;

import com.wanderershaven.WanderersHavenMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

/** Registers custom weapon items. */
public final class ModItems {

	public static final Item IRON_RAPIER = registerWeapon("iron_rapier", ToolMaterial.IRON, 5.0f, 1.8f);
	public static final Item GOLD_RAPIER = registerWeapon("gold_rapier", ToolMaterial.GOLD, 5.0f, 1.8f);
	public static final Item DIAMOND_RAPIER = registerWeapon("diamond_rapier", ToolMaterial.DIAMOND, 5.0f, 1.8f);
	public static final Item NETHERITE_RAPIER = registerWeapon("netherite_rapier", ToolMaterial.NETHERITE, 5.0f, 1.8f);

	public static final Item IRON_TWINBLADES = registerWeapon("iron_twinblades", ToolMaterial.IRON, 4.0f, 2.0f);
	public static final Item GOLD_TWINBLADES = registerWeapon("gold_twinblades", ToolMaterial.GOLD, 4.0f, 2.0f);
	public static final Item DIAMOND_TWINBLADES = registerWeapon("diamond_twinblades", ToolMaterial.DIAMOND, 4.0f, 2.0f);
	public static final Item NETHERITE_TWINBLADES = registerWeapon("netherite_twinblades", ToolMaterial.NETHERITE, 4.0f, 2.0f);

	public static final Item IRON_GREATSWORD = registerWeapon("iron_greatsword", ToolMaterial.IRON, 9.0f, 1.4f);
	public static final Item GOLD_GREATSWORD = registerWeapon("gold_greatsword", ToolMaterial.GOLD, 9.0f, 1.4f);
	public static final Item DIAMOND_GREATSWORD = registerWeapon("diamond_greatsword", ToolMaterial.DIAMOND, 9.0f, 1.4f);
	public static final Item NETHERITE_GREATSWORD = registerWeapon("netherite_greatsword", ToolMaterial.NETHERITE, 9.0f, 1.4f);

	public static final Item IRON_GREATHAMMER = registerWeapon("iron_greathammer", ToolMaterial.IRON, 11.0f, 1.2f);
	public static final Item STONE_GREATHAMMER = registerWeapon("stone_greathammer", ToolMaterial.STONE, 11.0f, 1.2f);
	public static final Item GOLD_GREATHAMMER = registerWeapon("gold_greathammer", ToolMaterial.GOLD, 11.0f, 1.2f);
	public static final Item DIAMOND_GREATHAMMER = registerWeapon("diamond_greathammer", ToolMaterial.DIAMOND, 11.0f, 1.2f);
	public static final Item NETHERITE_GREATHAMMER = registerWeapon("netherite_greathammer", ToolMaterial.NETHERITE, 11.0f, 1.2f);

	public static final Item IRON_SCYTHE = registerWeapon("iron_scythe", ToolMaterial.IRON, 8.0f, 1.3f);
	public static final Item GOLD_SCYTHE = registerWeapon("gold_scythe", ToolMaterial.GOLD, 8.0f, 1.3f);
	public static final Item DIAMOND_SCYTHE = registerWeapon("diamond_scythe", ToolMaterial.DIAMOND, 8.0f, 1.3f);
	public static final Item NETHERITE_SCYTHE = registerWeapon("netherite_scythe", ToolMaterial.NETHERITE, 8.0f, 1.3f);

	public static final Item IRON_MACE = registerWeapon("iron_mace", ToolMaterial.IRON, 7.0f, 1.5f);
	public static final Item GOLD_MACE = registerWeapon("gold_mace", ToolMaterial.GOLD, 7.0f, 1.5f);
	public static final Item DIAMOND_MACE = registerWeapon("diamond_mace", ToolMaterial.DIAMOND, 7.0f, 1.5f);
	public static final Item NETHERITE_MACE = registerWeapon("netherite_mace", ToolMaterial.NETHERITE, 7.0f, 1.5f);
	
	public static final Item QUIVER = registerQuiver("quiver");
	public static final Item TWIN_CROSSBOW = registerTwinCrossbow("twin_crossbow");
	public static final Item SHORTBOW_BUCKLER = registerShortbowBuckler("shortbow_buckler");

	private ModItems() {}

	public static void register() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
			entries.accept(IRON_RAPIER);
			entries.accept(GOLD_RAPIER);
			entries.accept(DIAMOND_RAPIER);
			entries.accept(NETHERITE_RAPIER);
			entries.accept(IRON_TWINBLADES);
			entries.accept(GOLD_TWINBLADES);
			entries.accept(DIAMOND_TWINBLADES);
			entries.accept(NETHERITE_TWINBLADES);
			entries.accept(IRON_GREATSWORD);
			entries.accept(GOLD_GREATSWORD);
			entries.accept(DIAMOND_GREATSWORD);
			entries.accept(NETHERITE_GREATSWORD);
			entries.accept(IRON_GREATHAMMER);
			entries.accept(STONE_GREATHAMMER);
			entries.accept(GOLD_GREATHAMMER);
			entries.accept(DIAMOND_GREATHAMMER);
			entries.accept(NETHERITE_GREATHAMMER);
			entries.accept(IRON_SCYTHE);
			entries.accept(GOLD_SCYTHE);
			entries.accept(DIAMOND_SCYTHE);
			entries.accept(NETHERITE_SCYTHE);
			entries.accept(IRON_MACE);
			entries.accept(GOLD_MACE);
			entries.accept(DIAMOND_MACE);
			entries.accept(NETHERITE_MACE);
			entries.accept(QUIVER);
			entries.accept(TWIN_CROSSBOW);
			entries.accept(SHORTBOW_BUCKLER);
		});
		WanderersHavenMod.LOGGER.info("Registered custom items");
	}

	private static Item registerWeapon(String id, ToolMaterial material, float damage, float attacksPerSecond) {
		Identifier itemId = Identifier.fromNamespaceAndPath(WanderersHavenMod.MOD_ID, id);
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, itemId);
		Item item = new AxeItem(material, damage, toAttackSpeedModifier(attacksPerSecond), new Item.Properties().setId(key));
		return Registry.register(BuiltInRegistries.ITEM, itemId, item);
	}

	private static Item registerQuiver(String id) {
		Identifier itemId = Identifier.fromNamespaceAndPath(WanderersHavenMod.MOD_ID, id);
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, itemId);
		Item item = new QuiverItem(new Item.Properties().stacksTo(1).setId(key));
		return Registry.register(BuiltInRegistries.ITEM, itemId, item);
	}

	private static Item registerTwinCrossbow(String id) {
		Identifier itemId = Identifier.fromNamespaceAndPath(WanderersHavenMod.MOD_ID, id);
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, itemId);
		Item item = new TwinCrossbowItem(new Item.Properties().stacksTo(1).durability(465).setId(key));
		return Registry.register(BuiltInRegistries.ITEM, itemId, item);
	}

	private static Item registerShortbowBuckler(String id) {
		Identifier itemId = Identifier.fromNamespaceAndPath(WanderersHavenMod.MOD_ID, id);
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, itemId);
		Item item = new ShortbowBucklerItem(
			ToolMaterial.IRON,
			3.0f,
			toAttackSpeedModifier(0.6f),
			new Item.Properties().stacksTo(1).durability(430).setId(key)
		);
		return Registry.register(BuiltInRegistries.ITEM, itemId, item);
	}

	private static float toAttackSpeedModifier(float attacksPerSecond) {
		return attacksPerSecond - 4.0f;
	}
}
