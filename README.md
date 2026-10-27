# Enchantment Packs

Enchantment Packs is a data-driven Forge mod for Minecraft 1.20.1 that dynamically registers player attribute enchantments from external JSON packs.

## Features
- External folder and zip loading
- Merge and replace behavior per enchantment id
- Fixed-value and percentage-based attribute modifiers
- Conditional loading based on loaded mods
- Generated English translations for loaded enchantments

## Folder Structure
`.minecraft/enchantmentpacks/<pack>/data/enchantmentpacks/enchantments/*.json`

## Supported modifier operations
- `add_value`
- `subtract_value`
- `multiply_base`
- `multiply_total`
- `add_percentage_base`
- `subtract_percentage_base`
- `add_percentage_total`
- `subtract_percentage_total`

Percentage operations use whole percentages in JSON. For example, `15` means `+15%`, and `-10` means `-10%`.

## Example
```json
{
  "id": "battle_instinct",
  "display_name": "Battle Instinct",
  "description": "+Max Health and +Attack Damage while worn",
  "category": "armor_chest",
  "slots": ["chest"],
  "max_level": 3,
  "rarity": "rare",
  "modifiers": [
    {
      "attribute": "minecraft:generic.max_health",
      "operation": "add_value",
      "amount": 2.0,
      "amount_per_level": 2.0
    },
    {
      "attribute": "minecraft:generic.attack_damage",
      "operation": "add_percentage_total",
      "amount": 5.0,
      "amount_per_level": 5.0
    }
  ]
}
```

## Notes
- Enchantments are applied from equipped items only.
- Managed attribute modifiers refresh automatically when equipment changes and on a short player tick interval.
- Negative values are supported for decreases.

## AI disclosure

Parts of this project were written with the assistance of AI coding tools.
AI-assisted code, data and documentation ship only after human review by the
maintainers.
