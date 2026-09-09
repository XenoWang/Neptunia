package com.MinerDimensionNeptunia.NeptuniaMod.network;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import com.MinerDimensionNeptunia.NeptuniaMod.capability.GoddessCapabilityProvider;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.GoddessRegistry;
import com.MinerDimensionNeptunia.NeptuniaMod.item.GoddessDiskItem;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class GoddessTypeSelectPacket {
    private final GoddessType selectedType;

    public GoddessTypeSelectPacket(GoddessType type) {
        this.selectedType = type;
    }

    public static void encode(GoddessTypeSelectPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.selectedType.name());
    }

    public static GoddessTypeSelectPacket decode(FriendlyByteBuf buf) {
        return new GoddessTypeSelectPacket(GoddessType.fromName(buf.readUtf()));
    }

    public static void handle(GoddessTypeSelectPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                System.out.println("❌ [服务端] 选择女神类型失败：sender 为 null");
                return;
            }

            // 验证女神是否已注册
            if (!GoddessRegistry.getInstance().isRegistered(msg.selectedType)) {
                player.sendSystemMessage(Component.literal("该女神尚未开放，请选择其他女神。"));
                return;
            }

            player.getCapability(GoddessCapabilityProvider.GODDESS_CAPABILITY).ifPresent(cap -> {
                boolean hadAbility = cap.getAbility();

                cap.setGoddessType(msg.selectedType);

                if (!hadAbility) {
                    cap.setAbility(true);
                    cap.setTransformStartTime(0);
                    consumeGoddessDisk(player);
                    player.sendSystemMessage(Component.literal("你获得了女神化的能力，并选择了 " + msg.selectedType.name()));
                } else {
                    consumeGoddessDisk(player);
                    player.sendSystemMessage(Component.literal("更换女神为 " + msg.selectedType.name()));
                }

                // 更新服务端缓存
                Neptunia.updatePlayerCache(
                        player.getUUID(),
                        cap.getAbility(),
                        cap.getGoddessType(),
                        cap.getTransformStartTime()
                );

                // 同步给客户端
                Neptunia.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new GoddessAbilitySyncPacket(
                                cap.getAbility(),
                                cap.getTransformStartTime(),
                                msg.selectedType
                        )
                );
                System.out.println("✅ [服务端] 玩家 " + player.getName().getString() +
                        " 选择/更换女神类型: " + msg.selectedType + "，缓存已更新");
            });
        });
        context.setPacketHandled(true);
    }

    private static void consumeGoddessDisk(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof GoddessDiskItem) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
                return;
            }
        }
        System.out.println("⚠️ [服务端] 未找到 goddess_disk 物品，无法消耗");
    }
}