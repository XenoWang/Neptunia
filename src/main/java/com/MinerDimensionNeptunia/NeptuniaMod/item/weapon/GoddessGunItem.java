package com.MinerDimensionNeptunia.NeptuniaMod.item.weapon;

import com.MinerDimensionNeptunia.NeptuniaMod.entity.GoddessProjectile;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Uni 的步枪：直线高速枪弹。 */
public class GoddessGunItem extends GoddessRangedWeaponItem {
    private final float damage;

    public GoddessGunItem(GoddessType type, float damage, float speed, Properties properties) {
        super(type, speed, properties);
        this.damage = damage;
    }

    @Override
    protected GoddessProjectile createProjectile(Level level, Player player) {
        return new GoddessProjectile(level, player, false, damage);
    }

    @Override
    protected float projectileSpeed() { return 3.5F; }

    @Override
    protected SoundEvent firingSound() { return SoundEvents.CROSSBOW_SHOOT; }
}
