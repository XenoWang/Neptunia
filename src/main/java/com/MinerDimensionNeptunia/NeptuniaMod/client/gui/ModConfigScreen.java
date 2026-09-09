package com.MinerDimensionNeptunia.NeptuniaMod.client.gui;

import com.MinerDimensionNeptunia.NeptuniaMod.client.gui.config.ConfigModule;
import com.MinerDimensionNeptunia.NeptuniaMod.client.gui.config.Modules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 配置主界面 - 显示所有配置模块列表
 */
public class ModConfigScreen extends Screen {
    private final Screen parent;
    private List<ConfigModule> modules;

    public ModConfigScreen(Screen parent) {
        super(Component.literal("Neptunia Mod 配置"));
        this.parent = parent;
        this.modules = Modules.getModules();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 50;
        int buttonWidth = 200;
        int buttonHeight = 25;
        int spacing = 5;

        for (int i = 0; i < modules.size(); i++) {
            ConfigModule module = modules.get(i);
            int y = startY + i * (buttonHeight + spacing);
            this.addRenderableWidget(Button.builder(
                    module.getDisplayName(),
                    button -> {
                        if (this.minecraft != null) {
                            this.minecraft.setScreen(module.createConfigScreen(this));
                        }
                    }
            ).bounds(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight).build());
        }

        int closeY = startY + modules.size() * (buttonHeight + spacing) + 20;
        this.addRenderableWidget(Button.builder(
                Component.literal("关闭"),
                button -> this.onClose()
        ).bounds(centerX - 50, closeY, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        String subtitle = "请选择要配置的模块";
        graphics.drawCenteredString(this.font, subtitle, this.width / 2, 38, 0x888888);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}