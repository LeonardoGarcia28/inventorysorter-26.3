# Corrección no oficial de Inventory HUD+ para NeoForge 26.3

Instala estos **dos mods** en `mods`:

- El archivo oficial `inventoryhud.neoforge.26.3-3.4.36.jar` de Inventory HUD+ de DmitryLovin.
- `inventoryhudcompat-neoforge-26.3.0.1.jar` (este pequeño mod adicional).

**No** reemplaza Inventory HUD+ ni redistribuye sus archivos. Desactiva solamente el método `NeoUpdateNotification.onPlayerJoin`, que llama a la clase faltante `net.neoforged.fml.VersionChecker` bajo NeoForge 26.3.0.58-beta. Se mantiene el inventario en pantalla y demás funciones originales.

Compatible **únicamente** con Minecraft 26.3 / NeoForge 26.3.0.58-beta e Inventory HUD+ 3.4.36 hasta que se prueben otras versiones.

Compilación: Java 25, Gradle 9.2.1, NeoGradle 7.1.39. Construir con `./gradlew build`.

Advertencia: la compilación valida el archivo, pero el comportamiento real debe probarse en el cliente de Minecraft. No instales en un mundo valioso sin una copia de seguridad.

Mod original: © DmitryLovin, All Rights Reserved. Este parche es código independiente. No oficial ni afiliado al autor.
