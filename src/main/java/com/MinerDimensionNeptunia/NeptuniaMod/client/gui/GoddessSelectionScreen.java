package com.MinerDimensionNeptunia.NeptuniaMod.client.gui;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import com.MinerDimensionNeptunia.NeptuniaMod.client.DevPlayerList;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.Goddess;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.GoddessRegistry;
import com.MinerDimensionNeptunia.NeptuniaMod.network.GoddessTypeSelectPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * 女神选择界面（游戏王卡牌风格）：
 * 每张卡牌上方为女神立绘框，下方为女神介绍；
 * 每行最多 4 张卡牌，根据界面宽度自动调整，放不下时自动换行；
 * 卡牌文字大小按卡牌宽度分为 4 档（只在初始化时计算一次）。
 */
public class GoddessSelectionScreen extends Screen {
    /** 卡牌默认尺寸（2 倍 GUI 缩放下，一行约可放 3 张） */
    private static final int CARD_WIDTH = 110;
    private static final int CARD_HEIGHT = 170;
    /** 每行最多展示的卡牌数量 */
    private static final int MAX_CARDS_PER_ROW = 4;
    /** 卡牌之间的间距 */
    private static final int CARD_GAP = 12;
    /** 页面左右留白 */
    private static final int PAGE_MARGIN = 20;
    /** 顶部标题占用的高度 */
    private static final int TITLE_AREA = 26;
    /** 底部取消按钮占用的高度 */
    private static final int BUTTON_AREA = 42;

    /** 文字缩放档位（根据卡牌宽度选择，宽度越小字号越小，但保证最小档仍可读） */
    private static final float[] TEXT_SCALES = {1.0F, 0.85F, 0.72F, 0.62F};
    private static final int[] TEXT_SCALE_WIDTHS = {100, 75, 55};

    // ---- 卡牌配色（金色外框 + 棕色卡面） ----
    private static final int CARD_BORDER = 0xFFC8A24B;
    private static final int CARD_BORDER_HOVER = 0xFFF5D265;
    private static final int CARD_BODY = 0xFF4E342E;
    private static final int NAME_STRIP = 0xFF33241A;
    private static final int NAME_TEXT = 0xFFE8C56B;
    private static final int ART_FRAME = 0xFF1B120B;
    private static final int DESC_TEXT = 0xFFE9DFC7;
    private static final int HOVER_OVERLAY = 0x22FFFFFF;

    public GoddessSelectionScreen() {
        super(Component.literal("选择女神"));
    }

    @Override
    protected void init() {
        List<Goddess> goddesses = new ArrayList<>(GoddessRegistry.getInstance().getAllGoddesses());

        // 过滤开发者专属女神：仅开发者名单（JSON 文件）内的玩家可见（界面会按剩余数量自动重排）
        String playerName = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getGameProfile().getName()
                : "";
        boolean isDevPlayer = DevPlayerList.isDevPlayer(playerName);
        goddesses.removeIf(goddess -> goddess.isDevOnly() && !isDevPlayer);

        int usableWidth = Math.max(1, this.width - PAGE_MARGIN * 2);
        int usableHeight = Math.max(1, this.height - TITLE_AREA - BUTTON_AREA);

        // ============================================================
        // 自适应布局：遍历所有可能的"每行张数"（最多 4 张），
        // 选出让卡牌最大的一种排法（卡牌过多时按比例缩小并自动换行）
        // ============================================================
        int bestPerRow = 1;
        int bestRows = Math.max(1, goddesses.size());
        int bestCardW = 0;
        int bestCardH = 0;
        long bestArea = -1;
        int maxPerRow = Math.min(goddesses.size(), MAX_CARDS_PER_ROW);

        for (int perRow = 1; perRow <= maxPerRow; perRow++) {
            int rows = (goddesses.size() + perRow - 1) / perRow;

            // 宽度与高度各自允许的最大卡牌尺寸
            int widthLimit = (usableWidth - (perRow - 1) * CARD_GAP) / perRow;
            int heightLimit = (usableHeight - (rows - 1) * CARD_GAP) / rows;
            if (widthLimit <= 0 || heightLimit <= 0)
                continue;

            int cardW = Math.min(CARD_WIDTH, widthLimit);
            int cardH = Math.min(CARD_HEIGHT, heightLimit);

            // 保持宽高比，取两个限制中更紧的一个
            int widthByHeight = cardH * CARD_WIDTH / CARD_HEIGHT;
            if (widthByHeight < cardW) {
                cardW = widthByHeight; // 高度是瓶颈，按比例缩窄
            } else {
                cardH = cardW * CARD_HEIGHT / CARD_WIDTH; // 宽度是瓶颈，按比例降低高度
            }

            long area = (long) cardW * cardH;
            if (area > bestArea) {
                bestArea = area;
                bestPerRow = perRow;
                bestRows = rows;
                bestCardW = cardW;
                bestCardH = cardH;
            }
        }

        int totalHeight = bestRows * bestCardH + (bestRows - 1) * CARD_GAP;
        int startY = TITLE_AREA + (usableHeight - totalHeight) / 2;

        // 按行放置卡牌，一行放满后自动换行（每行独立居中，末行不满也会居中）
        for (int i = 0; i < goddesses.size(); i++) {
            int row = i / bestPerRow;
            int col = i % bestPerRow;
            int cardsInRow = Math.min(bestPerRow, goddesses.size() - row * bestPerRow);
            int rowWidth = cardsInRow * bestCardW + (cardsInRow - 1) * CARD_GAP;
            int x = (this.width - rowWidth) / 2 + col * (bestCardW + CARD_GAP);
            int y = startY + row * (bestCardH + CARD_GAP);
            this.addRenderableWidget(new GoddessCardWidget(x, y, bestCardW, bestCardH, goddesses.get(i)));
        }

        // 底部居中的取消按钮
        this.addRenderableWidget(Button.builder(Component.literal("取消"), button -> this.onClose())
                .pos(this.width / 2 - 50, this.height - 30)
                .size(100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, "请选择你的女神", this.width / 2, 10, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** 女神卡牌控件：上方立绘框，下方介绍文字 */
    private class GoddessCardWidget extends AbstractWidget {
        private final Goddess goddess;
        /** 文字缩放档位：仅在创建时按卡牌宽度选择一次，渲染时不做任何计算 */
        private final float textScale;

        GoddessCardWidget(int x, int y, int width, int height, Goddess goddess) {
            super(x, y, width, height, Component.literal(goddess.getDisplayName()));
            this.goddess = goddess;
            this.textScale = pickTextScale(width);
        }

        /** 按卡牌宽度选择文字缩放档位（4 档） */
        private float pickTextScale(int cardWidth) {
            float scale = TEXT_SCALES[0];
            for (int i = 0; i < TEXT_SCALE_WIDTHS.length; i++) {
                if (cardWidth < TEXT_SCALE_WIDTHS[i]) {
                    scale = TEXT_SCALES[i + 1];
                }
            }
            return scale;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            Neptunia.CHANNEL.sendToServer(new GoddessTypeSelectPacket(goddess.getId()));
            System.out.println("📤 [客户端] 选择女神: " + goddess.getDisplayName());
            GoddessSelectionScreen.this.onClose();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Font font = GoddessSelectionScreen.this.font;
            int x = this.getX();
            int y = this.getY();
            int w = this.width;
            int h = this.height;
            boolean hovered = this.isHoveredOrFocused();

            int nameH = Math.max(10, Math.min(14, h / 10)); // 顶部名字条高度
            // 立绘框高度：介绍区域保底 36px，保证小卡上文字也有足够空间
            int artH = Math.min(90, Math.max(28, h - 46));
            int descH = h - nameH - artH;                   // 下方介绍区域高度

            // ---- 卡面：外框 + 底色 ----
            graphics.fill(x, y, x + w, y + h, hovered ? CARD_BORDER_HOVER : CARD_BORDER);
            graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, CARD_BODY);

            // ---- 名字条（按档位缩放绘制） ----
            graphics.fill(x + 2, y + 2, x + w - 2, y + nameH, NAME_STRIP);
            String name = goddess.getDisplayName();
            String shownName = font.plainSubstrByWidth(name, Math.max(1, (int) ((w - 10) / textScale)));
            if (!shownName.equals(name)) {
                shownName = shownName + "…";
            }
            int textHeight = Math.round(font.lineHeight * textScale);
            graphics.pose().pushPose();
            graphics.pose().scale(textScale, textScale, 1.0F);
            graphics.drawCenteredString(font, shownName,
                    Math.round((x + w / 2.0F) / textScale),
                    Math.round((y + 2 + (nameH - textHeight) / 2.0F) / textScale),
                    NAME_TEXT);
            graphics.pose().popPose();

            // ---- 立绘框 ----
            int artX = x + 2;
            int artY = y + nameH;
            int artW = w - 4;
            graphics.fill(artX, artY, artX + artW, artY + artH, ART_FRAME);
            drawGoddessArt(graphics, artX + 2, artY + 2, artW - 4, artH - 4);

            // ---- 介绍文字 ----
            drawDescription(graphics, x + 4, artY + artH + 4, w - 8, descH - 6);

            // ---- 悬停高亮遮罩 ----
            if (hovered) {
                graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, HOVER_OVERLAY);
            }
        }

        /** 在指定区域内等比缩放绘制立绘（保持宽高比并居中） */
        private void drawGoddessArt(GuiGraphics graphics, int x, int y, int w, int h) {
            ResourceLocation texture = goddess.getArtTexture();
            if (texture == null || w <= 0 || h <= 0)
                return;
            int texW = goddess.getArtTextureWidth();
            int texH = goddess.getArtTextureHeight();
            double scale = Math.min((double) w / texW, (double) h / texH);
            int drawW = (int) (texW * scale);
            int drawH = (int) (texH * scale);
            int drawX = x + (w - drawW) / 2;
            int drawY = y + (h - drawH) / 2;
            graphics.blit(texture, drawX, drawY, drawW, drawH, 0.0F, 0.0F, texW, texH, texW, texH);
        }

        /** 在介绍区域绘制自动换行的介绍文字（按档位缩放，超出区域部分截断） */
        private void drawDescription(GuiGraphics graphics, int x, int y, int w, int h) {
            String desc = goddess.getDescription();
            if (desc == null || desc.isEmpty() || h <= 0)
                return;
            Font font = GoddessSelectionScreen.this.font;
            int scaledWidth = Math.max(1, (int) (w / textScale));
            List<FormattedCharSequence> lines = font.split(Component.literal(desc), scaledWidth);
            int maxLines = (int) (h / (font.lineHeight * textScale));

            graphics.pose().pushPose();
            graphics.pose().scale(textScale, textScale, 1.0F);
            int sx = Math.round(x / textScale);
            int sy = Math.round(y / textScale);
            for (int i = 0; i < lines.size() && i < maxLines; i++) {
                graphics.drawString(font, lines.get(i), sx, sy + i * font.lineHeight, DESC_TEXT);
            }
            graphics.pose().popPose();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            this.defaultButtonNarrationText(narration);
        }
    }
}
