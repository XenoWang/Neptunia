package com.MinerDimensionNeptunia.NeptuniaMod.client.gui;

import com.MinerDimensionNeptunia.NeptuniaMod.config.GoddessColorConfig;
import com.MinerDimensionNeptunia.NeptuniaMod.config.GoddessConfig;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.Goddess;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.GoddessRegistry;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.awt.Color;

public class GoddessHudRenderer {
    private static final int BASE_BAR_WIDTH = 140;
    private static final int BASE_BAR_HEIGHT = 16;
    private static final int TRANSFORM_DURATION = 180;

    private static GoddessType currentType = GoddessType.NONE;
    private static long currentStartTime = 0;
    private static boolean isActive = false;

    public static void updateState(GoddessType type, long startTime) {
        currentType = type;
        currentStartTime = startTime;
        isActive = (startTime > 0);
    }

    public static void resetState() {
        currentType = GoddessType.NONE;
        currentStartTime = 0;
        isActive = false;
    }

    public static void render(GuiGraphics guiGraphics, float partialTick) {
        if (!isActive) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int remaining = getRemainingSeconds();
        if (remaining <= 0) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // 读取配置
        int offsetX = GoddessConfig.CLIENT.hudOffsetX.get();
        int offsetY = GoddessConfig.CLIENT.hudOffsetY.get();
        double scale = GoddessConfig.CLIENT.hudScale.get();

        // 缩放尺寸
        int barWidth = (int) (BASE_BAR_WIDTH * scale);
        int barHeight = (int) (BASE_BAR_HEIGHT * scale);

        // 计算位置（右下角）
        int x = screenWidth - barWidth - offsetX;
        int y = screenHeight - barHeight - offsetY;

        // 边界保护
        if (x < 0) x = 0;
        if (y < 0) y = 0;

        // 获取女神颜色
        GoddessColorConfig.GoddessColors colors = GoddessColorConfig.getColors(currentType);
        Goddess goddess = GoddessRegistry.getInstance().getGoddess(currentType);
        String goddessName = goddess != null ? goddess.getDisplayName() : currentType.name();

        float progress = (float) remaining / TRANSFORM_DURATION;

        // --- 1. 绘制背景槽（半透明磨砂效果） ---
        int bgColor = 0x80000000;
        guiGraphics.fill(x - 2, y - 2, x + barWidth + 2, y + barHeight + 2, bgColor);

        // --- 2. 绘制内部背景（根据女神颜色） ---
        int innerBg = colors.background.getRGB() & 0x88FFFFFF;
        guiGraphics.fill(x, y, x + barWidth, y + barHeight, innerBg);

        // --- 3. 绘制渐变填充条（从副色到主色） ---
        int fillWidth = (int) (barWidth * progress);
        if (fillWidth > 0) {
            for (int i = 0; i < fillWidth; i++) {
                float ratio = (float) i / barWidth;
                int r = (int) (colors.secondary.getRed() + (colors.main.getRed() - colors.secondary.getRed()) * ratio);
                int g = (int) (colors.secondary.getGreen() + (colors.main.getGreen() - colors.secondary.getGreen()) * ratio);
                int b = (int) (colors.secondary.getBlue() + (colors.main.getBlue() - colors.secondary.getBlue()) * ratio);
                int color = (0xFF << 24) | (r << 16) | (g << 8) | b;
                guiGraphics.fill(x + i, y + 2, x + i + 1, y + barHeight - 2, color);
            }
        }

        // --- 4. 绘制发光效果（在填充条上方叠加半透明光晕） ---
        if (fillWidth > 0) {
            int glowColor = colors.main.getRGB() & 0x44FFFFFF;
            int glowWidth = Math.min((int)(20 * scale), fillWidth);
            guiGraphics.fill(x, y, x + glowWidth, y + barHeight, glowColor);
        }

        // --- 5. 绘制边框（发光边框效果） ---
        drawGlowBorder(guiGraphics, x, y, barWidth, barHeight, colors.border);

        // --- 6. 绘制分段标记（类似HDD槽的分段效果） ---
        int segments = 6;
        for (int i = 1; i < segments; i++) {
            int segX = x + (barWidth * i / segments);
            guiGraphics.fill(segX, y + 2, segX + 1, y + barHeight - 2, 0x33FFFFFF);
        }

        // --- 7. 绘制倒计时文字（居中显示） ---
        String timeText = String.format("%02d:%02d", remaining / 60, remaining % 60);
        int textWidth = mc.font.width(timeText);
        int textX = x + (barWidth - textWidth) / 2;
        int textY = y + (barHeight - mc.font.lineHeight) / 2 + 1;
        guiGraphics.drawString(mc.font, timeText, textX, textY, colors.text.getRGB(), true);

        // --- 8. 绘制女神名称（在条上方） ---
        String nameText = "⚔ " + goddessName;
        int nameWidth = mc.font.width(nameText);
        int nameX = x + (barWidth - nameWidth) / 2;
        int nameY = y - mc.font.lineHeight - 2;
        guiGraphics.drawString(mc.font, nameText, nameX, nameY, 0xFFFFFF, true);

        // --- 9. 绘制属性数值（在条下方） ---
        double attack = mc.player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double speed = mc.player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double armor = mc.player.getAttributeValue(Attributes.ARMOR);
        String stats = String.format("⚔%.1f  🏃%.2f  🛡%.1f", attack, speed, armor);
        int statsWidth = mc.font.width(stats);
        int statsX = x + (barWidth - statsWidth) / 2;
        int statsY = y + barHeight + 2;
        guiGraphics.drawString(mc.font, stats, statsX, statsY, 0xCCCCCC, true);

        // --- 10. 绘制剩余百分比（在条右端） ---
        String percentText = String.format("%d%%", (int) (progress * 100));
        int percentWidth = mc.font.width(percentText);
        int percentX = x + barWidth - percentWidth - 4;
        int percentY = y + (barHeight - mc.font.lineHeight) / 2 + 1;
        guiGraphics.drawString(mc.font, percentText, percentX, percentY, 0x88FFFFFF, true);
    }

    private static void drawGlowBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, Color color) {
        int borderColor = color.getRGB() & 0xCCFFFFFF;
        int glowColor = color.getRGB() & 0x44FFFFFF;

        guiGraphics.fill(x - 1, y - 1, x + width + 1, y, glowColor);
        guiGraphics.fill(x - 1, y + height, x + width + 1, y + height + 1, glowColor);
        guiGraphics.fill(x - 1, y, x, y + height, glowColor);
        guiGraphics.fill(x + width, y, x + width + 1, y + height, glowColor);

        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);
    }

    private static int getRemainingSeconds() {
        if (currentStartTime <= 0) return 0;
        long elapsed = (System.currentTimeMillis() - currentStartTime) / 1000;
        return Math.max(0, TRANSFORM_DURATION - (int) elapsed);
    }
}