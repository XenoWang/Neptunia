package com.MinerDimensionNeptunia.NeptuniaMod.goddess;

import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
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
        // 原型女神（平衡型）
        Goddess prototype = new Goddess(GoddessType.PROTOTYPE, "Prototype Goddess")
                .addAttributeBoost(Attributes.ATTACK_DAMAGE, 2.0)
                .addAttributeBoost(Attributes.MOVEMENT_SPEED, 2.0)
                .addAttributeBoost(Attributes.ARMOR, 2.0)
                .addAttributeBoost(Attributes.ARMOR_TOUGHNESS, 2.0);
        registerGoddess(prototype);

        // ============================================================
        // 🆕 在这里添加更多女神，例如：
        // ============================================================
        // Goddess PurpleHeart = new Goddess(GoddessType.STRENGTH, "紫色之心")
        //         .addAttributeBoost(Attributes.ATTACK_DAMAGE, 3.0)
        //         .addAttributeBoost(Attributes.ARMOR, 1.5);
        // registerGoddess(PurpleHeart);
        // ============================================================
    }

    /**
     * 注册一个女神（也可用于动态添加自定义女神）
     */
    public void registerGoddess(Goddess goddess) {
        goddesses.put(goddess.getId(), goddess);
        System.out.println("📝 [GoddessRegistry] 已注册女神: " + goddess.getDisplayName());
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