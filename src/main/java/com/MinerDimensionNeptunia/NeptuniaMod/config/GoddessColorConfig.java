package com.MinerDimensionNeptunia.NeptuniaMod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;

import java.awt.Color;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 从 JSON 文件加载女神颜色配置
 */
public class GoddessColorConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<GoddessType, GoddessColors> COLOR_MAP = new HashMap<>();

    // 默认颜色（硬编码回退）
    private static final GoddessColors DEFAULT_COLORS = new GoddessColors(
            new Color(0x8B5CF6), // 主色 - 紫色
            new Color(0xA78BFA), // 副色 - 浅紫
            new Color(0x6D28D9), // 边框色 - 深紫
            new Color(0xFFFFFF), // 文字色 - 白色
            new Color(0xEDE9FE) // 背景色 - 淡紫
    );

    static {
        loadConfig();
    }

    public static void loadConfig() {
        try {
            InputStream stream = GoddessColorConfig.class.getClassLoader()
                    .getResourceAsStream("assets/miner_dimension_neptunia/config/goddess_colors.json");
            if (stream == null) {
                System.err.println("⚠️ [GoddessColorConfig] 配置文件未找到，使用默认颜色");
                return;
            }

            InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            for (GoddessType type : GoddessType.values()) {
                if (type == GoddessType.NONE)
                    continue;
                JsonElement element = root.get(type.name().toLowerCase());
                if (element != null && element.isJsonObject()) {
                    JsonObject colorObj = element.getAsJsonObject();
                    GoddessColors colors = parseColors(colorObj);
                    COLOR_MAP.put(type, colors);
                    System.out.println("📝 [GoddessColorConfig] 加载 " + type.name() + " 颜色配置");
                }
            }
            reader.close();
            stream.close();
        } catch (Exception e) {
            System.err.println("⚠️ [GoddessColorConfig] 加载配置失败，使用默认颜色: " + e.getMessage());
        }
    }

    private static GoddessColors parseColors(JsonObject obj) {
        int main = parseColor(obj, "main", DEFAULT_COLORS.main);
        int secondary = parseColor(obj, "secondary", DEFAULT_COLORS.secondary);
        int border = parseColor(obj, "border", DEFAULT_COLORS.border);
        int text = parseColor(obj, "text", DEFAULT_COLORS.text);
        int background = parseColor(obj, "background", DEFAULT_COLORS.background);
        return new GoddessColors(
                new Color(main),
                new Color(secondary),
                new Color(border),
                new Color(text),
                new Color(background));
    }

    private static int parseColor(JsonObject obj, String key, Color defaultColor) {
        if (obj.has(key)) {
            String hex = obj.get(key).getAsString();
            if (hex.startsWith("#"))
                hex = hex.substring(1);
            try {
                return Integer.parseInt(hex, 16);
            } catch (NumberFormatException e) {
                return defaultColor.getRGB();
            }
        }
        return defaultColor.getRGB();
    }

    public static GoddessColors getColors(GoddessType type) {
        return COLOR_MAP.getOrDefault(type, DEFAULT_COLORS);
    }

    public static void reload() {
        COLOR_MAP.clear();
        loadConfig();
        System.out.println("🔄 [GoddessColorConfig] 配置已重载");
    }

    // ===== 颜色数据类 =====
    public static class GoddessColors {
        public final Color main;
        public final Color secondary;
        public final Color border;
        public final Color text;
        public final Color background;

        public GoddessColors(Color main, Color secondary, Color border, Color text, Color background) {
            this.main = main;
            this.secondary = secondary;
            this.border = border;
            this.text = text;
            this.background = background;
        }
    }
}