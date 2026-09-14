package com.MinerDimensionNeptunia.NeptuniaMod.client.gui;

import com.MinerDimensionNeptunia.NeptuniaMod.config.GoddessColorConfig;
import com.MinerDimensionNeptunia.NeptuniaMod.config.GoddessConfig;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.Goddess;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.GoddessRegistry;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessDiskGen;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 女神化变身读条 HUD —— 超次元游戏海王星 HDD 槽风格：
 * <b>横向 5 段</b>：首段梯形（左平右收），后四段菱形依次相接，
 * 充能自左向右，已充能段按女神配色（副色 → 主色）发光渐变并带呼吸脉动，
 * 空段为暗色半透明。槽上方显示女神名称，下方显示
 * <b>当前基础属性</b>（攻击 / 攻速 / 移速 / 护甲 / 护甲韧性）与剩余时间。
 * <p>
 * 位置/缩放沿用 HUD 配置（右下角锚定，X/Y 偏移与缩放滑条）。
 */
public class GoddessHudRenderer {
    /** 分段数量（HDD 槽格数：1 梯形 + 4 菱形） */
    private static final int SEGMENTS = 5;
    /** 单格高度（缩放前） */
    private static final int CELL_H = 14;
    /** 梯形宽度（缩放前） */
    private static final int TRAP_W = 18;
    /** 菱形水平半宽（缩放前） */
    private static final int DIAMOND_HALF_W = 11;
    /** 尖端之间的间距（缩放前） */
    private static final int GAP = 2;
    /** 外框留白（缩放前） */
    private static final int FRAME_PAD = 4;

    private static GoddessType currentType = GoddessType.NONE;
    private static GoddessDiskGen currentGen = GoddessDiskGen.GEN5;
    private static long currentStartTime = 0;
    private static boolean isActive = false;

    public static void updateState(GoddessType type, GoddessDiskGen gen, long startTime) {
        currentType = type;
        currentGen = gen;
        currentStartTime = startTime;
        isActive = (startTime > 0);
    }

    public static void resetState() {
        currentType = GoddessType.NONE;
        currentGen = GoddessDiskGen.GEN5;
        currentStartTime = 0;
        isActive = false;
    }

    public static void render(GuiGraphics graphics, float partialTick) {
        if (!isActive) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.level == null) return;

        int remaining = getRemainingSeconds();
        if (remaining <= 0) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // 读取配置（位置与缩放）
        int offsetX = GoddessConfig.CLIENT.hudOffsetX.get();
        int offsetY = GoddessConfig.CLIENT.hudOffsetY.get();
        double scale = GoddessConfig.CLIENT.hudScale.get();

        int cellH = (int) (CELL_H * scale);
        int trapW = (int) (TRAP_W * scale);
        int halfW = (int) (DIAMOND_HALF_W * scale);
        int gap = (int) (GAP * scale);
        int pad = (int) (FRAME_PAD * scale);

        // 横向链条总宽：梯形 + 4 个菱形 + 3 个间隙
        int gaugeW = trapW + (SEGMENTS - 1) * (halfW * 2) + (SEGMENTS - 2) * gap;
        int gaugeH = cellH;

        // 文本区高度（名称 + 属性两行 + 时间一行）
        int textH = mc.font.lineHeight * 4 + 8;
        int totalH = gaugeH + pad * 2 + textH;
        int totalW = Math.max(gaugeW + pad * 2, mc.font.width("⚔ " + "xxxxxxxxxxx") + pad * 2);

        // 右下角锚定
        int x = screenWidth - totalW - offsetX;
        int y = screenHeight - totalH - offsetY;
        if (x < 0) x = 0;
        if (y < 0) y = 0;

        GoddessColorConfig.GoddessColors colors = GoddessColorConfig.getColors(currentType);
        Goddess goddess = GoddessRegistry.getInstance().getGoddess(currentType);
        String goddessName = goddess != null ? goddess.getDisplayName() : currentType.name();

        float progress = (float) remaining / currentGen.getTransformDurationSeconds();
        int filled = (int) Math.ceil(progress * SEGMENTS);

        // 呼吸脉动
        double pulse = 0.78 + 0.22 * Math.sin((mc.level.getGameTime() + partialTick) * 0.12);

        int gaugeX = x + pad;
        int gaugeY = y + pad;

        // 首段梯形四个角 + 后续菱形四角（自左向右首尾相接）
        for (int i = 0; i < SEGMENTS; i++) {
            float[][] corners = cornersOf(i, gaugeX, gaugeY, trapW, halfW, gap, cellH);
            float cx = (corners[0][0] + corners[1][0] + corners[2][0] + corners[3][0]) / 4f;
            float cy = (corners[0][1] + corners[1][1] + corners[2][1] + corners[3][1]) / 4f;

            float t = SEGMENTS > 1 ? (float) i / (SEGMENTS - 1) : 0F;
            if (i < filled) {
                int r = clamp((int) ((colors.secondary.getRed()
                        + (colors.main.getRed() - colors.secondary.getRed()) * t) * pulse));
                int g = clamp((int) ((colors.secondary.getGreen()
                        + (colors.main.getGreen() - colors.secondary.getGreen()) * t) * pulse));
                int b = clamp((int) ((colors.secondary.getBlue()
                        + (colors.main.getBlue() - colors.secondary.getBlue()) * t) * pulse));

                // 外发光 → 描边 → 填充 → 顶部亮核
                fillPolygon(graphics, expand(corners, cx, cy, 1.28f),
                        (0xFF << 24 | r << 16 | g << 8 | b) & 0x44FFFFFF);
                fillPolygon(graphics, expand(corners, cx, cy, 1.08f), colors.border.getRGB() & 0xCCFFFFFF);
                fillPolygon(graphics, corners, 0xFF000000 | (r << 16) | (g << 8) | b);
                int coreR = clamp(r + 45);
                int coreG = clamp(g + 45);
                int coreB = clamp(b + 45);
                fillPolygon(graphics, expand(corners, cx, cy - cellH * 0.1f, 0.55f),
                        (0x88 << 24) | (coreR << 16) | (coreG << 8) | coreB);
            } else {
                // 空段：暗色半透明 + 淡描边
                fillPolygon(graphics, expand(corners, cx, cy, 1.06f), 0x22FFFFFF);
                fillPolygon(graphics, corners, 0x33FFFFFF);
            }
        }

        // ---- 女神名称（槽上方） ----
        String nameText = "⚔ " + goddessName;
        graphics.drawString(mc.font, nameText, x + (totalW - mc.font.width(nameText)) / 2, y, 0xFFFFFF, true);

        // ---- 当前基础属性（槽下方两行，便于测试数值） ----
        double attack = mc.player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double attackSpeed = mc.player.getAttributeValue(Attributes.ATTACK_SPEED);
        double moveSpeed = mc.player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double armor = mc.player.getAttributeValue(Attributes.ARMOR);
        double toughness = mc.player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);

        String line1 = String.format("⚔ 攻击 %.1f   ⚡ 攻速 %.2f   🏃 移速 %.2f", attack, attackSpeed, moveSpeed);
        String line2 = String.format("🛡 护甲 %.1f   ⛨ 韧性 %.1f", armor, toughness);
        int lineY = y + pad * 2 + gaugeH + 2;
        graphics.drawString(mc.font, line1, x + (totalW - mc.font.width(line1)) / 2, lineY, 0xE8E8E8, true);
        graphics.drawString(mc.font, line2, x + (totalW - mc.font.width(line2)) / 2,
                lineY + mc.font.lineHeight, 0xE8E8E8, true);

        // ---- 剩余时间与百分比 ----
        String timeText = String.format("%02d:%02d  %d%%", remaining / 60, remaining % 60, (int) (progress * 100));
        graphics.drawString(mc.font, timeText, x + (totalW - mc.font.width(timeText)) / 2,
                lineY + mc.font.lineHeight * 2 + 2, 0xCCCCCC, true);
    }

    /** 第 i 段的四个角：0 = 梯形（左平右收），1~4 = 菱形首尾相接 */
    private static float[][] cornersOf(int i, int x, int y, int trapW, int halfW, int gap, int cellH) {
        float h = cellH;
        if (i == 0) {
            float x0 = x;
            float x1 = x + trapW;
            float topY = y + h * 0.22f;
            float botY = y + h * 0.78f;
            return new float[][]{{x0, y}, {x1, topY}, {x1, botY}, {x0, y + h}};
        }
        float tipX = x + trapW + (i - 1) * (halfW * 2 + gap);
        float cx = tipX + halfW;
        float cy = y + h / 2f;
        return new float[][]{{tipX, cy}, {cx, y}, {tipX + halfW * 2, cy}, {cx, y + h}};
    }

    /** 以 (cx, cy) 为中心等比缩放多边形（cx/cy 可带偏移，用于亮核上移） */
    private static float[][] expand(float[][] corners, float cx, float cy, float factor) {
        float[][] out = new float[corners.length][2];
        for (int i = 0; i < corners.length; i++) {
            out[i][0] = cx + (corners[i][0] - cx) * factor;
            out[i][1] = cy + (corners[i][1] - cy) * factor;
        }
        return out;
    }

    /** 扫描线填充凸多边形（GuiGraphics 只有矩形填充，逐行绘制 1px 高的横条） */
    private static void fillPolygon(GuiGraphics graphics, float[][] corners, int color) {
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (float[] c : corners) {
            minY = Math.min(minY, c[1]);
            maxY = Math.max(maxY, c[1]);
        }
        for (int y = (int) Math.floor(minY); y < (int) Math.ceil(maxY); y++) {
            float sy = y + 0.5f;
            float xMin = Float.MAX_VALUE;
            float xMax = -Float.MAX_VALUE;
            for (int i = 0; i < corners.length; i++) {
                float[] a = corners[i];
                float[] b = corners[(i + 1) % corners.length];
                if ((a[1] <= sy && b[1] > sy) || (b[1] <= sy && a[1] > sy)) {
                    float t = (sy - a[1]) / (b[1] - a[1]);
                    float ix = a[0] + t * (b[0] - a[0]);
                    xMin = Math.min(xMin, ix);
                    xMax = Math.max(xMax, ix);
                }
            }
            if (xMax > xMin) {
                graphics.fill((int) Math.floor(xMin), y, (int) Math.ceil(xMax), y + 1, color);
            }
        }
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    private static int getRemainingSeconds() {
        if (currentStartTime <= 0) return 0;
        long elapsed = (System.currentTimeMillis() - currentStartTime) / 1000;
        return Math.max(0, currentGen.getTransformDurationSeconds() - (int) elapsed);
    }
}
