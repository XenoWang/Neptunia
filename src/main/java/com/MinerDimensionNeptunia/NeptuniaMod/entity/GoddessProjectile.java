package com.MinerDimensionNeptunia.NeptuniaMod.entity;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

/** 有限寿命、无重力的枪弹与冰锥；物品同步数据决定客户端外观。 */
public class GoddessProjectile extends ThrowableItemProjectile {
    private float damage;

    public GoddessProjectile(EntityType<? extends GoddessProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public GoddessProjectile(Level level, Player owner, boolean ice, float baseDamage) {
        this(Neptunia.GODDESS_PROJECTILE.get(), level);
        setOwner(owner);
        setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        setItem(new ItemStack(ice ? Neptunia.ICE_CONE.get() : Neptunia.BULLET.get()));
        // 在副本上加入弹射物基础伤害，再按原版顺序应用属性修饰器。
        // 力量等固定加值只加一次，不能随武器阶级成倍放大，也不修改玩家属性。
        AttributeInstance attack = new AttributeInstance(Attributes.ATTACK_DAMAGE, attribute -> {});
        attack.replaceFrom(owner.getAttribute(Attributes.ATTACK_DAMAGE));
        attack.setBaseValue(attack.getBaseValue() + baseDamage - 1.0);
        damage = (float) attack.getValue();
    }

    private boolean isIce() { return getItem().is(Neptunia.ICE_CONE.get()); }

    @Override
    protected Item getDefaultItem() { return Neptunia.BULLET.get(); }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && isIce()) {
            level().addParticle(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 0, 0, 0);
        }
        if (!level().isClientSide && tickCount >= 40) discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) return;
        if (result.getEntity() instanceof Player target && getOwner() instanceof Player owner
                && !owner.canHarmPlayer(target)) return;
        boolean hit = result.getEntity().hurt(isIce()
                ? damageSources().indirectMagic(this, getOwner())
                : damageSources().thrown(this, getOwner()), damage);
        if (hit && isIce() && result.getEntity() instanceof LivingEntity target) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0), getOwner());
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(isIce() ? ParticleTypes.SNOWFLAKE : ParticleTypes.CRIT,
                    getX(), getY(), getZ(), 6, 0.12, 0.12, 0.12, 0.02);
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putInt("Age", tickCount);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        tickCount = tag.getInt("Age");
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
