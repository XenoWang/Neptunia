package com.MinerDimensionNeptunia.NeptuniaMod.client;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import com.MinerDimensionNeptunia.NeptuniaMod.client.gui.GoddessHudRenderer;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.Goddess;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.GoddessRegistry;
import com.MinerDimensionNeptunia.NeptuniaMod.network.TransformRequestPacket;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Neptunia.MODID, value = Dist.CLIENT)
public class ClientEvents {
    private static boolean cachedGoddessAbility = false;
    private static long cachedTransformStartTime = 0;
    private static GoddessType cachedGoddessType = GoddessType.NONE;
    private static boolean isLocallyTransformed = false;

    private static final int TRANSFORM_DURATION = 180;

    public static void setGoddessAbility(boolean value) {
        cachedGoddessAbility = value;
        System.out.println("💾 [客户端] 能力缓存更新为: " + value);
    }

    public static void setTransformStartTime(long time) {
        cachedTransformStartTime = time;
        isLocallyTransformed = (time > 0);
        GoddessHudRenderer.updateState(cachedGoddessType, time);
        System.out.println("💾 [客户端] 变身时间缓存更新为: " + time + "，状态: " + isLocallyTransformed);
    }

    public static void setGoddessType(GoddessType type) {
        cachedGoddessType = type;
        System.out.println("💾 [客户端] 女神类型缓存更新为: " + type);
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        cachedGoddessAbility = false;
        cachedTransformStartTime = 0;
        cachedGoddessType = GoddessType.NONE;
        isLocallyTransformed = false;
        GoddessHudRenderer.resetState();
        System.out.println("💾 [客户端] 离开世界，状态已重置");
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() == Minecraft.getInstance().player) {
            cachedTransformStartTime = 0;
            isLocallyTransformed = false;
            GoddessHudRenderer.resetState();
            System.out.println("💀 [客户端] 玩家死亡，变身状态已重置");
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (KeyBindings.transformKey.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                if (!cachedGoddessAbility) {
                    mc.player.displayClientMessage(Component.literal("你没有女神化的能力！请使用女神磁盘。"), true);
                    return;
                }
                if (cachedGoddessType == GoddessType.NONE) {
                    mc.player.displayClientMessage(Component.literal("请先使用女神磁盘选择你的女神！"), true);
                    return;
                }
                if (isLocallyTransformed) {
                    mc.player.displayClientMessage(Component.literal("你已经在变身状态中！"), true);
                    return;
                }
                Neptunia.CHANNEL.sendToServer(new TransformRequestPacket(true));
                System.out.println("📤 [客户端] 发送变身请求到服务端");
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (isLocallyTransformed && getRemainingSeconds() <= 0) {
            Neptunia.CHANNEL.sendToServer(new TransformRequestPacket(false));
            isLocallyTransformed = false;
            cachedTransformStartTime = 0;
            GoddessHudRenderer.resetState();
            System.out.println("⏰ [客户端] 变身超时，自动发送解除请求");
            return;
        }

        // ⭐ 委托给 HUD 渲染器
        GoddessHudRenderer.render(event.getGuiGraphics(), event.getPartialTick());
    }

    private static int getRemainingSeconds() {
        if (cachedTransformStartTime <= 0) return 0;
        long elapsed = (System.currentTimeMillis() - cachedTransformStartTime) / 1000;
        return Math.max(0, TRANSFORM_DURATION - (int) elapsed);
    }
}