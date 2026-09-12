package com.MinerDimensionNeptunia.NeptuniaMod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * 开发者名单：从游戏 config 目录的 JSON 文件读取
 * （config/miner_dimension_neptunia_dev_players.json）。
 * 文件缺失或解析失败时自动生成默认名单，方便直接编辑添加开发者。
 */
public class DevPlayerList {
    private static final Path FILE =
            FMLPaths.CONFIGDIR.get().resolve("miner_dimension_neptunia_dev_players.json");
    private static final String[] DEFAULT_NAMES = {"dev", "xenodomc"};
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private DevPlayerList() {
    }

    /** 判断玩家名是否在开发者名单内（不区分大小写） */
    public static boolean isDevPlayer(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return false;
        }
        return loadNames().contains(playerName.toLowerCase());
    }

    private static Set<String> loadNames() {
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                Set<String> names = new HashSet<>();
                if (root.has("dev_players") && root.get("dev_players").isJsonArray()) {
                    for (JsonElement element : root.getAsJsonArray("dev_players")) {
                        if (element.isJsonPrimitive()) {
                            names.add(element.getAsString().toLowerCase());
                        }
                    }
                }
                return names;
            } catch (Exception e) {
                // 读取失败，使用默认名单
            }
        }

        // 文件缺失或解析失败：写回默认名单
        writeDefault();
        Set<String> names = new HashSet<>();
        for (String name : DEFAULT_NAMES) {
            names.add(name.toLowerCase());
        }
        return names;
    }

    private static void writeDefault() {
        try {
            Files.createDirectories(FILE.getParent());
            JsonObject root = new JsonObject();
            JsonArray array = new JsonArray();
            for (String name : DEFAULT_NAMES) {
                array.add(name);
            }
            root.add("dev_players", array);
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            // 写入默认名单失败（使用内存中的默认名单）
        }
    }
}
