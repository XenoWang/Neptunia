package com.MinerDimensionNeptunia.NeptuniaMod.network;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import com.MinerDimensionNeptunia.NeptuniaMod.capability.GoddessCapabilityProvider;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.Goddess;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.GoddessRegistry;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class TransformRequestPacket {
    private final boolean startTransform;

    // 不再使用固定 UUID，每个女神有自己的 UUID（从 Goddess 对象获取）
    // 保留一个默认的用于向后兼容，但最好使用女神的 UUID
    private static final UUID LEGACY_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    public TransformRequestPacket(boolean startTransform) {
        this.startTransform = startTransform;
    }

    public static void encode(TransformRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.startTransform);
    }

    public static TransformRequestPacket decode(FriendlyByteBuf buf) {
        return new TransformRequestPacket(buf.readBoolean());
    }

    public static void handle(TransformRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                System.out.println("❌ [服务端] 变身请求处理失败：sender 为 null");
                return;
            }

            UUID uuid = player.getUUID();

            player.getCapability(GoddessCapabilityProvider.GODDESS_CAPABILITY).ifPresent(cap -> {
                if (msg.startTransform) {
                    // 开始变身
                    if (!cap.getAbility()) {
                        player.sendSystemMessage(Component.literal("你没有女神化的能力！"));
                        return;
                    }
                    if (cap.getTransformStartTime() > 0) {
                        player.sendSystemMessage(Component.literal("你已经处于变身状态！"));
                        return;
                    }
                    GoddessType type = cap.getGoddessType();
                    if (type == GoddessType.NONE) {
                        player.sendSystemMessage(Component.literal("请先选择你的女神！"));
                        return;
                    }

                    // 从注册中心获取女神对象
                    Goddess goddess = GoddessRegistry.getInstance().getGoddess(type);
                    if (goddess == null) {
                        player.sendSystemMessage(Component.literal("女神数据未加载，请联系管理员！"));
                        return;
                    }

                    long startTime = System.currentTimeMillis();
                    cap.setTransformStartTime(startTime);
                    System.out.println("✅ [服务端] 玩家 " + player.getName().getString() +
                            " 开始变身，女神: " + goddess.getDisplayName() + "，时间: " + startTime);

                    // 应用属性加成
                    applyGoddessBoost(player, goddess, true);

                    // 调用女神的特殊效果（预留）
                    goddess.onTransformStart(player);

                    // 更新缓存
                    Neptunia.updatePlayerCache(uuid, cap.getAbility(), type, startTime);

                    Neptunia.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new GoddessAbilitySyncPacket(cap.getAbility(), startTime, type)
                    );
                    player.sendSystemMessage(Component.literal("变身！"));

                } else {
                    // 解除变身
                    if (cap.getTransformStartTime() == 0) {
                        player.sendSystemMessage(Component.literal("你不在变身状态！"));
                        return;
                    }
                    GoddessType type = cap.getGoddessType();
                    Goddess goddess = GoddessRegistry.getInstance().getGoddess(type);
                    if (goddess != null) {
                        applyGoddessBoost(player, goddess, false);
                        goddess.onTransformEnd(player);
                    } else {
                        // 降级处理：使用默认移除方式
                        applyLegacyBoostRemoval(player);
                    }
                    cap.setTransformStartTime(0);
                    System.out.println("✅ [服务端] 玩家 " + player.getName().getString() + " 解除变身");

                    Neptunia.updatePlayerCache(uuid, cap.getAbility(), type, 0);

                    Neptunia.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new GoddessAbilitySyncPacket(cap.getAbility(), 0, type)
                    );
                    player.sendSystemMessage(Component.literal("解除变身！"));
                }
            });
        });
        context.setPacketHandled(true);
    }

    /**
     * 应用或移除女神的属性加成
     */
    public static void applyGoddessBoost(ServerPlayer player, Goddess goddess, boolean apply) {
        if (goddess == null) return;
        UUID boostUUID = goddess.getBoostUUID();

        for (Map.Entry<Attribute, Double> entry : goddess.getAttributeMultipliers().entrySet()) {
            Attribute attr = entry.getKey();
            double multiplier = entry.getValue();
            AttributeInstance instance = player.getAttribute(attr);
            if (instance == null) continue;

            AttributeModifier existing = instance.getModifier(boostUUID);
            if (apply) {
                if (existing != null) instance.removeModifier(existing);
                AttributeModifier modifier = new AttributeModifier(boostUUID,
                        "goddess_boost_" + goddess.getId().name(),
                        multiplier - 1.0,
                        AttributeModifier.Operation.MULTIPLY_BASE);
                instance.addPermanentModifier(modifier);
            } else {
                if (existing != null) instance.removeModifier(existing);
            }
        }
    }

    /**
     * 降级处理：移除旧的加成（用于兼容旧版本数据）
     */
    private static void applyLegacyBoostRemoval(ServerPlayer player) {
        // 如果女神不存在，尝试移除所有可能的加成（使用固定 UUID）
        // 实际上，由于我们使用女神自己的 UUID，此方法主要用于容错
        // 可以遍历常用属性移除固定 UUID
        Attribute[] commonAttrs = {
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
                net.minecraft.world.entity.ai.attributes.Attributes.ARMOR,
                net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS
        };
        for (Attribute attr : commonAttrs) {
            AttributeInstance instance = player.getAttribute(attr);
            if (instance == null) continue;
            AttributeModifier existing = instance.getModifier(LEGACY_UUID);
            if (existing != null) instance.removeModifier(existing);
        }
    }
}