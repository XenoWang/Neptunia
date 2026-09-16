package com.MinerDimensionNeptunia.NeptuniaMod.item.weapon;

import com.MinerDimensionNeptunia.NeptuniaMod.entity.GoddessProjectile;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** 枪械、法杖共用的服务端发射逻辑，沿用女神武器的伪耐久与恢复机制。 */
public abstract class GoddessRangedWeaponItem extends GoddessWeaponItem {
    private static final int SHOT_DURABILITY_COST = 10;
    private final float baseShotsPerSecond;

    protected GoddessRangedWeaponItem(GoddessType type, float speed, Properties properties) {
        // 远程武器仅保留 1 点基础近战伤害，弹射物伤害单独计算。
        super(type, Tiers.DIAMOND, -3, speed, properties);
        this.baseShotsPerSecond = 4.0F + speed;
    }

    protected abstract GoddessProjectile createProjectile(Level level, Player player);
    protected abstract float projectileSpeed();
    protected abstract SoundEvent firingSound();
    protected abstract float projectileDamage();

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.miner_dimension_neptunia.ranged.use").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.miner_dimension_neptunia.ranged.damage",
                ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(projectileDamage())).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.miner_dimension_neptunia.ranged.speed",
                ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(baseShotsPerSecond)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.miner_dimension_neptunia.ranged.cost",
                SHOT_DURABILITY_COST).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.miner_dimension_neptunia.ranged.scaling").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // 属性来自主手，避免副手借用其它武器的伤害和攻速。
        if (hand != InteractionHand.MAIN_HAND || player.getCooldowns().isOnCooldown(this)
                || stack.getMaxDamage() - 1 - stack.getDamageValue() < SHOT_DURABILITY_COST) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            GoddessProjectile projectile = createProjectile(level, player);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0,
                    projectileSpeed(), 0.5F);
            if (!level.addFreshEntity(projectile)) return InteractionResultHolder.fail(stack);
            stack.hurtAndBreak(SHOT_DURABILITY_COST, player, entity -> entity.broadcastBreakEvent(hand));
            int cooldown = Math.max(4, (int) Math.ceil(20.0 /
                    Math.max(0.1, player.getAttributeValue(Attributes.ATTACK_SPEED))));
            // 所有枪械、法杖共用冷却，切换类型或阶级不能绕过射速。
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
