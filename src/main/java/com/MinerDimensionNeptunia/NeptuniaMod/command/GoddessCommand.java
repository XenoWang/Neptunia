package com.MinerDimensionNeptunia.NeptuniaMod.command;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import com.MinerDimensionNeptunia.NeptuniaMod.capability.GoddessCapabilityProvider;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.Goddess;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.GoddessRegistry;
import com.MinerDimensionNeptunia.NeptuniaMod.item.usable.GoddessFlightHandler;
import com.MinerDimensionNeptunia.NeptuniaMod.network.GoddessAbilitySyncPacket;
import com.MinerDimensionNeptunia.NeptuniaMod.network.TransformRequestPacket;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessDiskGen;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class GoddessCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("neptunia")
                        .requires(source -> source.hasPermission(2)) // OP 权限
                        .then(Commands.literal("goddess")
                                // ----- clear 子命令 -----
                                .then(Commands.literal("clear")
                                        .executes(ctx -> clearGoddess(ctx, ctx.getSource().getPlayerOrException()))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> clearGoddess(ctx,
                                                        EntityArgument.getPlayer(ctx, "player")))))
                                // ----- add 子命令 -----
                                .then(Commands.literal("add")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("type", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> {
                                                            // 提供女神类型自动补全
                                                            for (GoddessType type : GoddessType.values()) {
                                                                if (type != GoddessType.NONE) {
                                                                    builder.suggest(type.name().toLowerCase());
                                                                }
                                                            }
                                                            return builder.buildFuture();
                                                        })
                                                        // 不带世代参数：默认 Gen5（满额）
                                                        .executes(ctx -> addGoddess(ctx,
                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                StringArgumentType.getString(ctx, "type"),
                                                                GoddessDiskGen.GEN1))
                                                        // 可选世代参数 1~5
                                                        .then(Commands.argument("gen", IntegerArgumentType.integer(1, 5))
                                                                .suggests((ctx, builder) -> {
                                                                    for (int i = 1; i <= 5; i++) {
                                                                        builder.suggest(String.valueOf(i));
                                                                    }
                                                                    return builder.buildFuture();
                                                                })
                                                                .executes(ctx -> addGoddess(ctx,
                                                                        EntityArgument.getPlayer(ctx, "player"),
                                                                        StringArgumentType.getString(ctx, "type"),
                                                                        GoddessDiskGen.values()[IntegerArgumentType
                                                                                .getInteger(ctx, "gen") - 1]))))))));
    }

    // 清除女神化能力（可指定玩家）
    private static int clearGoddess(CommandContext<CommandSourceStack> ctx, ServerPlayer target)
            throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        target.getCapability(GoddessCapabilityProvider.GODDESS_CAPABILITY).ifPresent(cap -> {
            // 如果处于变身状态，移除加成
            if (cap.getTransformStartTime() > 0) {
                var goddess = GoddessRegistry.getInstance().getGoddess(cap.getGoddessType());
                if (goddess != null) {
                    TransformRequestPacket.applyGoddessBoost(target, goddess, false);
                }
            }
            // 重置能力
            cap.setAbility(false);
            cap.setTransformStartTime(0);
            cap.setGoddessType(GoddessType.NONE);
            // 移除变身飞行（创造/旁观模式玩家不受影响）
            GoddessFlightHandler.revokeFlight(target);
            // 更新缓存（死亡→重生恢复用；清除状态会移除缓存）
            Neptunia.updatePlayerCache(target.getUUID(), false, GoddessType.NONE, cap.getDiskGen(), 0);
            // 同步给客户端
            Neptunia.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> target),
                    new GoddessAbilitySyncPacket(false, 0, GoddessType.NONE, cap.getDiskGen()));
            source.sendSuccess(() -> Component.literal("已清除 " + target.getName().getString() + " 的女神化能力！"), true);
        });
        return 1;
    }

    // 添加女神化能力（可指定玩家、女神类型与磁盘世代 1~5）
    private static int addGoddess(CommandContext<CommandSourceStack> ctx, ServerPlayer target, String typeName,
                                  GoddessDiskGen gen) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        // 解析女神类型
        GoddessType type = GoddessType.fromName(typeName);
        if (type == GoddessType.NONE || !GoddessRegistry.getInstance().isRegistered(type)) {
            source.sendFailure(Component.literal("无效的女神类型！可用类型: " + String.join(", ",
                    GoddessRegistry.getInstance().getAllGoddesses().stream()
                            .map(g -> g.getId().name().toLowerCase())
                            .toArray(String[]::new))));
            return 0;
        }

        target.getCapability(GoddessCapabilityProvider.GODDESS_CAPABILITY).ifPresent(cap -> {
            // 如果正在变身，先移除旧女神的属性加成，避免新旧加成叠加
            if (cap.getTransformStartTime() > 0) {
                Goddess oldGoddess = GoddessRegistry.getInstance().getGoddess(cap.getGoddessType());
                if (oldGoddess != null) {
                    TransformRequestPacket.applyGoddessBoost(target, oldGoddess, false);
                }
            }
            // 如果已有能力，覆盖（赋予新类型与世代）
            cap.setAbility(true);
            cap.setGoddessType(type);
            cap.setDiskGen(gen);
            cap.setTransformStartTime(0); // 重置变身时间（如果正在变身会立即解除）
            // 正在变身时被覆盖：移除变身飞行（创造/旁观模式玩家不受影响）
            GoddessFlightHandler.revokeFlight(target);
            // 更新缓存（死亡→重生恢复用）
            Neptunia.updatePlayerCache(target.getUUID(), true, type, gen, 0);
            // 同步给客户端（含世代，客户端倒计时时长随世代变化）
            Neptunia.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> target),
                    new GoddessAbilitySyncPacket(true, 0, type, gen));
            source.sendSuccess(() -> Component.literal("已为 " + target.getName().getString() +
                    " 添加女神化能力，类型: " + type.name() + "，世代: " + gen.name()), true);
        });
        return 1;
    }
}