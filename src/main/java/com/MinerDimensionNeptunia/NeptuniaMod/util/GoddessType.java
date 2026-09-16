package com.MinerDimensionNeptunia.NeptuniaMod.util;

public enum GoddessType {
    NONE,
    PROTOTYPE,
    PURPLE_HEART,
    BLACK_HEART,
    WHITE_HEART,
    GREEN_HEART,
    PURPLE_SISTER,
    BLACK_SISTER,
    WHITE_SISTER_ROM,
    WHITE_SISTER_RAM;

    public boolean isNone() {
        return this == NONE;
    }

    public static GoddessType fromName(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
