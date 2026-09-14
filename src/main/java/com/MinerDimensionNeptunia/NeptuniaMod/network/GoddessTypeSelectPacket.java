package com.MinerDimensionNeptunia.NeptuniaMod.network;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import com.MinerDimensionNeptunia.NeptuniaMod.capability.GoddessCapabilityProvider;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.Goddess;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.GoddessRegistry;
import com.MinerDimensionNeptunia.NeptuniaMod.item.usable.GoddessDiskItem;
import com.MinerDimensionNeptunia.NeptuniaMod.item.usable.GoddessFlightHandler;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessDiskGen;
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
    private final GoddessDiskGen gen;

    public GoddessTypeSelectPacket(GoddessType type, GoddessDiskGen gen) {
        this.selectedType = type;
        this.gen = gen;
    }

    public static void encode(GoddessTypeSelectPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.selectedType.name());
        buf.writeUtf(msg.gen.name());
    }

    public static GoddessTypeSelectPacket decode(FriendlyByteBuf buf) {
        return new GoddessTypeSelectPacket(
                GoddessType.fromName(buf.readUtf()),
                GoddessDiskGen.fromName(buf.readUtf()));
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
                boolean wasSelected = cap.getAbility();

                // 若正在变身，先移除旧女神的属性加成与飞行（先清后写，绝不叠加），
                // 并结束本次变身（重新选择后需再按变身键）
                if (cap.getTransformStartTime() > 0) {
                    Goddess oldGoddess = GoddessRegistry.getInstance().getGoddess(cap.getGoddessType());
                    if (oldGoddess != null) {
                        TransformRequestPacket.applyGoddessBoost(player, oldGoddess, false);
                    }
                    cap.setTransformStartTime(0);
                    GoddessFlightHandler.revokeFlight(player);
                }

                // 覆盖为新选择的女神与世代
                cap.setAbility(true);
                cap.setGoddessType(msg.selectedType);
                cap.setDiskGen(msg.gen);

                // 消耗 1 张本次使用的世代磁盘
                consumeGoddessDisk(player, msg.gen);

                // 给予该女神的默认武器（已拥有则不重复给）
                Goddess goddess = GoddessRegistry.getInstance().getGoddess(msg.selectedType);
                if (goddess != null) {
                    giveStarterWeapon(player, goddess);
                }

                // 更新缓存（死亡→重生恢复用）
                Neptunia.updatePlayerCache(player.getUUID(), cap.getAbility(), msg.selectedType, msg.gen,
                        cap.getTransformStartTime());

                // 同步给客户端（含磁盘世代，客户端倒计时时长随世代变化）
                Neptunia.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new GoddessAbilitySyncPacket(
                                cap.getAbility(),
                                cap.getTransformStartTime(),
                                msg.selectedType,
                                msg.gen
                        )
                );

                player.sendSystemMessage(Component.literal(wasSelected
                        ? "你重新选择了女神：" + msg.selectedType.name() + "（世代 " + msg.gen.name() + "）"
                        : "你获得了女神化的能力，并选择了 " + msg.selectedType.name()));
            });
        });
        context.setPacketHandled(true);
    }

    /** 消耗 1 张指定世代的女神磁盘（选择女神后扣除） */
    private static void consumeGoddessDisk(ServerPlayer player, GoddessDiskGen gen) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof GoddessDiskItem disk && disk.getGen() == gen) {
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
