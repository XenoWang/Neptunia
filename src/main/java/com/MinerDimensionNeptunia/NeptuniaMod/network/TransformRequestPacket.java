package com.MinerDimensionNeptunia.NeptuniaMod.network;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import com.MinerDimensionNeptunia.NeptuniaMod.capability.GoddessCapabilityProvider;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.Goddess;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.GoddessRegistry;
import com.MinerDimensionNeptunia.NeptuniaMod.item.usable.GoddessFlightHandler;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessDiskGen;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
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
                return;
            }

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
                            " 变身，女神: " + goddess.getDisplayName() + "，时间: " + startTime);

                    // 应用属性加成
                    applyGoddessBoost(player, goddess, true);

                    // Gen4/Gen5：授予创造飞行（Gen5 飞行速度 ×1.5）
                    GoddessFlightHandler.grantFlight(player, cap.getDiskGen());

                    // 调用女神的特殊效果（预留）
                    goddess.onTransformStart(player);

                    // 更新缓存（死亡→重生恢复用）
                    Neptunia.updatePlayerCache(player.getUUID(), cap.getAbility(), type, cap.getDiskGen(), startTime);

                    Neptunia.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new GoddessAbilitySyncPacket(cap.getAbility(), startTime, type, cap.getDiskGen())
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
                    // 移除变身飞行（创造/旁观模式玩家不受影响）
                    GoddessFlightHandler.revokeFlight(player);
                    System.out.println("✅ [服务端] 玩家 " + player.getName().getString() + " 解除变身");

                    // 更新缓存（死亡→重生恢复用）
                    Neptunia.updatePlayerCache(player.getUUID(), cap.getAbility(), type, cap.getDiskGen(), 0);

                    Neptunia.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new GoddessAbilitySyncPacket(cap.getAbility(), 0, type, cap.getDiskGen())
                    );
                    player.sendSystemMessage(Component.literal("解除变身！"));
                }
            });
        });
        context.setPacketHandled(true);
    }

    /**
     * 应用或移除女神的属性加成。
     * <p>
     * 实际倍率按磁盘世代缩放：最终倍率 = 1 + (注册值 − 1) × 世代系数
     * （注册值为 Gen5 满额数值，见 {@link GoddessDiskGen}）。
     * <p>
     * 注意：应用前会先无条件清除玩家身上所有女神加成，
     * 防止更换女神/异常流程导致多个女神的加成叠加。
     */
    public static void applyGoddessBoost(ServerPlayer player, Goddess goddess, boolean apply) {
        if (goddess == null) return;

        // 先清除所有女神加成（自愈残留的叠加状态）
        removeAllGoddessBoosts(player);
        if (!apply) return;

        // 读取玩家所用磁盘世代（默认 Gen5 = 满额，兼容旧数据与指令赋予）
        float factor = player.getCapability(GoddessCapabilityProvider.GODDESS_CAPABILITY)
                .map(cap -> cap.getDiskGen().getBoostFactor())
                .orElse(GoddessDiskGen.GEN5.getBoostFactor());

        UUID boostUUID = goddess.getBoostUUID();
        for (Map.Entry<Attribute, Double> entry : goddess.getAttributeMultipliers().entrySet()) {
            Attribute attr = entry.getKey();
            double base = entry.getValue();
            double multiplier = 1.0 + (base - 1.0) * factor;
            AttributeInstance instance = player.getAttribute(attr);
            if (instance == null) continue;

            AttributeModifier modifier = new AttributeModifier(boostUUID,
                    "goddess_boost_" + goddess.getId().name(),
                    multiplier - 1.0,
                    AttributeModifier.Operation.MULTIPLY_BASE);
            instance.addPermanentModifier(modifier);
        }
    }

    /**
     * 移除玩家身上所有女神的加成修饰器（无论来自哪位女神）。
     * 先收集所有女神涉及的属性（去重），再按修饰器名称前缀匹配清除。
     */
    private static void removeAllGoddessBoosts(ServerPlayer player) {
        java.util.Set<Attribute> attributes = new java.util.HashSet<>();
        for (Goddess registered : GoddessRegistry.getInstance().getAllGoddesses()) {
            attributes.addAll(registered.getAttributeMultipliers().keySet());
        }
        for (Attribute attr : attributes) {
            AttributeInstance instance = player.getAttribute(attr);
            if (instance == null) continue;
            for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
                if (modifier.getName().startsWith("goddess_boost_")) {
                    instance.removeModifier(modifier);
                }
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