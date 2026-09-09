package com.MinerDimensionNeptunia.NeptuniaMod.client.gui.config;

import com.MinerDimensionNeptunia.NeptuniaMod.config.GoddessConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HudConfigModule implements ConfigModule {

    @Override
    public Component getDisplayName() {
        return Component.literal("女神条HUD 设置");
    }

    @Override
    public Screen createConfigScreen(Screen parent) {
        return new HudConfigScreen(parent);
    }

    // ===== HUD 配置子界面 =====
    @OnlyIn(Dist.CLIENT)
    public static class HudConfigScreen extends Screen {
        private final Screen parent;

        private int cachedOffsetX;
        private int cachedOffsetY;
        private double cachedScale;

        public HudConfigScreen(Screen parent) {
            super(Component.literal("女神条HUD 设置"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            cachedOffsetX = GoddessConfig.CLIENT.hudOffsetX.get();
            cachedOffsetY = GoddessConfig.CLIENT.hudOffsetY.get();
            cachedScale = GoddessConfig.CLIENT.hudScale.get();

            int centerX = this.width / 2;
            int startY = 50;
            int sliderWidth = 220;
            int rowHeight = 30;

            // ---- 返回按钮 ----
            this.addRenderableWidget(Button.builder(
                    Component.literal("← 返回"),
                    button -> {
                        GoddessConfig.CLIENT_SPEC.save();
                        if (this.minecraft != null) {
                            this.minecraft.setScreen(this.parent);
                        }
                    }
            ).bounds(10, 10, 60, 20).build());

            // ---- X 偏移滑块 ----
            this.addRenderableWidget(new ConfigSlider(
                    centerX - sliderWidth / 2,
                    startY,
                    sliderWidth, 20,
                    Component.literal("X 偏移"),
                    0, 500, cachedOffsetX,
                    value -> {
                        cachedOffsetX = value;
                        GoddessConfig.CLIENT.hudOffsetX.set(value);
                    }
            ));

            // ---- Y 偏移滑块 ----
            this.addRenderableWidget(new ConfigSlider(
                    centerX - sliderWidth / 2,
                    startY + rowHeight,
                    sliderWidth, 20,
                    Component.literal("Y 偏移"),
                    0, 500, cachedOffsetY,
                    value -> {
                        cachedOffsetY = value;
                        GoddessConfig.CLIENT.hudOffsetY.set(value);
                    }
            ));

            // ---- 缩放滑块 ----
            this.addRenderableWidget(new ConfigSlider(
                    centerX - sliderWidth / 2,
                    startY + rowHeight * 2,
                    sliderWidth, 20,
                    Component.literal("缩放"),
                    50, 200, (int) (cachedScale * 100),
                    value -> {
                        cachedScale = value / 100.0;
                        GoddessConfig.CLIENT.hudScale.set(cachedScale);
                    }
            ));

            // ---- 重置默认按钮 ----
            this.addRenderableWidget(Button.builder(
                    Component.literal("重置默认"),
                    button -> {
                        cachedOffsetX = 20;
                        cachedOffsetY = 70;
                        cachedScale = 1.0;
                        GoddessConfig.CLIENT.hudOffsetX.set(20);
                        GoddessConfig.CLIENT.hudOffsetY.set(70);
                        GoddessConfig.CLIENT.hudScale.set(1.0);
                        this.clearWidgets();
                        this.init();
                    }
            ).bounds(centerX - 50, startY + rowHeight * 3 + 20, 100, 20).build());

            // ---- 保存并应用按钮 ----
            this.addRenderableWidget(Button.builder(
                    Component.literal("保存并应用"),
                    button -> {
                        GoddessConfig.CLIENT_SPEC.save();
                    }
            ).bounds(centerX - 50, startY + rowHeight * 3 + 50, 100, 20).build());
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(graphics);
            graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

            String previewText = String.format(
                    "当前位置: X=%d  Y=%d  缩放: %.0f%%",
                    cachedOffsetX, cachedOffsetY, cachedScale * 100
            );
            graphics.drawCenteredString(this.font, previewText, this.width / 2, 135, 0xAAAAAA);

            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void onClose() {
            GoddessConfig.CLIENT_SPEC.save();
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        // ===== 自定义滑块类 =====
        @OnlyIn(Dist.CLIENT)
        private static class ConfigSlider extends AbstractSliderButton {
            private final String label;
            private final int min;
            private final int max;
            private final IntConsumer onValueChanged;

            private interface IntConsumer {
                void accept(int value);
            }

            public ConfigSlider(int x, int y, int width, int height,
                                Component label, int min, int max, int currentValue,
                                IntConsumer onValueChanged) {
                super(x, y, width, height,
                        Component.empty(),
                        (currentValue - min) / (double) (max - min));
                this.label = label.getString();
                this.min = min;
                this.max = max;
                this.onValueChanged = onValueChanged;
                this.updateMessage();
            }

            @Override
            protected void updateMessage() {
                int value = min + (int) ((max - min) * this.value);
                if (label.contains("缩放")) {
                    this.setMessage(Component.literal(label + ": " + value + "%"));
                } else {
                    this.setMessage(Component.literal(label + ": " + value));
                }
            }

            @Override
            protected void applyValue() {
                int value = min + (int) ((max - min) * this.value);
                if (onValueChanged != null) {
                    onValueChanged.accept(value);
                }
            }
        }
    }
}