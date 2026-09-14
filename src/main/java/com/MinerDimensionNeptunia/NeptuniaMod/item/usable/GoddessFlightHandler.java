package com.MinerDimensionNeptunia.NeptuniaMod.item.usable;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import com.MinerDimensionNeptunia.NeptuniaMod.capability.GoddessCapabilityProvider;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessDiskGen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Gen4 / Gen5 磁盘的变身飞行能力（变身期间 = transformStartTime &gt; 0）。
 * <ul>
 *     <li>授予：变身开始（{@link net.minecraftforge.fml.common.Mod.EventBusSubscriber} 由
 *         {@code TransformRequestPacket} 调用 {@link #grantFlight}）。</li>
 *     <li>Tick 保活：切换维度会重置 abilities，本类在服务端每 tick 校验，
 *         变身着陆 / 换维度后自动恢复飞行。</li>
 *     <li>清理：变身结束 / 死亡时移除飞行；创造与旁观模式玩家不受影响。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Neptunia.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GoddessFlightHandler {
    /** 原版默认飞行速度 */
    public static final float VANILLA_FLY_SPEED = 0.05F;

    /** 授予创造飞行（Gen4/Gen5）；重复调用幂等，只在状态变化时同步 */
    public static void grantFlight(ServerPlayer player, GoddessDiskGen gen) {
        if (!gen.grantsFlight()) {
            return;
        }
        Abilities abilities = player.getAbilities();
        boolean changed = false;
        if (!abilities.mayfly) {
            abilities.mayfly = true;
            changed = true;
        }
        if (!abilities.flying) {
            abilities.flying = true;
            changed = true;
        }
        float speed = VANILLA_FLY_SPEED * gen.getFlightSpeedMultiplier();
        if (Math.abs(abilities.getFlyingSpeed() - speed) > 0.0001F) {
            abilities.setFlyingSpeed(speed);
            changed = true;
        }
        if (changed) {
            player.onUpdateAbilities();
        }
    }

    /** 移除飞行（变身结束 / 死亡时调用）。创造与旁观玩家本就有飞行，只恢复默认速度 */
    public static void revokeFlight(ServerPlayer player) {
        Abilities abilities = player.getAbilities();
        boolean changed = false;
        if (!player.isCreative() && !player.isSpectator()) {
            if (abilities.mayfly) {
                abilities.mayfly = false;
                changed = true;
            }
            if (abilities.flying) {
                abilities.flying = false;
                changed = true;
            }
        }
        if (Math.abs(abilities.getFlyingSpeed() - VANILLA_FLY_SPEED) > 0.0001F) {
            abilities.setFlyingSpeed(VANILLA_FLY_SPEED);
            changed = true;
        }
        if (changed) {
            player.onUpdateAbilities();
        }
    }

    /** Tick 保活：变身期间保证飞行可用（维度切换会重置 abilities，此处自动恢复） */
    @SubscribeEvent
    public static void onServerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        player.getCapability(GoddessCapabilityProvider.GODDESS_CAPABILITY).ifPresent(cap -> {
            if (cap.isTransformed()) {
                grantFlight(player, cap.getDiskGen());
            }
        });
    }

    /** 死亡清理：立即移除飞行（非创造玩家；重生后由 respawn 流程统一处理） */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            revokeFlight(player);
        }
    }
}
