#!/usr/bin/env bash
set -euo pipefail

rm -rf work/archerythings artifacts
mkdir -p work artifacts

git clone --depth 1 --branch 26.3 https://github.com/CoolerProYT/Archery-Things.git work/archerythings

python3 <<'PY'
from pathlib import Path

root = Path("work/archerythings")

# Target the exact NeoForge build used by the user's instance and mark this build.
p = root / "gradle.properties"
s = p.read_text()
s = s.replace("version=26.3.0.0", "version=26.3.0.0-ohmega.6")
s = s.replace("neoforge_version=26.3.0.1-beta", "neoforge_version=26.3.0.48-beta")
p.write_text(s)

# Compile against Ohmega's public API without bundling it.
p = root / "neoforge/build.gradle"
s = p.read_text()
needle = 'dependencies {\n'
if 'ohmega-neoforge' not in s:
    s = s.replace(
        needle,
        needle + '    compileOnly "io.github.swackyy:ohmega-neoforge:1.5.21-mc26.3"\n'
    )
p.write_text(s)

# Ohmega integration: bind Quiver as a Utility accessory and expose it to
# Archery Things' existing IQuiverHelper abstraction.
compat = root / "neoforge/src/main/java/com/coolerpromc/archerythings/compat/OhmegaHelper.java"
compat.parent.mkdir(parents=True, exist_ok=True)
compat.write_text(r'''package com.coolerpromc.archerythings.compat;

import com.coolerpromc.archerythings.item.ModItems;
import com.swacky.ohmega.api.AccessoryHelper;
import com.swacky.ohmega.api.IAccessory;
import com.swacky.ohmega.api.event.AccessoryOverrideTypesEvent;
import com.swacky.ohmega.common.accessorytype.AccessoryType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;

public final class OhmegaHelper {
    private static final IAccessory QUIVER_ACCESSORY = new IAccessory() {};

    private OhmegaHelper() {}

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(OhmegaHelper::onOverrideAccessoryTypes);
    }

    private static void onOverrideAccessoryTypes(AccessoryOverrideTypesEvent event) {
        Item quiver = ModItems.QUIVER.get();

        if (!AccessoryHelper.isItemAccessoryBound(quiver)) {
            AccessoryHelper.bindAccessory(quiver, QUIVER_ACCESSORY);
        }

        event.overrideRemaps.put(quiver, AccessoryType.UTILITY.get());
    }

    public static boolean isQuiverEquipped(Player player) {
        for (ItemStack stack : AccessoryHelper.getAccessoryStacks(player)) {
            if (stack.is(ModItems.QUIVER.get())) {
                return true;
            }
        }

        return false;
    }

    public static ItemStack getQuiver(Player player) {
        for (ItemStack stack : AccessoryHelper.getAccessoryStacks(player)) {
            if (stack.is(ModItems.QUIVER.get())) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }
}
''')

# Register the Ohmega listener only when Ohmega is installed.
p = root / "neoforge/src/main/java/com/coolerpromc/archerythings/ArcheryThings.java"
s = p.read_text()
if "import com.coolerpromc.archerythings.compat.OhmegaHelper;" not in s:
    s = s.replace(
        "package com.coolerpromc.archerythings;\n",
        "package com.coolerpromc.archerythings;\n\nimport com.coolerpromc.archerythings.compat.OhmegaHelper;\n"
    )
if "import net.neoforged.fml.ModList;" not in s:
    s = s.replace(
        "import net.neoforged.fml.common.Mod;\n",
        "import net.neoforged.fml.ModList;\nimport net.neoforged.fml.common.Mod;\n"
    )
old = """        NeoForgeRegistryHelper.register(modEventBus);

        modEventBus.addListener(this::onRegisterPayloadHandlers);"""
new = """        NeoForgeRegistryHelper.register(modEventBus);

        if (ModList.get().isLoaded("ohmega")) {
            OhmegaHelper.init(modEventBus);
        }

        modEventBus.addListener(this::onRegisterPayloadHandlers);"""
s = s.replace(old, new)
p.write_text(s)

# Teach the existing Quiver helper to read Ohmega accessory slots.
p = root / "neoforge/src/main/java/com/coolerpromc/archerythings/platform/NeoForgeQuiverHelper.java"
s = p.read_text()
if "import com.coolerpromc.archerythings.compat.OhmegaHelper;" not in s:
    s = s.replace(
        "import com.coolerpromc.archerythings.compat.CuriosHelper;\n",
        "import com.coolerpromc.archerythings.compat.CuriosHelper;\nimport com.coolerpromc.archerythings.compat.OhmegaHelper;\n"
    )
s = s.replace(
"""        return (Services.PLATFORM.isModLoaded("curios") && CuriosHelper.isQuiverEquipped(player)) || isQuiverEquippedCommon(player);""",
"""        return (Services.PLATFORM.isModLoaded("ohmega") && OhmegaHelper.isQuiverEquipped(player))
                || (Services.PLATFORM.isModLoaded("curios") && CuriosHelper.isQuiverEquipped(player))
                || isQuiverEquippedCommon(player);"""
)
s = s.replace(
"""        if (isQuiverEquippedCommon(player)) return getQuiverCommon(player);
        return CuriosHelper.getQuiver(player);""",
"""        if (Services.PLATFORM.isModLoaded("ohmega") && OhmegaHelper.isQuiverEquipped(player)) {
            return OhmegaHelper.getQuiver(player);
        }
        if (isQuiverEquippedCommon(player)) return getQuiverCommon(player);
        if (Services.PLATFORM.isModLoaded("curios")) return CuriosHelper.getQuiver(player);
        return ItemStack.EMPTY;"""
)
p.write_text(s)

# Declare Ohmega as optional integration metadata.
p = root / "neoforge/src/main/resources/META-INF/neoforge.mods.toml"
s = p.read_text()
if 'modId = "ohmega"' not in s:
    s += '''

[[dependencies.archerythings]]
modId = "ohmega"
type = "required"
versionRange = "[1.5.21,)"
ordering = "AFTER"
side = "BOTH"
'''
p.write_text(s)
# NeoForge 26.3.0.48 removed the old IItemHandler API used by Archery Things'
# optional Curios bridge. This Ohmega build does not need Curios, so drop only
# that optional integration while keeping Trinkets + Ohmega support.
p = root / "neoforge/build.gradle"
s = p.read_text()
s = s.replace('    compileOnly "top.theillusivec4.curios:curios-neoforge:16.0.0+26.2:api"\n', '')
p.write_text(s)

curios = root / "neoforge/src/main/java/com/coolerpromc/archerythings/compat/CuriosHelper.java"
if curios.exists():
    curios.unlink()

p = root / "neoforge/src/main/java/com/coolerpromc/archerythings/platform/NeoForgeQuiverHelper.java"
s = p.read_text()
s = s.replace("import com.coolerpromc.archerythings.compat.CuriosHelper;\n", "")
s = s.replace(
"""        return (Services.PLATFORM.isModLoaded("ohmega") && OhmegaHelper.isQuiverEquipped(player))
                || (Services.PLATFORM.isModLoaded("curios") && CuriosHelper.isQuiverEquipped(player))
                || isQuiverEquippedCommon(player);""",
"""        return (Services.PLATFORM.isModLoaded("ohmega") && OhmegaHelper.isQuiverEquipped(player))
                || isQuiverEquippedCommon(player);"""
)
s = s.replace(
"""        if (Services.PLATFORM.isModLoaded("ohmega") && OhmegaHelper.isQuiverEquipped(player)) {
            return OhmegaHelper.getQuiver(player);
        }
        if (isQuiverEquippedCommon(player)) return getQuiverCommon(player);
        if (Services.PLATFORM.isModLoaded("curios")) return CuriosHelper.getQuiver(player);
        return ItemStack.EMPTY;""",
"""        if (Services.PLATFORM.isModLoaded("ohmega") && OhmegaHelper.isQuiverEquipped(player)) {
            return OhmegaHelper.getQuiver(player);
        }
        if (isQuiverEquippedCommon(player)) return getQuiverCommon(player);
        return ItemStack.EMPTY;"""
)
p.write_text(s)

PY


# Apply maintained source overrides for the Ohmega edition.
echo "Applying Archery Things Ohmega overrides..."
cp -R ports/archerythings/overrides/common/src/main/java/com work/archerythings/common/src/main/java/
cp -R ports/archerythings/overrides/neoforge/src/main/java/com work/archerythings/neoforge/src/main/java/

cd work/archerythings
gradle :neoforge:clean :neoforge:build --stacktrace
cd ../..

cp work/archerythings/neoforge/build/libs/*.jar artifacts/
ls -lah artifacts
