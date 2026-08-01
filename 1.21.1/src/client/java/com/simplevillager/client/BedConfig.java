package com.simplevillager.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class BedConfig {

    public static double offsetZ = 0.1875;
    public static double scale = 0.4;
    public static double translateX = 1.75;
    public static double translateY = 0.0;
    public static double translateZ = 1.75;
    public static double rotateCenterZ = -0.5;
    public static double rotationY = 180.0;
    public static double villagerGroundScale = 1.25;
    public static double villagerGroundY = 0.0;

    private BedConfig() {
    }

    private static final String DEFAULT_CONTENT = """
            [bed]
            offsetZ = 0.1875
            scale = 0.4
            translateX = 1.75
            translateY = 0.0
            translateZ = 1.75
            rotateCenterZ = -0.5
            rotationY = 180.0
            [villager]
            groundScale = 1.25
            groundY = 0.0
            """;

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("test.toml");
        try {
            if (!Files.exists(path)) {
                Files.writeString(path, DEFAULT_CONTENT);
                return;
            }
            Map<String, String> values = new HashMap<>();
            for (String line : Files.readAllLines(path)) {
                String s = line.trim();
                if (s.isEmpty() || s.startsWith("#")) continue;
                if (s.startsWith("[") && s.endsWith("]")) continue;
                int eq = s.indexOf('=');
                if (eq < 0) continue;
                values.put(s.substring(0, eq).trim(), s.substring(eq + 1).trim());
            }
            offsetZ = getDouble(values, "offsetZ", offsetZ);
            scale = getDouble(values, "scale", scale);
            translateX = getDouble(values, "translateX", translateX);
            translateY = getDouble(values, "translateY", translateY);
            translateZ = getDouble(values, "translateZ", translateZ);
            rotateCenterZ = getDouble(values, "rotateCenterZ", rotateCenterZ);
            rotationY = getDouble(values, "rotationY", rotationY);
            villagerGroundScale = getDouble(values, "groundScale", villagerGroundScale);
            villagerGroundY = getDouble(values, "groundY", villagerGroundY);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static double getDouble(Map<String, String> values, String key, double fallback) {
        String v = values.get(key);
        if (v == null) return fallback;
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
