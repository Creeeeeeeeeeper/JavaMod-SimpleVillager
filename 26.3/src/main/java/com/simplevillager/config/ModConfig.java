package com.simplevillager.config;

import com.simplevillager.SimpleVillagerMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModConfig {
    private static final Path CONFIG_DIR = Path.of("config", "simplevillager");
    private static final Path SERVER_CONFIG_PATH = CONFIG_DIR.resolve("server.toml");
    private static final Path CLIENT_CONFIG_PATH = CONFIG_DIR.resolve("client.toml");

    private static ServerConfig serverConfig;
    private static ClientConfig clientConfig;

    public static void init() {
        serverConfig = loadOrCreateServer();
        clientConfig = loadOrCreateClient();
    }

    public static void reload() {
        serverConfig = loadOrCreateServer();
        clientConfig = loadOrCreateClient();
        SimpleVillagerMod.LOGGER.info("Config reloaded");
    }

    public static ServerConfig server() {
        return serverConfig;
    }

    public static ClientConfig client() {
        return clientConfig;
    }

    private static ServerConfig loadOrCreateServer() {
        ServerConfig config = new ServerConfig();
        if (Files.exists(SERVER_CONFIG_PATH)) {
            try {
                String content = Files.readString(SERVER_CONFIG_PATH);
                Map<String, String> map = parseToml(content);
                config.breedingTime = getInt(map, "breeding_time", config.breedingTime);
                config.convertingTime = getInt(map, "converting_time", config.convertingTime);
                config.farmerSpeed = getInt(map, "farmer_speed", config.farmerSpeed);
                config.cropBlacklist = getStringArray(map, "crop_blacklist", config.cropBlacklist);
                config.golemSpawnTime = getInt(map, "golem_spawn_time", config.golemSpawnTime);
                config.traderRestockTime = getInt(map, "trader_restock_time", config.traderRestockTime);
                config.traderRestockUses = getInt(map, "trader_restock_uses", config.traderRestockUses);
                config.traderMaxUses = getInt(map, "trader_max_uses", config.traderMaxUses);
                config.autoTraderSpeed = getInt(map, "auto_trader_speed", config.autoTraderSpeed);
                config.incubatorSpeed = getInt(map, "incubator_speed", config.incubatorSpeed);
                config.villagerSounds = getBool(map, "villager_sounds", config.villagerSounds);
                config.tradeCycling = getBool(map, "trade_cycling", config.tradeCycling);
                config.autoTraderInfinite = getBool(map, "auto_trader_infinite", config.autoTraderInfinite);
            } catch (Exception e) {
                SimpleVillagerMod.LOGGER.error("Failed to load server config, using defaults", e);
            }
        } else {
            saveServer(config);
        }
        return config;
    }

    private static ClientConfig loadOrCreateClient() {
        ClientConfig config = new ClientConfig();
        if (Files.exists(CLIENT_CONFIG_PATH)) {
            try {
                String content = Files.readString(CLIENT_CONFIG_PATH);
                Map<String, String> map = parseToml(content);
                config.sneakPickup = getBool(map, "sneak_pickup", config.sneakPickup);
                config.volume = getFloat(map, "volume", config.volume);
                config.renderBlockContents = getBool(map, "render_block_contents", config.renderBlockContents);
                config.blockRenderDistance = getInt(map, "block_render_distance", config.blockRenderDistance);
            } catch (Exception e) {
                SimpleVillagerMod.LOGGER.error("Failed to load client config, using defaults", e);
            }
        } else {
            saveClient(config);
        }
        return config;
    }

    public static void saveAll() {
        saveServer(serverConfig);
        saveClient(clientConfig);
    }

    private static void saveServer(ServerConfig config) {
        String toml = """
                # Simple Villagers - Server Configuration
                # 简易村民 - 服务端配置
                # Changes require a world restart to take effect.
                # 修改后需要重启世界生效。

                # Interval in ticks between breeding attempts (20 ticks = 1 second).
                # The breeder tries to breed every this many ticks, but needs 2 adult
                # villagers and 24+ food points to actually produce a baby villager.
                # 繁殖尝试间隔（20刻 = 1秒）。繁殖器每过这些刻数尝试繁殖一次，
                # 需要两只成年村民且食物营养值达到24才会产出小村民。
                breeding_time = %d

                # Time in ticks for the zombie villager to be cured (20 ticks = 1 second).
                # The whole conversion takes converting_time + 100 ticks:
                # 0-100 zombify, converting_time cure, +100 output delay. Default 1900 = 100s total.
                # 僵尸村民治愈所需时间（20刻 = 1秒）。完整转换耗时 = converting_time + 100 刻：
                # 前100刻僵尸化，converting_time刻治愈，最后100刻输出。默认1900 = 总计100秒。
                converting_time = %d

                # Crop growth speed: every second there is a 1/farmer_speed chance to grow
                # the crop one stage. Higher = slower growth (default 10 = 10 percent chance per second).
                # 作物生长速度的分母：每秒有 1/farmer_speed 的概率让作物生长一格。
                # 数值越大生长越慢（默认10 = 每秒百分之十概率）。
                farmer_speed = %d

                # List of crop block IDs that the farmer will NOT harvest
                # 农民不会收割的作物方块ID列表
                # Example / 示例: crop_blacklist = ["minecraft:nether_wart", "minecraft:beetroot"]
                crop_blacklist = [%s]

                # Time in ticks until the iron golem appears (20 ticks = 1 second).
                # The full loot cycle takes golem_spawn_time + 100 ticks. Default 1100 = 60s per cycle.
                # 铁傀儡出现所需时间（20刻 = 1秒）。完整掉落循环耗时 = golem_spawn_time + 100 刻。
                # 默认1100 = 每60秒一个循环。
                golem_spawn_time = %d

                # Base wait in ticks before the trader restocks (20 ticks = 1 second).
                # Actual wait = trader_restock_time + a random 0-2400 tick delay.
                # A restock is also triggered early when total uses reach trader_restock_uses.
                # 交易站补货的基础等待时间（20刻 = 1秒）。实际等待 = trader_restock_time + 随机0~2400刻。
                # 当所有交易累计次数达到 trader_restock_uses 时也会提前补货。
                trader_restock_time = %d

                # Total number of trades (across all offers) that triggers an instant restock
                # 所有交易选项累计交易次数达到多少后立即触发补货
                trader_restock_uses = %d

                # Maximum number of uses for each trade offer. 0 = keep vanilla limits.
                # 每个交易选项的最大可交易次数。设为0则保持原版限制。
                trader_max_uses = %d

                # Interval in ticks between automatic trades (20 ticks = 1 second)
                # 自动交易站每交易一次的间隔（20刻 = 1秒）
                auto_trader_speed = %d

                # How many age ticks a baby villager grows per game tick (min 1).
                # 1 = vanilla speed (about 20 minutes to become adult); larger = faster.
                # 每游戏刻小村民成长的年龄刻数（最小为1）。1 = 原版速度（约20分钟成年）；越大越快。
                incubator_speed = %d

                # Whether villager blocks play sounds (ambient, work, breeding, conversion, etc.)
                # 村民方块是否播放音效（环境、工作、繁殖、转换等）
                villager_sounds = %b

                # Enable the trade cycling button in the merchant screen (server accepts the request)
                # 是否启用交易刷新按钮（服务端是否接受刷新请求）
                trade_cycling = %b

                # Auto trader infinite mode - trades never run out, no restocking needed
                # 自动交易站无限模式 - 交易不会耗尽，无需补货
                auto_trader_infinite = %b
                """.formatted(
                config.breedingTime, config.convertingTime, config.farmerSpeed,
                formatStringArray(config.cropBlacklist),
                config.golemSpawnTime, config.traderRestockTime, config.traderRestockUses,
                config.traderMaxUses, config.autoTraderSpeed, config.incubatorSpeed,
                config.villagerSounds, config.tradeCycling,
                config.autoTraderInfinite
        );
        writeToml(SERVER_CONFIG_PATH, toml);
    }

    private static void saveClient(ClientConfig config) {
        String toml = """
                # Simple Villagers - Client Configuration
                # 简易村民 - 客户端配置

                # Whether to pick up villagers by sneaking and right-clicking
                # 是否可以通过潜行+右键拾取村民
                sneak_pickup = %b

                # Volume for villager block sounds (0.0 - 1.0)
                # 村民方块音效音量（0.0 - 1.0）
                volume = %s

                # Whether to render the contents of blocks in the world
                # 是否在方块上方渲染内部物品/村民
                render_block_contents = %b

                # Maximum distance (in blocks) to render block contents
                # 渲染方块内容的最大距离（单位：方块）
                block_render_distance = %d
                """.formatted(
                config.sneakPickup, config.volume,
                config.renderBlockContents, config.blockRenderDistance
        );
        writeToml(CLIENT_CONFIG_PATH, toml);
    }

    private static void writeToml(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
        } catch (IOException e) {
            SimpleVillagerMod.LOGGER.error("Failed to save config: {}", path, e);
        }
    }

    private static Map<String, String> parseToml(String content) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String line : content.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            map.put(key, value);
        }
        return map;
    }

    private static int getInt(Map<String, String> map, String key, int def) {
        String v = map.get(key);
        if (v == null) return def;
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return def; }
    }

    private static float getFloat(Map<String, String> map, String key, float def) {
        String v = map.get(key);
        if (v == null) return def;
        try { return Float.parseFloat(v.trim()); } catch (Exception e) { return def; }
    }

    private static boolean getBool(Map<String, String> map, String key, boolean def) {
        String v = map.get(key);
        if (v == null) return def;
        return Boolean.parseBoolean(v.trim());
    }

    private static String getString(Map<String, String> map, String key, String def) {
        String v = map.get(key);
        if (v == null) return def;
        return v.trim().replace("\"", "");
    }

    private static String[] getStringArray(Map<String, String> map, String key, String[] def) {
        String v = map.get(key);
        if (v == null) return def;
        v = v.trim();
        if (v.equals("[]")) return new String[0];
        v = v.replaceAll("^\\[|\\]$", "");
        String[] parts = v.split(",");
        String[] result = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = parts[i].trim().replace("\"", "");
        }
        return result;
    }

    private static String formatStringArray(String[] arr) {
        if (arr.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(arr[i]).append("\"");
        }
        return sb.toString();
    }
}
