package com.MinerDimensionNeptunia.NeptuniaMod.util;

/**
 * 女神磁盘的世代（Gen），决定女神化后的属性强弱、飞行能力与获取维度。
 * <p>
 * 属性增幅公式：最终倍率 = 1 + (注册值 − 1) × boostFactor。
 * 注册值视为 Gen5（满额）数值，低世代按比例缩小。
 * 以 Prototype（注册值 ×3.0）为例，各世代实际倍率：
 * Gen1 ×1.3、Gen2 ×1.7、Gen3 ×2.0、Gen4 ×2.5、Gen5 ×3.0。
 * <p>
 * 飞行能力：Gen4 起变身期间获得创造飞行；Gen5 飞行速度 ×1.5
 * （flightSpeedMultiplier 为 0 表示不授予飞行）。
 * <p>
 * 变身时长随世代递增：1 分钟 / 3 分钟 / 6 分钟 / 12 分钟 / 30 分钟。
 */
public enum GoddessDiskGen {
    /** 主世界（地牢 / 废弃矿井 / 沙漠神殿等宝箱，低概率）；变身 1 分钟 */
    GEN1(0.15F, 0F, 60),
    /** 下界（要塞 / 堡垒遗迹宝箱）；变身 3 分钟 */
    GEN2(0.35F, 0F, 180),
    /** 预留：未来新维度的结构宝箱；变身 6 分钟 */
    GEN3(0.5F, 0F, 360),
    /** 末地（末地城宝箱）：变身期间获得创造飞行；变身 12 分钟 */
    GEN4(0.75F, 1.0F, 720),
    /** 预留：未来新维度的结构宝箱：飞行 + 飞行速度 ×1.5；变身 30 分钟 */
    GEN5(1.0F, 1.2F, 1800);

    private final float boostFactor;
    private final float flightSpeedMultiplier;
    private final int transformDurationSeconds;

    GoddessDiskGen(float boostFactor, float flightSpeedMultiplier, int transformDurationSeconds) {
        this.boostFactor = boostFactor;
        this.flightSpeedMultiplier = flightSpeedMultiplier;
        this.transformDurationSeconds = transformDurationSeconds;
    }

    /** 属性增幅 (注册值 − 1) 的缩放系数 */
    public float getBoostFactor() {
        return boostFactor;
    }

    /** 是否在变身期间授予创造飞行（Gen4 及以上） */
    public boolean grantsFlight() {
        return flightSpeedMultiplier > 0F;
    }

    /** 飞行速度倍率（未授予飞行时为 0） */
    public float getFlightSpeedMultiplier() {
        return flightSpeedMultiplier;
    }

    /** 变身持续秒数（客户端倒计时 / HUD / 登录兜底共用） */
    public int getTransformDurationSeconds() {
        return transformDurationSeconds;
    }

    /** 按名字解析；无法识别时回退到满额 Gen5 */
    public static GoddessDiskGen fromName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return GEN5;
        }
    }
}
