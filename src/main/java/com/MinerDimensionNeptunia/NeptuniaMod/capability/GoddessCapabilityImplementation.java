package com.MinerDimensionNeptunia.NeptuniaMod.capability;

import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GoddessCapabilityImplementation implements GoddessCapability, ICapabilitySerializable<CompoundTag> {
    private boolean ability = false;
    private long transformStartTime = 0;
    private GoddessType goddessType = GoddessType.NONE;

    @Override
    public boolean getAbility() {
        return ability;
    }

    @Override
    public void setAbility(boolean value) {
        this.ability = value;
    }

    @Override
    public long getTransformStartTime() {
        return transformStartTime;
    }

    @Override
    public void setTransformStartTime(long time) {
        this.transformStartTime = time;
    }

    @Override
    public GoddessType getGoddessType() {
        return goddessType;
    }

    @Override
    public void setGoddessType(GoddessType type) {
        this.goddessType = type;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("goddess_ability", ability);
        tag.putLong("transform_start_time", transformStartTime);
        tag.putString("goddess_type", goddessType.name());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        ability = nbt.getBoolean("goddess_ability");
        transformStartTime = nbt.getLong("transform_start_time");
        goddessType = GoddessType.fromName(nbt.getString("goddess_type"));
    }

    private final LazyOptional<GoddessCapability> holder = LazyOptional.of(() -> this);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == GoddessCapabilityProvider.GODDESS_CAPABILITY) {
            return holder.cast();
        }
        return LazyOptional.empty();
    }
}