# Bow Boost - Fabric, Minecraft 26.2

Requires: JDK 25, Gradle 9.3+, Fabric Loader >= 0.19.1, Fabric API (0.150.1+26.2 or newer for 26.2).

## Build
    gradle wrapper --gradle-version 9.3     # once, creates ./gradlew
    ./gradlew build
Jar: build/libs/bowboost-1.0.0.jar -> put in .minecraft/mods together with Fabric API.

(Or generate a fresh 26.2 project at https://fabricmc.net/develop/template/ and copy `src/` over it.)

## Use
* Right Shift: settings screen (sliders, toggles, click a key button then press a key / side mouse button)
* G: boost with a bow in your main hand
* H: toggle hold-bow mode (starts the boost automatically when you start drawing the bow)
Keys also appear in Options > Controls > Key Binds under "Bow Boost".
Settings file: config/bowboost.properties
