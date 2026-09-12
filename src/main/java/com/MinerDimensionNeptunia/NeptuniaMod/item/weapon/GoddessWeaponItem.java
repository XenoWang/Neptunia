package com.MinerDimensionNeptunia.NeptuniaMod.item.weapon;

import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

import java.util.function.Consumer;

/**
 * 女神专属武器。
 * <p>
 * 目前统一使用剑类行为（钻石强度基准），锤类等差异化机制后续按需扩展。
 * 记录所属女神类型，便于后续升级配方与强化逻辑识别。
 * <p>
 * <b>伪耐久条</b>：耐久永远不会扣到最后 {@link #RESERVED_DURABILITY} 点，
 * 因此武器不会损坏；耐久通过每秒 +1、击杀生物 +20 恢复（见 {@link GoddessWeaponEvents}）。
 */
public class GoddessWeaponItem extends SwordItem {
    /** 武器耐久上限（与原版钻石武器一致） */
    public static final int DURABILITY = 1561;
    /** 伪耐久：始终保留的最后 1 点耐久，保证武器永不损坏 */
    private static final int RESERVED_DURABILITY = 1;

    private final GoddessType goddessType;

    public GoddessWeaponItem(GoddessType goddessType, Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
        this.goddessType = goddessType;
    }

    public GoddessType getGoddessType() {
        return this.goddessType;
    }

    /**
     * 伪耐久条的核心：限制单次扣减量，使耐久最多扣到「上限 - 1」，
     * 武器因此永远不会损坏，只会显示为接近耗尽的状态。
     *
     * @return 实际要扣减的耐久值（返回 0 表示本次不扣耐久）
     */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
        int allowed = Math.max(0, stack.getMaxDamage() - RESERVED_DURABILITY - stack.getDamageValue());
        return Math.min(amount, allowed);
    }
}
