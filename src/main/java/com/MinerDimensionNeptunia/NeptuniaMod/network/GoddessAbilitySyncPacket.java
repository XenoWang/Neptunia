package com.MinerDimensionNeptunia.NeptuniaMod.network;

import com.MinerDimensionNeptunia.NeptuniaMod.capability.GoddessCapabilityProvider;
import com.MinerDimensionNeptunia.NeptuniaMod.client.ClientEvents;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessDiskGen;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：同步女神化能力状态（能力 / 变身开始时间 / 女神类型 / 磁盘世代）。
 * 磁盘世代影响客户端倒计时时长与 HUD 显示，必须随包同步。
 */
public class GoddessAbilitySyncPacket {
    private final boolean hasAbility;
    private final long transformStartTime;
    private final GoddessType goddessType;
    private final GoddessDiskGen diskGen;

    public GoddessAbilitySyncPacket(boolean hasAbility, long transformStartTime,
                                    GoddessType goddessType, GoddessDiskGen diskGen) {
        this.hasAbility = hasAbility;
        this.transformStartTime = transformStartTime;
        this.goddessType = goddessType;
        this.diskGen = diskGen;
    }

    public static void encode(GoddessAbilitySyncPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.hasAbility);
        buf.writeLong(msg.transformStartTime);
        buf.writeUtf(msg.goddessType.name());
        buf.writeUtf(msg.diskGen.name());
    }

    public static GoddessAbilitySyncPacket decode(FriendlyByteBuf buf) {
        return new GoddessAbilitySyncPacket(
                buf.readBoolean(),
                buf.readLong(),
                GoddessType.fromName(buf.readUtf()),
                GoddessDiskGen.fromName(buf.readUtf())
        );
    }

    public static void handle(GoddessAbilitySyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.getCapability(GoddessCapabilityProvider.GODDESS_CAPABILITY).ifPresent(cap -> {
                    cap.setAbility(msg.hasAbility);
                    cap.setTransformStartTime(msg.transformStartTime);
                    cap.setGoddessType(msg.goddessType);
                    cap.setDiskGen(msg.diskGen);
                });
                ClientEvents.setGoddessAbility(msg.hasAbility);
                ClientEvents.setTransformStartTime(msg.transformStartTime);
                ClientEvents.setGoddessType(msg.goddessType);
                ClientEvents.setGoddessGen(msg.diskGen);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
