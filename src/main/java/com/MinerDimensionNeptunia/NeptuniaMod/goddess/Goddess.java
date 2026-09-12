package com.MinerDimensionNeptunia.NeptuniaMod.goddess;

import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

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

    // 开发者专属：仅开发者名单内的玩家在选择界面可见
    private boolean devOnly = false;

    // 默认武器：使用女神磁盘选定该女神后自动获得（用 Supplier 延迟取值，避免注册顺序问题）
    private Supplier<? extends Item> starterWeapon;

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

    /** 标记为开发者专属女神：选择界面仅对开发者名单内的玩家显示 */
    public Goddess setDevOnly(boolean devOnly) {
        this.devOnly = devOnly;
        return this;
    }

    /** 设置默认武器：玩家用女神磁盘选定该女神后会自动获得（传注册项，延迟取值） */
    public Goddess setStarterWeapon(Supplier<? extends Item> weapon) {
        this.starterWeapon = weapon;
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
    public boolean isDevOnly() { return devOnly; }

    /** 获取默认武器，未设置则返回 null（仅在物品注册完成后调用） */
    public Item getStarterWeapon() { return starterWeapon == null ? null : starterWeapon.get(); }
    public Map<Attribute, Double> getAttributeMultipliers() { return attributeMultipliers; }
    public UUID getBoostUUID() { return boostUUID; }

    public void onTransformStart(ServerPlayer player) {}
    public void onTransformEnd(ServerPlayer player) {}
}