package com.MinerDimensionNeptunia.NeptuniaMod.item.weapon;

import com.MinerDimensionNeptunia.NeptuniaMod.entity.GoddessProjectile;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;

/** 枪械、法杖共用的服务端发射逻辑，沿用女神武器的伪耐久与恢复机制。 */
public abstract class GoddessRangedWeaponItem extends GoddessWeaponItem {
    protected GoddessRangedWeaponItem(GoddessType type, float speed, Properties properties) {
        // 远程武器仅保留 1 点基础近战伤害，弹射物伤害单独计算。
        super(type, Tiers.DIAMOND, -3, speed, properties);
    }

    protected abstract GoddessProjectile createProjectile(Level level, Player player);
    protected abstract float projectileSpeed();
    protected abstract SoundEvent firingSound();

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // 属性来自主手，避免副手借用其它武器的伤害和攻速。
        if (hand != InteractionHand.MAIN_HAND || player.getCooldowns().isOnCooldown(this)
                || stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            GoddessProjectile projectile = createProjectile(level, player);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0,
                    projectileSpeed(), 0.5F);
            if (!level.addFreshEntity(projectile)) return InteractionResultHolder.fail(stack);
            stack.hurtAndBreak(10, player, entity -> entity.broadcastBreakEvent(hand));
            int cooldown = Math.max(4, (int) Math.ceil(20.0 /
                    Math.max(0.1, player.getAttributeValue(Attributes.ATTACK_SPEED))));
            // 同类所有阶级共用冷却，切换阶级不能绕过射速。
            for (var weapon : com.MinerDimensionNeptunia.NeptuniaMod.Neptunia.ALL_GODDESS_WEAPONS) {
                if (weapon.get() instanceof GoddessRangedWeaponItem) {
                    player.getCooldowns().addCooldown(weapon.get(), cooldown);
                }
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), firingSound(),
                    SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
