package com.MinerDimensionNeptunia.NeptuniaMod.network;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import com.MinerDimensionNeptunia.NeptuniaMod.capability.GoddessCapabilityProvider;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.Goddess;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.GoddessRegistry;
import com.MinerDimensionNeptunia.NeptuniaMod.item.usable.GoddessDiskItem;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
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
                return;
            }

            // 验证女神是否已注册
            if (!GoddessRegistry.getInstance().isRegistered(msg.selectedType)) {
                player.sendSystemMessage(Component.literal("该女神尚未开放，请选择其他女神。"));
                return;
            }

            player.getCapability(GoddessCapabilityProvider.GODDESS_CAPABILITY).ifPresent(cap -> {
                // ⭐ 新增：如果已拥有能力，拒绝处理（防止作弊/重复使用）
                if (cap.getAbility()) {
                    player.sendSystemMessage(Component.literal("你已经拥有女神化的能力了！"));
                    return;
                }

                // 未拥有能力，正常处理
                cap.setAbility(true);
                cap.setGoddessType(msg.selectedType);
                cap.setTransformStartTime(0);
                consumeGoddessDisk(player);

                // 给予该女神的默认武器
                Goddess goddess = GoddessRegistry.getInstance().getGoddess(msg.selectedType);
                if (goddess != null) {
                    giveStarterWeapon(player, goddess);
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

                player.sendSystemMessage(Component.literal("你获得了女神化的能力，并选择了 " + msg.selectedType.name()));
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
    }

    /**
     * 给予该女神的默认武器：已拥有同种武器则不重复给予；
     * 背包已满时掉落在玩家脚下。
     */
    private static void giveStarterWeapon(ServerPlayer player, Goddess goddess) {
        Item weapon = goddess.getStarterWeapon();
        if (weapon == null) {
            return;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(weapon)) {
                return; // 已拥有
            }
        }
        ItemStack stack = new ItemStack(weapon);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}