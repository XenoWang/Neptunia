package com.MinerDimensionNeptunia.NeptuniaMod.item.weapon;

import com.MinerDimensionNeptunia.NeptuniaMod.entity.GoddessProjectile;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Rom / Ram 的基础冰魔法：远程释放冰锥。 */
public class GoddessStaffItem extends GoddessRangedWeaponItem {
    private final float damage;

    public GoddessStaffItem(GoddessType type, float damage, float speed, Properties properties) {
        super(type, speed, properties);
        this.damage = damage;
    }

    @Override
    protected GoddessProjectile createProjectile(Level level, Player player) {
        return new GoddessProjectile(level, player, true, damage);
    }

    @Override
    protected float projectileSpeed() { return 1.7F; }

    @Override
    protected SoundEvent firingSound() { return SoundEvents.SNOW_GOLEM_SHOOT; }
}
