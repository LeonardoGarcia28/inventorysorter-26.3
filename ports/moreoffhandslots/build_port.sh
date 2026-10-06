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
x = x.replace("protected void renderBg(", "public void extractBackground(")
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
s = p.read_text()
s = s.replace("repositories {", "repositories {\n    mavenLocal()", 1)
s = s.replace("JavaLanguageVersion.of(21)", "JavaLanguageVersion.of(25)")
s = re.sub(r'^\s*jarJar\("net\.akkynaa:slotlib:\$\{slotlib_version\}"\)\s*$', '', s, flags=re.M)
s = re.sub(r'\njarJar\.enable\(\).*?tasks\.named\(\'jar\'\) \{\s*archiveClassifier = \'slim\'\s*\}\n', '\n', s, flags=re.S)
p.write_text(s)

p = root / "src/main/java/net/akkynaa/moreoffhandslots/client/input/KeyBindings.java"
s = p.read_text()
s = s.replace("import org.lwjgl.glfw.GLFW;\n", "")
s = s.replace("GLFW.GLFW_MOUSE_BUTTON_5", "5 /* SDL_BUTTON_X2 */")
s = s.replace("GLFW.GLFW_MOUSE_BUTTON_4", "4 /* SDL_BUTTON_X1 */")
s = s.replace("GLFW.GLFW_KEY_LEFT_SHIFT", "1073742049 /* SDLK_LSHIFT */")
p.write_text(s)

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
