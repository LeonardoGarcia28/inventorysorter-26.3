#!/usr/bin/env bash
set -euo pipefail

rm -rf work
mkdir -p work

git clone --depth 1 --branch neoforge-1.21.11 https://github.com/AkkynaA/slotlib.git work/slotlib
git clone --depth 1 --branch neoforge-1.21.11 https://github.com/AkkynaA/moreoffhandslots.git work/moreoffhandslots

python3 <<'PY'
from pathlib import Path
import re

# ---------- SlotLib ----------
root = Path("work/slotlib")
p = root / "gradle.properties"
s = p.read_text()
repls = {
    "minecraft_version=1.21.11": "minecraft_version=26.3",
    "minecraft_version_range=[1.21.11]": "minecraft_version_range=[26.3]",
    "neo_version=21.11.38-beta": "neo_version=26.3.0.48-beta",
    "neo_version_range=[21.11.0,)": "neo_version_range=[26.3.0.48-beta,)",
    "mod_version=21.11.0": "mod_version=26.3.0-unofficial.1",
}
for a,b in repls.items(): s=s.replace(a,b)
p.write_text(s)

p = root / "build.gradle"
s = p.read_text()
s = s.replace("id 'net.neoforged.moddev' version '2.0.140'", "id 'net.neoforged.moddev' version '2.0.141'")
s = s.replace("JavaLanguageVersion.of(21)", "JavaLanguageVersion.of(25)")
s = re.sub(r"\n\s*parchment \{.*?\n\s*\}\n", "\n", s, flags=re.S)
s = re.sub(r'^\s*compileOnly "top\.theillusivec4\.curios:curios-neoforge:[^"]+"\s*$', '', s, flags=re.M)
p.write_text(s)

p = root / "src/main/java/net/akkynaa/slotlib/SlotLib.java"
s = p.read_text().replace("ModConfig.Type.COMMON", "ModConfig.Type.LOCAL")
p.write_text(s)

# Temporarily drop optional Curios compile-time integration for 26.3.
curios = root / "src/main/java/net/akkynaa/slotlib/client/compat/CuriosCompat.java"
if curios.exists(): curios.unlink()

p = root / "src/main/java/net/akkynaa/slotlib/client/gui/GuiEventHandler.java"
p.write_text("""package net.akkynaa.slotlib.client.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

public class GuiEventHandler {
    @SubscribeEvent
    public void onInventoryGuiInit(ScreenEvent.Init.Post evt) {
        Screen screen = evt.getScreen();
        if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) {
            AbstractContainerScreen<?> gui = (AbstractContainerScreen<?>) screen;
            boolean isCreative = screen instanceof CreativeModeInventoryScreen;
            int size = isCreative ? 8 : 10;
            int[] pos = SlotLibButton.getButtonPosition(gui);
            evt.addListener(new SlotLibButton(gui, pos[0], pos[1], size, size,
                    isCreative ? SlotLibButton.SMALL : SlotLibButton.BIG));
        }
    }
}
""")

p = root / "src/main/java/net/akkynaa/slotlib/client/gui/SlotLibScreen.java"
s = p.read_text()
s = s.replace("import net.akkynaa.slotlib.client.compat.CuriosCompat;\n", "")
s = re.sub(r'\n\s*if \(ModList\.get\(\)\.isLoaded\("curios"\)\) \{\s*this\.addRenderableWidget\(CuriosCompat\.createCuriosButtonForSlotLibScreen\(this\)\);\s*\}', '', s, flags=re.S)
p.write_text(s)

# 26.3 input and GUI API changes.
p = root / "src/main/java/net/akkynaa/slotlib/client/KeyRegistry.java"
x = p.read_text()
x = x.replace("import org.lwjgl.glfw.GLFW;\n", "")
x = x.replace("GLFW.GLFW_KEY_G", "103 /* SDLK_g */")
p.write_text(x)

p = root / "src/main/java/net/akkynaa/slotlib/client/gui/SlotLibButton.java"
x = p.read_text()
x = x.replace("import net.minecraft.client.gui.GuiGraphics;", "import net.minecraft.client.gui.GuiGraphicsExtractor;")
x = x.replace("mc.setScreen(inventory);", "mc.gui.setScreen(inventory);")
x = x.replace("getGuiLeft()", "getLeftPos()").replace("getGuiTop()", "getTopPos()")
x = x.replace("renderContents(@Nonnull GuiGraphics guiGraphics", "extractContents(@Nonnull GuiGraphicsExtractor guiGraphics")
x = x.replace("super.renderContents(guiGraphics, mouseX, mouseY, partialTicks);", "super.extractContents(guiGraphics, mouseX, mouseY, partialTicks);")
p.write_text(x)

p = root / "src/main/java/net/akkynaa/slotlib/client/gui/SlotLibScreen.java"
x = p.read_text()
x = x.replace("import net.minecraft.client.gui.GuiGraphics;", "import net.minecraft.client.gui.GuiGraphicsExtractor;")
x = x.replace("GuiGraphics guiGraphics", "GuiGraphicsExtractor guiGraphics")
# 26.3 uses extraction rather than immediate rendering.
x = re.sub(r'\n\s*@Override\n\s*public void render\(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks\) \{.*?\n\s*\}\n(?=\s*@Override\n\s*protected void renderLabels)',
           '\n', x, flags=re.S)
x = x.replace("protected void renderLabels(", "protected void extractLabels(")
x = x.replace("guiGraphics.drawString(", "guiGraphics.text(")
x = x.replace("protected void renderBg(@Nonnull GuiGraphicsExtractor guiGraphics, float partialTicks, int mouseX, int mouseY)",
              "public void extractBackground(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks)")
x = x.replace("InventoryScreen.renderEntityInInventoryFollowsMouse(", "InventoryScreen.extractEntityInInventoryFollowsMouse(")
p.write_text(x)

p = root / "src/main/java/net/akkynaa/slotlib/common/inventory/container/SlotLibContainer.java"
x = p.read_text()
x = x.replace("import net.minecraft.world.item.ItemStack;", "import net.minecraft.world.item.ItemStack;\nimport net.minecraft.util.Prediction;")
x = x.replace("playerIn.drop(itemstack1, false);", "playerIn.drop(itemstack1, false, Prediction.SERVER_ONLY);")
p.write_text(x)

p = root / "src/main/java/net/akkynaa/slotlib/common/event/SlotLibEventHandler.java"
x = p.read_text()
x = x.replace("player.level().getServer().getWorldData().getGameRules().get(GameRules.KEEP_INVENTORY)",
              "player.level().getServer().getGameRules().get(GameRules.KEEP_INVENTORY)")
x = x.replace("livingEntity.level().random.nextFloat()", "livingEntity.level().getRandom().nextFloat()")
p.write_text(x)

# ---------- More Offhand Slots ----------
root = Path("work/moreoffhandslots")
p = root / "gradle.properties"
s = p.read_text()
repls = {
    "minecraft_version=1.21.11": "minecraft_version=26.3",
    "minecraft_version_range=[1.21.11]": "minecraft_version_range=[26.3]",
    "neo_version=21.11.38-beta": "neo_version=26.3.0.48-beta",
    "neo_version_range=[21.11.0,)": "neo_version_range=[26.3.0.48-beta,)",
    "mod_version=21.11.0.0": "mod_version=26.3.0.0-unofficial.1",
    "slotlib_version=neoforge-21.11.0": "slotlib_version=neoforge-26.3.0-unofficial.1",
}
for a,b in repls.items(): s=s.replace(a,b)
p.write_text(s)

p = root / "build.gradle"
p.write_text(r"""plugins {
    id 'java-library'
    id 'maven-publish'
    id 'net.neoforged.moddev' version '2.0.141'
}

version = mod_version
group = mod_group_id

repositories {
    mavenLocal()
    maven { url = 'https://maven.neoforged.net/releases' }
}

base {
    archivesName = "moreoffhandslots-neoforge"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

neoForge {
    version = project.neo_version
    runs {
        client { client() }
        server {
            server()
            programArgument '--nogui'
        }
    }

    mods {
        "moreoffhandslots" {
            sourceSet(sourceSets.main)
        }
    }
}

sourceSets.main.resources { srcDir 'src/generated/resources' }

configurations {
    runtimeClasspath.extendsFrom localRuntime
}

dependencies {
    implementation "net.akkynaa:slotlib:" + slotlib_version
}

tasks.withType(ProcessResources).configureEach {
    var replaceProperties = [
        minecraft_version: minecraft_version,
        minecraft_version_range: minecraft_version_range,
        neo_version: neo_version,
        mod_id: mod_id,
        mod_name: mod_name,
        mod_license: mod_license,
        mod_version: mod_version,
        mod_authors: mod_authors,
        mod_description: mod_description
    ]
    inputs.properties replaceProperties
    filesMatching(['META-INF/neoforge.mods.toml']) {
        expand replaceProperties
    }
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}
""")

p = root / "settings.gradle"
p.write_text("""pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = 'https://maven.neoforged.net/releases' }
    }
}
plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0'
}
""")

p = root / "src/main/java/net/akkynaa/moreoffhandslots/client/input/KeyBindings.java"
s = p.read_text()
s = s.replace("import org.lwjgl.glfw.GLFW;\n", "")
s = s.replace("GLFW.GLFW_MOUSE_BUTTON_5", "5 /* SDL_BUTTON_X2 */")
s = s.replace("GLFW.GLFW_MOUSE_BUTTON_4", "4 /* SDL_BUTTON_X1 */")
s = s.replace("GLFW.GLFW_KEY_LEFT_SHIFT", "InputConstants.KEY_LSHIFT")
s = s.replace("InputConstants.Type.KEYSYM", "InputConstants.Type.KEYBOARD")
p.write_text(s)

# Minecraft 26.3 moved screen ownership onto Minecraft.gui.
p = root / "src/main/java/net/akkynaa/moreoffhandslots/client/input/ScrollWheelHandler.java"
x = p.read_text().replace("minecraft.screen != null", "minecraft.gui.screen() != null")
p.write_text(x)

# Register the HUD layer with the 26.3 extraction graphics type.
p = root / "src/main/java/net/akkynaa/moreoffhandslots/MoreOffhandSlots.java"
x = p.read_text()
x = x.replace("import net.minecraft.client.gui.GuiGraphics;", "import net.minecraft.client.gui.GuiGraphicsExtractor;")
x = x.replace("(GuiGraphics guiGraphics, DeltaTracker deltaTracker)", "(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker)")
p.write_text(x)

# The old GuiMixin replaces vanilla's immediate-mode hotbar renderer, which no longer exists in 26.3.
# The port renders its own offhand HUD layer and leaves the vanilla hotbar untouched.
mixin = root / "src/main/java/net/akkynaa/moreoffhandslots/mixin/GuiMixin.java"
if mixin.exists():
    mixin.unlink()

p = root / "src/main/resources/moreoffhandslots.mixins.json"
x = p.read_text().replace('"client": [\n    "GuiMixin"\n  ]', '"client": []')
p.write_text(x)

# Keep the public renderer API, migrated to GuiGraphicsExtractor.
p = root / "src/main/java/net/akkynaa/moreoffhandslots/api/IOffhandHudRenderer.java"
p.write_text("""package net.akkynaa.moreoffhandslots.api;

import net.akkynaa.moreoffhandslots.client.render.OffhandHudRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface IOffhandHudRenderer {
    void renderOffhandHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);

    void renderHotbarStyleOffhand(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, LocalPlayer player,
                                  int screenWidth, int screenHeight, List<ItemStack> items);

    void renderDefaultStyleOffhand(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, LocalPlayer player,
                                   int screenWidth, int screenHeight, ItemStack prevItem,
                                   ItemStack currentItem, ItemStack nextItem);

    void renderDetailedStyleOffhand(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, LocalPlayer player,
                                    int screenWidth, int screenHeight, ItemStack prevItem,
                                    ItemStack currentItem, ItemStack nextItem);

    int getMiddleX(LocalPlayer player, int screenWidth);

    void renderItem(GuiGraphicsExtractor graphics, int x, int y, DeltaTracker deltaTracker,
                    Player player, ItemStack stack, boolean doDecoration, boolean doBounce);

    static void setOffhandHudRenderer(IOffhandHudRenderer renderer) {
        OffhandHudRenderer.setOffhandRenderer(renderer);
    }

    static IOffhandHudRenderer getOffhandHudRenderer() {
        return OffhandHudRenderer.getOffhandRenderer();
    }
}
""")

# Native 26.3 HUD implementation. It preserves all three mod HUD modes while using extraction rendering.
p = root / "src/main/java/net/akkynaa/moreoffhandslots/client/render/OffhandHudRenderer.java"
p.write_text("""package net.akkynaa.moreoffhandslots.client.render;

import net.akkynaa.moreoffhandslots.api.IOffhandHudRenderer;
import net.akkynaa.moreoffhandslots.api.OffhandInventory;
import net.akkynaa.moreoffhandslots.capability.OffhandRegistry;
import net.akkynaa.moreoffhandslots.client.config.ClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.List;
import java.util.Objects;

public final class OffhandHudRenderer implements IOffhandHudRenderer {
    private static final int ITEM_SIZE = 16;
    private static final int SLOT_SIZE = 20;
    private static final int HOTBAR_WIDTH = 182;
    private static final int HOTBAR_MARGIN = 25;

    private static int hotbarOffset;
    private static IOffhandHudRenderer instance = new OffhandHudRenderer();

    public static int getHotbarOffset() {
        return hotbarOffset;
    }

    private static void setHotbarOffset(int value) {
        hotbarOffset = value;
    }

    public static void setOffhandRenderer(IOffhandHudRenderer renderer) {
        instance = Objects.requireNonNull(renderer);
    }

    public static IOffhandHudRenderer getOffhandRenderer() {
        return instance;
    }

    @Override
    public void renderOffhandHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (ClientConfig.INDICATOR_STYLE.get() == ClientConfig.IndicatorStyle.VANILLA) return;
        if (mc.gui.hud.isHidden()) return;
        if (mc.gameMode == null || mc.gameMode.getPlayerMode() == GameType.SPECTATOR) return;

        Entity entity = mc.getCameraEntity();
        if (!(entity instanceof LocalPlayer player)) return;
        if (net.akkynaa.moreoffhandslots.compat.BetterCombatCompat.hasTwoHandedWeaponEquipped(player)) return;

        List<ItemStack> items = OffhandInventory.getOffhandItemsToRender(player);
        if (items.isEmpty()) return;

        ItemStack current = player.getItemInHand(InteractionHand.OFF_HAND);
        if (current.isEmpty() && !ClientConfig.RENDER_EMPTY_OFFHAND.get()) return;

        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        switch (ClientConfig.INDICATOR_STYLE.get()) {
            case HOTBAR -> renderHotbarStyleOffhand(graphics, deltaTracker, player, w, h, items);
            case DETAILED -> {
                List<ItemStack> processed = ClientConfig.EMPTY_SLOT_BEHAVIOR.get() == ClientConfig.EmptySlotBehavior.COLLAPSE
                        ? OffhandInventory.collapseConsecutiveEmpties(items) : items;
                ItemStack next = processed.size() > 1 ? processed.get(1) : processed.getFirst();
                ItemStack prev = processed.getLast();
                renderDetailedStyleOffhand(graphics, deltaTracker, player, w, h, prev, current, next);
            }
            case DEFAULT -> {
                List<ItemStack> processed = ClientConfig.EMPTY_SLOT_BEHAVIOR.get() == ClientConfig.EmptySlotBehavior.COLLAPSE
                        ? OffhandInventory.collapseConsecutiveEmpties(items) : items;
                ItemStack next = processed.size() > 1 ? processed.get(1) : processed.getFirst();
                ItemStack prev = processed.getLast();
                renderDefaultStyleOffhand(graphics, deltaTracker, player, w, h, prev, current, next);
            }
            default -> { }
        }
    }

    @Override
    public void renderHotbarStyleOffhand(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker,
                                         LocalPlayer player, int screenWidth, int screenHeight,
                                         List<ItemStack> items) {
        int width = items.size() * SLOT_SIZE;
        boolean right = player.getMainArm() == HumanoidArm.RIGHT;
        int center = screenWidth / 2;
        int x = right ? center - HOTBAR_WIDTH / 2 - 10 - width
                      : center + HOTBAR_WIDTH / 2 + 10;
        x += ClientConfig.X_OFFSET.get();
        int y = screenHeight - 22 + ClientConfig.Y_OFFSET.get();

        int selected = ClientConfig.EMPTY_SLOT_BEHAVIOR.get() == ClientConfig.EmptySlotBehavior.SKIP
                ? OffhandInventory.getRenderPosition(player)
                : player.getData(OffhandRegistry.OFFHAND_POSITION).getPosition();
        if (!items.isEmpty()) selected = Math.floorMod(selected, items.size());

        graphics.fill(x, y, x + width + 2, y + 22, 0x90000000);
        graphics.fill(x + selected * SLOT_SIZE, y, x + selected * SLOT_SIZE + SLOT_SIZE + 2, y + 2, 0xFFFFFFFF);
        graphics.fill(x + selected * SLOT_SIZE, y + 20, x + selected * SLOT_SIZE + SLOT_SIZE + 2, y + 22, 0xFFFFFFFF);

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            int itemPosition = (i + selected) % items.size();
            renderItem(graphics, x + itemPosition * SLOT_SIZE + 3, y + 3,
                    deltaTracker, player, stack, true, false);
        }

        if (ClientConfig.ALIGN_TO_CENTER.get()) {
            setHotbarOffset(right ? center - width / 2 : center + width / 2);
        } else {
            setHotbarOffset(center);
        }
    }

    @Override
    public void renderDefaultStyleOffhand(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker,
                                          LocalPlayer player, int screenWidth, int screenHeight,
                                          ItemStack prevItem, ItemStack currentItem, ItemStack nextItem) {
        int middle = getMiddleX(player, screenWidth) + ClientConfig.X_OFFSET.get();
        int y = screenHeight - ITEM_SIZE - 3 + ClientConfig.Y_OFFSET.get();
        graphics.fill(middle - 23, y - 2, middle + 39, y + 18, 0x70000000);
        renderItem(graphics, middle - 20, y, deltaTracker, player, prevItem, false, false);
        renderItem(graphics, middle, y, deltaTracker, player, currentItem, true, false);
        renderItem(graphics, middle + 20, y, deltaTracker, player, nextItem, false, false);
    }

    @Override
    public void renderDetailedStyleOffhand(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker,
                                           LocalPlayer player, int screenWidth, int screenHeight,
                                           ItemStack prevItem, ItemStack currentItem, ItemStack nextItem) {
        int middle = getMiddleX(player, screenWidth) + ClientConfig.X_OFFSET.get();
        int y = screenHeight - ITEM_SIZE - 3 + ClientConfig.Y_OFFSET.get();
        graphics.fill(middle - 24, y - 3, middle + 40, y + 19, 0x90000000);
        graphics.fill(middle - 2, y - 3, middle + 18, y - 1, 0xFFFFFFFF);
        graphics.fill(middle - 2, y + 17, middle + 18, y + 19, 0xFFFFFFFF);
        renderItem(graphics, middle - 20, y, deltaTracker, player, prevItem, true, false);
        renderItem(graphics, middle, y, deltaTracker, player, currentItem, true, false);
        renderItem(graphics, middle + 20, y, deltaTracker, player, nextItem, true, false);
    }

    @Override
    public int getMiddleX(LocalPlayer player, int screenWidth) {
        int center = screenWidth / 2;
        if (player.getMainArm() == HumanoidArm.RIGHT) {
            return center - HOTBAR_WIDTH / 2 - HOTBAR_MARGIN - 29;
        }
        return center + HOTBAR_WIDTH / 2 + HOTBAR_MARGIN + 13;
    }

    @Override
    public void renderItem(GuiGraphicsExtractor graphics, int x, int y, DeltaTracker deltaTracker,
                           Player player, ItemStack stack, boolean doDecoration, boolean doBounce) {
        if (stack.isEmpty()) return;
        graphics.item(player, stack, x, y, 0);
        if (doDecoration) {
            graphics.itemDecorations(Minecraft.getInstance().font, stack, x, y);
        }
    }
}
""")

# Explicit separate SlotLib dependency for the port build.
p = root / "src/main/resources/META-INF/neoforge.mods.toml"
s = p.read_text()
if "modId=\"slotlib\"" not in s:
    s += """
[[dependencies.moreoffhandslots]]
modId="slotlib"
type="required"
versionRange="[26.3.0-unofficial.1,)"
ordering="AFTER"
side="BOTH"
"""
p.write_text(s)
PY

echo "=== Building SlotLib ==="
( cd work/slotlib && gradle clean publishToMavenLocal --stacktrace )

echo "=== Building More Offhand Slots ==="
( cd work/moreoffhandslots && gradle clean build --stacktrace )

mkdir -p artifacts
cp work/slotlib/build/libs/*.jar artifacts/ || true
cp work/moreoffhandslots/build/libs/*.jar artifacts/ || true
ls -lah artifacts
