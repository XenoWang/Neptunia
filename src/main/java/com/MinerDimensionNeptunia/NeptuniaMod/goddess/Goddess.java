package com.MinerDimensionNeptunia.NeptuniaMod.goddess;

import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Goddess {
    private final GoddessType id;
    private final String displayName;
    private final Map<Attribute, Double> attributeMultipliers = new HashMap<>();
    private final UUID boostUUID;
    private String colorKey; // 可选：用于指定颜色配置中的键，默认使用 id

    public Goddess(GoddessType id, String displayName) {
        this.id = id;
        this.displayName = displayName;
        this.boostUUID = UUID.randomUUID();
        this.colorKey = id.name().toLowerCase();
    }

    public Goddess setColorKey(String colorKey) {
        this.colorKey = colorKey;
        return this;
    }

    public String getColorKey() {
        return colorKey;
    }

    public Goddess addAttributeBoost(Attribute attr, double multiplier) {
        attributeMultipliers.put(attr, multiplier);
        return this;
    }

    public GoddessType getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Map<Attribute, Double> getAttributeMultipliers() { return attributeMultipliers; }
    public UUID getBoostUUID() { return boostUUID; }

    public void onTransformStart(ServerPlayer player) {}
    public void onTransformEnd(ServerPlayer player) {}
}