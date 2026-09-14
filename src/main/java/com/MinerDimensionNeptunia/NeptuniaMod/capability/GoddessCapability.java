package com.MinerDimensionNeptunia.NeptuniaMod.capability;

import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessDiskGen;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;

public interface GoddessCapability {
    boolean getAbility();
    void setAbility(boolean value);

    long getTransformStartTime();
    void setTransformStartTime(long time);

    GoddessType getGoddessType();
    void setGoddessType(GoddessType type);

    /** 使用的女神磁盘世代（决定属性强弱） */
    GoddessDiskGen getDiskGen();
    void setDiskGen(GoddessDiskGen gen);

    default boolean isTransformed() {
        return getTransformStartTime() > 0;
    }
}