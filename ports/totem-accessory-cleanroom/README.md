# Totem Accessory 26.3 Compat

Clean-room compatibility implementation for Minecraft 26.3 + NeoForge.

Features:
- Adds one dedicated Ohmega accessory slot of type `totem`.
- Only Totems of Undying can be equipped in that slot.
- A Totem equipped there activates on lethal damage, is consumed, restores the player through Minecraft's vanilla DeathProtection component, and shows the vanilla Totem animation.
- Keeps Elytra and Totem in separate accessory slots.

Requires:
- Minecraft 26.3
- NeoForge 26.3.0.48-beta
- Ohmega 1.5.21
- Elytra Accessory 26.3 Compat 1.0.2 or newer (provides the Ohmega 1.5.21 SERVER -> SYNCED compatibility patch for NeoForge .48-beta)
