package com.example.bowboost;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Plain .properties config in the Fabric config folder (no extra libraries). */
public class BoostConfig {
    public static int lookMs = 200;         // time to look down / back up
    public static int chargeMs = 1000;      // bow draw time before release
    public static int releaseBase = 120;    // ms between jump and release (before ping comp.)
    public static int manualPing = 0;       // 0 = use real latency from the player list
    public static double pingFactor = 0.5;  // release delay += factor * ping
    public static int lookDeg = 90;         // pitch to look at (90 = straight down)
    public static boolean holdMode = false;
    public static boolean returnLook = true;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("bowboost.properties");
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) {
            save();
            return;
        }
        try (InputStream in = Files.newInputStream(f)) {
            Properties p = new Properties();
            p.load(in);
            lookMs = Integer.parseInt(p.getProperty("lookMs", "" + lookMs));
            chargeMs = Integer.parseInt(p.getProperty("chargeMs", "" + chargeMs));
            releaseBase = Integer.parseInt(p.getProperty("releaseBase", "" + releaseBase));
            manualPing = Integer.parseInt(p.getProperty("manualPing", "" + manualPing));
            pingFactor = Double.parseDouble(p.getProperty("pingFactor", "" + pingFactor));
            lookDeg = Integer.parseInt(p.getProperty("lookDeg", "" + lookDeg));
            holdMode = Boolean.parseBoolean(p.getProperty("holdMode", "" + holdMode));
            returnLook = Boolean.parseBoolean(p.getProperty("returnLook", "" + returnLook));
        } catch (Exception e) {
            System.err.println("[BowBoost] Could not read config, using defaults: " + e);
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("lookMs", "" + lookMs);
        p.setProperty("chargeMs", "" + chargeMs);
        p.setProperty("releaseBase", "" + releaseBase);
        p.setProperty("manualPing", "" + manualPing);
        p.setProperty("pingFactor", "" + pingFactor);
        p.setProperty("lookDeg", "" + lookDeg);
        p.setProperty("holdMode", "" + holdMode);
        p.setProperty("returnLook", "" + returnLook);
        try (OutputStream out = Files.newOutputStream(file())) {
            p.store(out, "Bow Boost settings");
        } catch (Exception e) {
            System.err.println("[BowBoost] Could not save config: " + e);
        }
    }
}
