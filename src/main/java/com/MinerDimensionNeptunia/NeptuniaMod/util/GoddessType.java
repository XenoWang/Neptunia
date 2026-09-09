package com.MinerDimensionNeptunia.NeptuniaMod.util;

public enum GoddessType {
    NONE,
    PROTOTYPE;

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