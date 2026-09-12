package com.MinerDimensionNeptunia.NeptuniaMod.client.gui.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置模块注册中心
 */
public class Modules {
    private static final List<ConfigModule> MODULES = new ArrayList<>();

    static {
        // 注册 HUD 配置模块
        register(new HudConfigModule());
        // 后续可在此添加更多模块：
        // register(new CombatConfigModule());
        // register(new AudioConfigModule());
        // register(new GraphicsConfigModule());
    }

    public static void register(ConfigModule module) {
        MODULES.add(module);
    }

    public static List<ConfigModule> getModules() {
        return new ArrayList<>(MODULES);
    }

    public static ConfigModule getModule(int index) {
        if (index >= 0 && index < MODULES.size()) {
            return MODULES.get(index);
        }
        return null;
    }

    public static int getModuleCount() {
        return MODULES.size();
    }
}