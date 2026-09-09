package com.MinerDimensionNeptunia.NeptuniaMod.capability;

import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;

public interface GoddessCapability {
    boolean getAbility();
    void setAbility(boolean value);

    long getTransformStartTime();
    void setTransformStartTime(long time);

    GoddessType getGoddessType();
    void setGoddessType(GoddessType type);

    default boolean isTransformed() {
        return getTransformStartTime() > 0;
    }
}