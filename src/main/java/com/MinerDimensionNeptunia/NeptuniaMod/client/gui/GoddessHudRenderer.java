package com.MinerDimensionNeptunia.NeptuniaMod.client.gui;

import com.MinerDimensionNeptunia.NeptuniaMod.config.GoddessColorConfig;
import com.MinerDimensionNeptunia.NeptuniaMod.config.GoddessConfig;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.Goddess;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.GoddessRegistry;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessDiskGen;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class GoddessHudRenderer {
    private static final int CELLS = 5, W = 184, H = 52;
    private static GoddessType currentType = GoddessType.NONE;
    private static GoddessDiskGen currentGen = GoddessDiskGen.GEN5;
    private static long currentStartTime;
    private static boolean isActive;

    private GoddessHudRenderer() {}

    public static void updateState(GoddessType type, GoddessDiskGen gen, long startTime) {
        currentType = type; currentGen = gen; currentStartTime = startTime; isActive = startTime > 0;
    }

    public static void resetState() {
        currentType = GoddessType.NONE; currentGen = GoddessDiskGen.GEN5; currentStartTime = 0; isActive = false;
    }

    public static void render(GuiGraphics g, float partialTick) {
        if (!isActive) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.level == null) return;
        int seconds = remaining();
        if (seconds <= 0) return;

        int guiWidth = mc.getWindow().getGuiScaledWidth();
        float resolutionScale = guiWidth <= 640 ? 0.60F : guiWidth <= 960 ? 0.78F : 1.0F;
        double scale = GoddessConfig.CLIENT.hudScale.get() * resolutionScale;
        int x = guiWidth - (int)Math.ceil(W * scale) - GoddessConfig.CLIENT.hudOffsetX.get();
        int y = mc.getWindow().getGuiScaledHeight() - (int)Math.ceil(H * scale) - GoddessConfig.CLIENT.hudOffsetY.get();
        GoddessColorConfig.GoddessColors c = GoddessColorConfig.getColors(currentType);
        Goddess goddess = GoddessRegistry.getInstance().getGoddess(currentType);
        String name = goddess == null ? currentType.name() : goddess.getDisplayName();
        float energy = Math.max(0F, Math.min(1F, (float)seconds / currentGen.getTransformDurationSeconds()));

        g.pose().pushPose();
        g.pose().translate(Math.max(0, x), Math.max(0, y), 0);
        g.pose().scale((float)scale, (float)scale, 1F);
        panel(g, c);
        String heading = "TRANSFORM // " + name;
        g.drawString(mc.font, shorten(mc, heading, 132), 43, 12, argb(c.text.getRGB(), 255), true);
        gauge(g, mc, energy, partialTick, c);
        String time = String.format("%02d:%02d", seconds / 60, seconds % 60);
        g.drawString(mc.font, String.format("ENERGY  %3d%%", Math.round(energy * 100)), 43, 43, 0xFFC8CBD7, false);
        g.drawString(mc.font, time, 174 - mc.font.width(time), 43, argb(c.text.getRGB(), 255), true);
        g.pose().popPose();
    }

    private static void panel(GuiGraphics g, GoddessColorConfig.GoddessColors c) {
        quad(g, 3, 3, 181, 49, 8, 0x88000000);
        quad(g, 0, 0, 179, 47, 8, 0xD9101018);
        quad(g, 2, 2, 175, 45, 6, argb(c.background.getRGB(), 105));
        quad(g, 4, 5, 34, 45, 5, argb(c.main.getRGB(), 230));
        quad(g, 7, 8, 30, 42, 4, argb(c.secondary.getRGB(), 150));
        g.fill(38, 8, 40, 43, argb(c.border.getRGB(), 220));
        g.fill(43, 23, 174, 25, 0x667FFFFF);
    }

    private static void gauge(GuiGraphics g, Minecraft mc, float energy, float tick, GoddessColorConfig.GoddessColors c) {
        int gap = 3, width = 24, full = (int)Math.ceil(energy * CELLS);
        float pulse = .86F + .14F * (float)Math.sin((mc.level.getGameTime() + tick) * .18F);
        for (int i = 0; i < CELLS; i++) {
            int left = 42 + i * (width + gap);
            boolean lit = i < full;
            cell(g, left, 28, width, 12, 4, lit ? argb(c.border.getRGB(), 245) : 0x763D424F);
            if (lit) {
                int shade = mix(c.secondary.getRGB(), c.main.getRGB(), i / 4F);
                cell(g, left + 2, 30, width - 4, 8, 3, argb(dim(shade, pulse), 255));
                g.fill(left + 5, 31, left + width - 4, 32, 0x78FFFFFF);
            } else g.fill(left + 5, 33, left + width - 5, 35, 0x306B7080);
        }
    }

    private static String shorten(Minecraft mc, String text, int max) {
        if (mc.font.width(text) <= max) return text;
        int end = text.length();
        while (end > 0 && mc.font.width(text.substring(0, end) + "...") > max) end--;
        return text.substring(0, end) + "...";
    }

    private static void quad(GuiGraphics g, int l, int t, int r, int b, int s, int color) {
        poly(g, new float[][]{{l+s,t},{r,t},{r-s,b},{l,b}}, color);
    }

    private static void cell(GuiGraphics g, int l, int t, int w, int h, int n, int color) {
        poly(g, new float[][]{{l+n,t},{l+w,t},{l+w-n,t+h/2F},{l+w,t+h},{l+n,t+h},{l,t+h/2F}}, color);
    }

    private static void poly(GuiGraphics g, float[][] p, int color) {
        float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
        for (float[] v : p) { lo = Math.min(lo, v[1]); hi = Math.max(hi, v[1]); }
        for (int y = (int)Math.floor(lo); y < (int)Math.ceil(hi); y++) {
            float sy = y + .5F, left = Float.MAX_VALUE, right = -Float.MAX_VALUE;
            for (int i = 0; i < p.length; i++) {
                float[] a = p[i], b = p[(i+1)%p.length];
                if ((a[1] <= sy && b[1] > sy) || (b[1] <= sy && a[1] > sy)) {
                    float hit = a[0] + (sy-a[1])/(b[1]-a[1])*(b[0]-a[0]);
                    left = Math.min(left, hit); right = Math.max(right, hit);
                }
            }
            if (right > left) g.fill((int)Math.floor(left), y, (int)Math.ceil(right), y+1, color);
        }
    }

    private static int argb(int rgb, int a) { return (a & 255) << 24 | rgb & 0xFFFFFF; }
    private static int dim(int rgb, float n) {
        return Math.min(255, Math.round(((rgb>>16)&255)*n))<<16 | Math.min(255, Math.round(((rgb>>8)&255)*n))<<8 | Math.min(255, Math.round((rgb&255)*n));
    }
    private static int mix(int a, int b, float t) {
        return Math.round(((a>>16)&255)+(((b>>16)&255)-((a>>16)&255))*t)<<16 | Math.round(((a>>8)&255)+(((b>>8)&255)-((a>>8)&255))*t)<<8 | Math.round((a&255)+((b&255)-(a&255))*t);
    }
    private static int remaining() {
        if (currentStartTime <= 0) return 0;
        return Math.max(0, currentGen.getTransformDurationSeconds() - (int)((System.currentTimeMillis()-currentStartTime)/1000));
    }
}
