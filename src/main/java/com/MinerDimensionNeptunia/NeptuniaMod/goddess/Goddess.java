package com.MinerDimensionNeptunia.NeptuniaMod.goddess;

import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.resources.ResourceLocation;
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

    // 选择界面卡牌展示用：介绍文字 + 立绘纹理（及纹理实际像素尺寸）
    private String description = "";
    private ResourceLocation artTexture;
    private int artTextureWidth = 64;
    private int artTextureHeight = 64;

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

    /** 设置选择界面卡牌上显示的女神介绍 */
    public Goddess setDescription(String description) {
        this.description = description;
        return this;
    }

    /** 设置选择界面卡牌上显示的立绘纹理（需同时给出纹理图片的实际像素尺寸） */
    public Goddess setArtTexture(ResourceLocation texture, int textureWidth, int textureHeight) {
        this.artTexture = texture;
        this.artTextureWidth = textureWidth;
        this.artTextureHeight = textureHeight;
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
    public String getDescription() { return description; }
    public ResourceLocation getArtTexture() { return artTexture; }
    public int getArtTextureWidth() { return artTextureWidth; }
    public int getArtTextureHeight() { return artTextureHeight; }
    public Map<Attribute, Double> getAttributeMultipliers() { return attributeMultipliers; }
    public UUID getBoostUUID() { return boostUUID; }

    public void onTransformStart(ServerPlayer player) {}
    public void onTransformEnd(ServerPlayer player) {}
}