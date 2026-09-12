package com.MinerDimensionNeptunia.NeptuniaMod.item.weapon;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 女神武器的伪耐久恢复逻辑：
 * <ul>
 *     <li>随时间恢复：每秒 +1 点耐久</li>
 *     <li>击杀任意生物：额外 +20 点耐久</li>
 * </ul>
 * 耐久永远不会扣到 0（见 {@link GoddessWeaponItem#damageItem}），因此武器不会损坏。
 * 恢复作用于玩家背包内（含副手）的所有女神武器。
 */
@Mod.EventBusSubscriber(modid = Neptunia.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GoddessWeaponEvents {
    /** 恢复间隔：20 tick = 1 秒 */
    private static final int REGEN_INTERVAL_TICKS = 20;
    /** 每秒恢复的耐久 */
    private static final int REGEN_PER_SECOND = 10;
    /** 击杀生物额外恢复的耐久 */
    private static final int REGEN_PER_KILL = 200;

    /** 随时间恢复：每秒一次 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % REGEN_INTERVAL_TICKS != 0) {
            return;
        }
        restoreDurability(player, REGEN_PER_SECOND);
    }

    /** 击杀任意生物：额外恢复 */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        // getEntity() 会返回弹射物的发射者，因此远程击杀同样计入
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        restoreDurability(player, REGEN_PER_KILL);
    }

    /** 恢复玩家背包内所有女神武器的耐久 */
    private static void restoreDurability(ServerPlayer player, int amount) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof GoddessWeaponItem && stack.isDamaged()) {
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - amount));
            }
        }
    }
}
