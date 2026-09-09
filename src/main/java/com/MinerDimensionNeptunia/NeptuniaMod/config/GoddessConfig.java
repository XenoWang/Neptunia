package com.MinerDimensionNeptunia.NeptuniaMod.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class GoddessConfig {
    public static final ClientConfig CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        final Pair<ClientConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
        System.out.println("✅ [GoddessConfig] 客户端配置规范已加载！");
    }

    public static class ClientConfig {
        // ---- HUD 配置分组 ----
        public final ForgeConfigSpec.IntValue hudOffsetX;
        public final ForgeConfigSpec.IntValue hudOffsetY;
        public final ForgeConfigSpec.DoubleValue hudScale;

        // ---- 后续可添加其他分组 ----
        // public final ForgeConfigSpec.BooleanValue someToggle;

        ClientConfig(ForgeConfigSpec.Builder builder) {
            // ========== 1. HUD 设置 ==========
            builder.push("hud");
            hudOffsetX = builder
                    .comment("Horizontal offset from the right edge of the screen (in scaled pixels)")
                    .defineInRange("offsetX", 20, 0, 500);
            hudOffsetY = builder
                    .comment("Vertical offset from the bottom edge of the screen (in scaled pixels)")
                    .defineInRange("offsetY", 70, 0, 500);
            hudScale = builder
                    .comment("Scale factor for the HUD bar (1.0 = 100%)")
                    .defineInRange("scale", 1.0, 0.5, 2.0);
            builder.pop();

            // ========== 2. 后续扩展示例 ==========
            // builder.push("combat");
            // someToggle = builder.comment("Enable something").define("enableSomething", true);
            // builder.pop();
        }
    }
}