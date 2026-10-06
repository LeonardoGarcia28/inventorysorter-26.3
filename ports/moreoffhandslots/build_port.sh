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
s = p.read_text()
s = s.replace("import net.akkynaa.slotlib.client.compat.CuriosCompat;\n", "")
s = re.sub(r'\s*else if \(ModList\.get\(\)\.isLoaded\("curios"\).*?\n\s*\}', '\n        }', s, flags=re.S)
p.write_text(s)

p = root / "src/main/java/net/akkynaa/slotlib/client/gui/SlotLibScreen.java"
s = p.read_text()
s = s.replace("import net.akkynaa.slotlib.client.compat.CuriosCompat;\n", "")
s = re.sub(r'\n\s*if \(ModList\.get\(\)\.isLoaded\("curios"\)\) \{\s*this\.addRenderableWidget\(CuriosCompat\.createCuriosButtonForSlotLibScreen\(this\)\);\s*\}', '', s, flags=re.S)
p.write_text(s)

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
