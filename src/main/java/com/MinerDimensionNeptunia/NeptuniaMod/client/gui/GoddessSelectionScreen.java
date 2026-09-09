package com.MinerDimensionNeptunia.NeptuniaMod.client.gui;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import com.MinerDimensionNeptunia.NeptuniaMod.network.GoddessTypeSelectPacket;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GoddessSelectionScreen extends Screen {
    public GoddessSelectionScreen() {
        super(Component.literal("选择女神"));
    }

    @Override
    protected void init() {
        int buttonWidth = 150;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;
        int centerY = this.height / 2 - 30;

        this.addRenderableWidget(Button.builder(
                        Component.literal("原型女神 (平衡型)"),
                        button -> {
                            Neptunia.CHANNEL.sendToServer(new GoddessTypeSelectPacket(GoddessType.PROTOTYPE));
                            this.onClose();
                        })
                .pos(centerX, centerY)
                .size(buttonWidth, buttonHeight)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.literal("取消"),
                        button -> this.onClose())
                .pos(centerX, centerY + 30)
                .size(buttonWidth, buttonHeight)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, "请选择你的女神", this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}