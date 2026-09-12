package com.MinerDimensionNeptunia.NeptuniaMod.network;

import com.MinerDimensionNeptunia.NeptuniaMod.capability.GoddessCapabilityProvider;
import com.MinerDimensionNeptunia.NeptuniaMod.client.ClientEvents;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GoddessAbilitySyncPacket {
    private final boolean hasAbility;
    private final long transformStartTime;
    private final GoddessType goddessType;

    public GoddessAbilitySyncPacket(boolean hasAbility, long transformStartTime, GoddessType goddessType) {
        this.hasAbility = hasAbility;
        this.transformStartTime = transformStartTime;
        this.goddessType = goddessType;
    }

    public static void encode(GoddessAbilitySyncPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.hasAbility);
        buf.writeLong(msg.transformStartTime);
        buf.writeUtf(msg.goddessType.name());
    }

    public static GoddessAbilitySyncPacket decode(FriendlyByteBuf buf) {
        return new GoddessAbilitySyncPacket(
                buf.readBoolean(),
                buf.readLong(),
                GoddessType.fromName(buf.readUtf())
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
                });
                ClientEvents.setGoddessAbility(msg.hasAbility);
                ClientEvents.setTransformStartTime(msg.transformStartTime);
                ClientEvents.setGoddessType(msg.goddessType);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}