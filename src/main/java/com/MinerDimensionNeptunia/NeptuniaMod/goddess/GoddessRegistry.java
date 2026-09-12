package com.MinerDimensionNeptunia.NeptuniaMod.goddess;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * 女神注册中心（单例），管理所有内置女神，支持后续动态注册自定义女神
 */
public class GoddessRegistry {
    private static final GoddessRegistry INSTANCE = new GoddessRegistry();
    private final Map<GoddessType, Goddess> goddesses = new EnumMap<>(GoddessType.class);

    private GoddessRegistry() {
        registerDefaultGoddesses();
    }

    public static GoddessRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * 注册所有内置女神
     */
    private void registerDefaultGoddesses() {
        // Prototype（平衡型，开发者专属）
        Goddess prototype = new Goddess(GoddessType.PROTOTYPE, "原始之初")
                .setDevOnly(true) // 普通玩家在选择界面不可见
                .setStarterWeapon(Neptunia.PROTOTYPE_RAPIER) // 默认武器：刺剑
                .setDescription("均衡型女神，能力全面均衡，测试专用。")
                .setArtTexture(new ResourceLocation(Neptunia.MODID, "textures/gui/goddess/prototype.png"), 512, 512)
                .addAttributeBoost(Attributes.ATTACK_DAMAGE, 3.0)
                .addAttributeBoost(Attributes.ATTACK_SPEED, 3.0)
                .addAttributeBoost(Attributes.MOVEMENT_SPEED, 3.0)
                .addAttributeBoost(Attributes.ARMOR, 3.0)
                .addAttributeBoost(Attributes.ARMOR_TOUGHNESS, 3.0);
        registerGoddess(prototype);

        // ============================================================
        // 四位 超次元游戏海王星 CPU 女神
        // ============================================================

        // 绀紫之心（涅普顿，均衡型）：五项属性各提升 30%（0.3 × 5 = 1.5）
        Goddess purpleHeart = new Goddess(GoddessType.PURPLE_HEART, "绀紫之心")
                .setStarterWeapon(Neptunia.PURPLE_HEART_KATANA) // 默认武器：太刀
                .setDescription("均衡型。五项能力全面提升，攻守兼备。")
                .setArtTexture(new ResourceLocation(Neptunia.MODID, "textures/gui/goddess/purple_heart.png"), 512, 512)
                .addAttributeBoost(Attributes.ATTACK_SPEED, 1.25) // DPS
                .addAttributeBoost(Attributes.ATTACK_DAMAGE, 1.35) // 1.25×1.35 ≈ 1.69
                .addAttributeBoost(Attributes.ARMOR, 1.3) // 生存
                .addAttributeBoost(Attributes.ARMOR_TOUGHNESS, 1.2) // 1.3×1.2 ≈ 1.56
                .addAttributeBoost(Attributes.MOVEMENT_SPEED, 1.3); // 移速
        registerGoddess(purpleHeart);

        // 圣黑之心（诺瓦露，速攻型）：0.5 + 0.5 + 0.1 + 0.2 + 0.2 = 1.5
        Goddess blackHeart = new Goddess(GoddessType.BLACK_HEART, "圣黑之心")
                .setStarterWeapon(Neptunia.BLACK_HEART_LONGSWORD) // 默认武器：长剑
                .setDescription("速攻型。攻势凌厉，防御与韧性同样不俗。")
                .setArtTexture(new ResourceLocation(Neptunia.MODID, "textures/gui/goddess/black_heart.png"), 512, 512)
                .addAttributeBoost(Attributes.ATTACK_SPEED, 1.4) // DPS
                .addAttributeBoost(Attributes.ATTACK_DAMAGE, 1.45) // 1.4×1.45 ≈ 2.03
                .addAttributeBoost(Attributes.ARMOR, 1.15) // 生存
                .addAttributeBoost(Attributes.ARMOR_TOUGHNESS, 1.15) // 1.15×1.15 ≈ 1.32
                .addAttributeBoost(Attributes.MOVEMENT_SPEED, 1.2); // 移速
        registerGoddess(blackHeart);

        // 群白之心（布兰，重装型）：-0.3 + 1.0 + 0.1 + 0.35 + 0.35 = 1.5
        Goddess whiteHeart = new Goddess(GoddessType.WHITE_HEART, "群白之心")
                .setStarterWeapon(Neptunia.WHITE_HEART_HAMMER) // 默认武器：战锤
                .setDescription("重装型。一击必杀的破坏力与坚固防御，挥击稍显迟缓。")
                .setArtTexture(new ResourceLocation(Neptunia.MODID, "textures/gui/goddess/white_heart.png"), 512, 512)
                .addAttributeBoost(Attributes.ATTACK_SPEED, 0.75) // 降低攻速
                .addAttributeBoost(Attributes.ATTACK_DAMAGE, 2.5) // 0.75×2.5 ≈ 1.875
                .addAttributeBoost(Attributes.ARMOR, 1.45) // 生存
                .addAttributeBoost(Attributes.ARMOR_TOUGHNESS, 1.3) // 1.45×1.3 ≈ 1.885
                .addAttributeBoost(Attributes.MOVEMENT_SPEED, 1.05); // 移速略升
        registerGoddess(whiteHeart);

        // 翡绿之心（贝露，速攻型）：0.5 + 0.5 + 0.3 + 0.1 + 0.1 = 1.5
        Goddess greenHeart = new Goddess(GoddessType.GREEN_HEART, "翡绿之心")
                .setStarterWeapon(Neptunia.GREEN_HEART_SPEAR) // 默认武器：长枪
                .setDescription("速攻型。身手迅捷攻势凶猛，护甲相对薄弱。")
                .setArtTexture(new ResourceLocation(Neptunia.MODID, "textures/gui/goddess/green_heart.png"), 512, 512)
                .addAttributeBoost(Attributes.ATTACK_SPEED, 1.45) // DPS
                .addAttributeBoost(Attributes.ATTACK_DAMAGE, 1.35) // 1.45×1.35 ≈ 1.96
                .addAttributeBoost(Attributes.ARMOR, 1.1) // 生存
                .addAttributeBoost(Attributes.ARMOR_TOUGHNESS, 1.1) // 1.1×1.1 ≈ 1.21
                .addAttributeBoost(Attributes.MOVEMENT_SPEED, 1.5); // ⭐ 移速最高
        registerGoddess(greenHeart);
    }

    /**
     * 注册一个女神（也可用于动态添加自定义女神）
     */
    public void registerGoddess(Goddess goddess) {
        goddesses.put(goddess.getId(), goddess);
    }

    /**
     * 根据类型获取女神，若不存在返回 null
     */
    public Goddess getGoddess(GoddessType type) {
        return goddesses.get(type);
    }

    /**
     * 获取所有已注册的女神
     */
    public Collection<Goddess> getAllGoddesses() {
        return goddesses.values();
    }

    /**
     * 检查某类型是否已注册
     */
    public boolean isRegistered(GoddessType type) {
        return goddesses.containsKey(type);
    }
}