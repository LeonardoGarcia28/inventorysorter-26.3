"""Always display configured offhand slots in the 26.3 HUD.

Runs after the original source migration in the GitHub Actions build.
Slot visibility is independent from EMPTY_SLOT_BEHAVIOR, which keeps controlling cycling.
"""
from pathlib import Path

repo = Path(__file__).resolve().parents[1]
root = repo / "port-build"
renderer = root / "moreoffhandslots/src/main/java/net/akkynaa/moreoffhandslots/client/render/OffhandHudRenderer.java"
config = root / "moreoffhandslots/src/main/java/net/akkynaa/moreoffhandslots/client/config/ClientConfig.java"

def replace_one(source, before, after, name):
    count = source.count(before)
    if count != 1:
        raise RuntimeError(f"{name}: expected 1 occurrence, found {count}")
    return source.replace(before, after, 1)

s = renderer.read_text(encoding="utf-8")
s = replace_one(s,
    """        List<ItemStack> cycleItems = OffhandInventory.getOffhandItemsToRender(player);
        if (cycleItems.isEmpty()) return;

        ItemStack currentItem = player.getItemInHand(InteractionHand.OFF_HAND);
        if (currentItem.isEmpty() && !ClientConfig.RENDER_EMPTY_OFFHAND.get()) return;""",
    """        // Draw the indicator even when the active offhand and all extra slots
        // are empty. F1, spectator mode and compatibility rules still apply.
        ItemStack currentItem = player.getItemInHand(InteractionHand.OFF_HAND);
        List<ItemStack> cycleItems = OffhandInventory.getOffhandItemsToRender(player);""",
    "offhand visibility")
s = replace_one(s,
    """            case HOTBAR -> renderHotbarStyleOffhand(guiGraphics, deltaTracker, player,
                    screenWidth, screenHeight, cycleItems);""",
    """            // Always draw every configured position, including empty ones.
            // SKIP affects keybind cycling, not how many slots are visible.
            case HOTBAR -> renderHotbarStyleOffhand(guiGraphics, deltaTracker, player,
                    screenWidth, screenHeight, OffhandInventory.getAllOffhandItems(player));""",
    "full hotbar")
s = replace_one(s,
    """        int currentIndex;
        if (ClientConfig.EMPTY_SLOT_BEHAVIOR.get() != ClientConfig.EmptySlotBehavior.SKIP) { // COLLAPSE OR CYCLE
            currentIndex = player.getData(OffhandRegistry.OFFHAND_POSITION).getPosition();
        } else { //SKIP
            currentIndex = OffhandInventory.getRenderPosition(player);
        }""",
    """        // Full-width bar: the highlighted slot uses physical slot indices
        // regardless of whether empty slots are skipped during cycling.
        int currentIndex = Math.floorMod(
                player.getData(OffhandRegistry.OFFHAND_POSITION).getPosition(), renderSize);""",
    "selection index")
assert "getAllOffhandItems(player)" in s
assert "!ClientConfig.RENDER_EMPTY_OFFHAND.get()) return;" not in s
renderer.write_text(s, encoding="utf-8")

# Retain this deprecated config key for old profiles, but remove the misleading description.
c = config.read_text(encoding="utf-8")
c = replace_one(c,
    '                .comment("Whether to render the offhand slots when empty items are in them. (will only take effect if emptySlotBehavior is not SKIP)")',
    '                .comment("Legacy option retained for saved profiles. The offhand HUD is always shown, even if empty.")',
    "legacy config comment")
config.write_text(c, encoding="utf-8")
print("Always-visible offhand HUD migration applied")
