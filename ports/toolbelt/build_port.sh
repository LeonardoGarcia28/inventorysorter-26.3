#!/usr/bin/env bash
set -euo pipefail

rm -rf work/toolbelt artifacts
mkdir -p work artifacts

git clone --depth 1 --branch master https://github.com/gigaherz/ToolBelt.git work/toolbelt

python3 <<'PY'
from pathlib import Path
import re

root = Path("work/toolbelt")

# Build target / release identity
p = root / "build.gradle"
s = p.read_text()
s = s.replace('neoForge.version = "26.2.0.7-beta"', 'neoForge.version = "26.3.0.48-beta"')
s = s.replace('version = "2.10.0"', 'version = "2.10.0-26.3-unofficial.1"')
s = s.replace('artifactId project.archivesBaseName', 'artifactId base.archivesName.get()')
p.write_text(s)

# Metadata
p = root / "src/main/resources/META-INF/neoforge.mods.toml"
s = p.read_text().replace('versionRange="[26.2,)"', 'versionRange="[26.3,)"')
p.write_text(s)

# Config enum changes in 26.3
p = root / "src/main/java/dev/gigaherz/toolbelt/ToolBelt.java"
s = p.read_text()
s = s.replace("ModConfig.Type.SERVER", "ModConfig.Type.SYNCED")
s = s.replace("ModConfig.Type.COMMON", "ModConfig.Type.LOCAL")
p.write_text(s)

# GLFW -> SDL / InputConstants
p = root / "src/main/java/dev/gigaherz/toolbelt/client/ToolBeltClient.java"
s = p.read_text()
s = s.replace("import org.lwjgl.glfw.GLFW;\n", "import org.lwjgl.sdl.SDLMouse;\nimport org.lwjgl.system.MemoryStack;\n")
s = s.replace("GLFW.GLFW_KEY_R", "InputConstants.KEY_R")
s = s.replace("GLFW.GLFW_KEY_V", "InputConstants.KEY_V")
s = s.replace("case KEYSYM ->\n                    InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), keybind.getKey().getValue());",
              "case KEYBOARD ->\n                    InputConstants.isKeyDown(keybind.getKey().getValue());")
s = s.replace("case MOUSE ->\n                    GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), keybind.getKey().getValue()) == GLFW.GLFW_PRESS;",
              "case MOUSE -> isMouseButtonDown(keybind.getKey().getValue());")
marker = "    public static boolean isKeyDown(KeyMapping keybind)\n"
helper = """    private static boolean isMouseButtonDown(int button)\n    {\n        try (MemoryStack stack = MemoryStack.stackPush())\n        {\n            var x = stack.mallocFloat(1);\n            var y = stack.mallocFloat(1);\n            int mask = SDLMouse.SDL_GetMouseState(x, y);\n            return button > 0 && (mask & (1 << (button - 1))) != 0;\n        }\n    }\n\n"""
if marker in s and "isMouseButtonDown(int button)" not in s:
    s = s.replace(marker, helper + marker)
p.write_text(s)

# Radial menu native cursor operations: GLFW -> SDL3
p = root / "src/main/java/dev/gigaherz/toolbelt/client/radial/GenericRadialMenu.java"
s = p.read_text()
s = s.replace("import org.lwjgl.glfw.GLFW;", "import org.lwjgl.sdl.SDLMouse;")
s = s.replace("GLFW.glfwSetCursorPos(mainWindow.handle(), (int) (x * mainWindow.getScreenWidth() / owner.width), (int) (y * mainWindow.getScreenHeight() / owner.height));",
              "SDLMouse.SDL_WarpMouseInWindow(mainWindow.handle(), (float) (x * mainWindow.getScreenWidth() / owner.width), (float) (y * mainWindow.getScreenHeight() / owner.height));")
old = """            double[] xPos = new double[1];\n            double[] yPos = new double[1];\n            GLFW.glfwGetCursorPos(mainWindow.handle(), xPos, yPos);\n\n            double scaledX = xPos[0] - (windowWidth / 2.0f);\n            double scaledY = yPos[0] - (windowHeight / 2.0f);"""
new = """            double scaledX = minecraft.mouseHandler.xpos() - (windowWidth / 2.0f);\n            double scaledY = minecraft.mouseHandler.ypos() - (windowHeight / 2.0f);"""
s = s.replace(old, new)
s = s.replace("GLFW.glfwSetCursorPos(mainWindow.handle(), (int) (windowWidth / 2 + fixedX), (int) (windowHeight / 2 + fixedY));",
              "SDLMouse.SDL_WarpMouseInWindow(mainWindow.handle(), (float) (windowWidth / 2 + fixedX), (float) (windowHeight / 2 + fixedY));")
p.write_text(s)

# Additional 26.3 API migrations discovered by compiler

# Drop optional Curios integration until a 26.3-compatible Curios API is available.
curios_finder = root / "src/main/java/dev/gigaherz/toolbelt/BeltFinderCurios.java"
if curios_finder.exists():
    curios_finder.unlink()

p = root / "src/main/java/dev/gigaherz/toolbelt/ToolBelt.java"
s = p.read_text()
s = re.sub(r'\n\s*if \(ModList\.get\(\)\.isLoaded\("curios"\)\)\s*\{\s*BeltFinderCurios\.initCurios\(\);\s*\}', '', s, flags=re.S)

# Data-generation APIs changed substantially in 26.3. Runtime resources are already generated
# in the upstream repository, so keep runtime behavior and disable only the datagen implementation.
marker = "    public static class DataGen"
idx = s.find(marker)
if idx >= 0:
    brace = s.find("{", idx)
    depth = 0
    end = None
    for i in range(brace, len(s)):
        if s[i] == "{":
            depth += 1
        elif s[i] == "}":
            depth -= 1
            if depth == 0:
                end = i
                break
    if end is not None:
        replacement = """    public static class DataGen
    {
        public static void gatherData(GatherDataEvent.Client event)
        {
            // Runtime port: generated resources are already bundled upstream.
        }
    }"""
        s = s[:idx] + replacement + s[end+1:]
p.write_text(s)

# RenderPipeline moved to RenderPearl in 26.3.
p = root / "src/main/java/dev/gigaherz/toolbelt/client/radial/GenericRadialMenu.java"
s = p.read_text().replace("import com.mojang.blaze3d.pipeline.RenderPipeline;",
                          "import com.mojang.renderpearl.api.pipeline.RenderPipeline;")
p.write_text(s)

p = root / "src/main/java/dev/gigaherz/toolbelt/slot/BeltSlotScreen.java"
s = p.read_text().replace("import com.mojang.blaze3d.pipeline.RenderPipeline;\n", "")
p.write_text(s)

# Player inventory/drop prediction API.
p = root / "src/main/java/dev/gigaherz/toolbelt/slot/BeltAttachment.java"
s = p.read_text()
if "import net.minecraft.util.Prediction;" not in s:
    s = s.replace("import net.minecraft.server.level.ServerPlayer;\n",
                  "import net.minecraft.server.level.ServerPlayer;\nimport net.minecraft.util.Prediction;\n")
s = s.replace("player.drop(stack, true, false);", "player.drop(stack, true, Prediction.SERVER_ONLY);")
s = s.replace("player.getInventory().placeItemBackInInventory(stack, true);",
              "player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);")
p.write_text(s)

p = root / "src/main/java/dev/gigaherz/toolbelt/slot/BeltSlotMenu.java"
s = p.read_text()
if "import net.minecraft.util.Prediction;" not in s:
    s = s.replace("import net.minecraft.server.level.ServerLevel;\n",
                  "import net.minecraft.server.level.ServerLevel;\nimport net.minecraft.util.Prediction;\n")
s = s.replace("player.drop(slotContents, false);",
              "player.drop(slotContents, false, Prediction.SERVER_ONLY);")
p.write_text(s)

# Render-state and PoseStack changes.
p = root / "src/main/java/dev/gigaherz/toolbelt/client/ToolBeltLayer.java"
s = p.read_text()
s = s.replace("avatarState.attackArm == HumanoidArm.RIGHT",
              "avatarState.mainArm == HumanoidArm.RIGHT")
s = s.replace("poseStack.mulPose(Axis.XP.rotationDegrees(40));",
              "poseStack.rotate(Axis.XP.rotationDegrees(40));")
p.write_text(s)

PY

cd work/toolbelt
gradle clean build --stacktrace
cd ../..

cp work/toolbelt/build/libs/*.jar artifacts/
ls -lah artifacts
