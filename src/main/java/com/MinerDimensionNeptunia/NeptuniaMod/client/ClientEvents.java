package com.MinerDimensionNeptunia.NeptuniaMod.client;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import com.MinerDimensionNeptunia.NeptuniaMod.client.gui.GoddessHudRenderer;
import com.MinerDimensionNeptunia.NeptuniaMod.network.TransformRequestPacket;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessDiskGen;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Neptunia.MODID, value = Dist.CLIENT)
public class ClientEvents {
    private static boolean cachedGoddessAbility = false;
    private static long cachedTransformStartTime = 0;
    private static GoddessType cachedGoddessType = GoddessType.NONE;
    private static GoddessDiskGen cachedGoddessGen = GoddessDiskGen.GEN5;
    private static boolean isLocallyTransformed = false;

    public static void setGoddessAbility(boolean value) {
        cachedGoddessAbility = value;
    }

    public static void setTransformStartTime(long time) {
        cachedTransformStartTime = time;
        isLocallyTransformed = (time > 0);
        GoddessHudRenderer.updateState(cachedGoddessType, cachedGoddessGen, time);
    }

    public static void setGoddessType(GoddessType type) {
        cachedGoddessType = type;
    }

    public static void setGoddessGen(GoddessDiskGen gen) {
        cachedGoddessGen = gen;
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        cachedGoddessAbility = false;
        cachedTransformStartTime = 0;
        cachedGoddessType = GoddessType.NONE;
        cachedGoddessGen = GoddessDiskGen.GEN5;
        isLocallyTransformed = false;
        GoddessHudRenderer.resetState();
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() == Minecraft.getInstance().player) {
            cachedTransformStartTime = 0;
            isLocallyTransformed = false;
            GoddessHudRenderer.resetState();
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
            }
        }
    }

    /** 客户端变身倒计时归零 → 通知服务端解除变身（每 tick 一次，而非渲染事件里判断） */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (isLocallyTransformed && getRemainingSeconds() <= 0) {
            Neptunia.CHANNEL.sendToServer(new TransformRequestPacket(false));
            isLocallyTransformed = false;
            cachedTransformStartTime = 0;
            GoddessHudRenderer.resetState();
        }
    }

    /** RenderGuiEvent 每帧只触发一次（不像覆盖层事件那样每层都触发） */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        GoddessHudRenderer.render(event.getGuiGraphics(), event.getPartialTick());
    }

    private static int getRemainingSeconds() {
        if (cachedTransformStartTime <= 0)
            return 0;
        long elapsed = (System.currentTimeMillis() - cachedTransformStartTime) / 1000;
        return Math.max(0, cachedGoddessGen.getTransformDurationSeconds() - (int) elapsed);
    }
}