package com.MinerDimensionNeptunia.NeptuniaMod.item.ingredient;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 光碟刻印装置（MK1~MK5）：女神磁盘隐藏合成配方的中心关键道具。
 * <p>
 * 耐久条 = 可用刻印次数：每次参与合成消耗 1 次，次数用完后装置直接消失
 * （合成后返回的"剩余物品"为空时，原版工作台会将其消耗掉）。
 * <p>
 * 后续规划：特定怪物掉落、更多用途（升级装备等）。
 */
public class EngraveUnitItem extends Item {
    /** 可用刻印次数（耐久上限） */
    public static final int USES = 10;

    public EngraveUnitItem(Properties properties) {
        super(properties);
    }

    /** 参与合成时不直接消耗，而是返回消耗过耐久的新堆（原版"剩余物品"机制） */
    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        if (stack.getDamageValue() + 1 >= stack.getMaxDamage()) {
            // 次数用完：返回 EMPTY，原版合成流程会将其消耗（装置消失）
            return ItemStack.EMPTY;
        }
        ItemStack remaining = stack.copy();
        remaining.setDamageValue(stack.getDamageValue() + 1);
        return remaining;
    }
}
