package com.MinerDimensionNeptunia.NeptuniaMod.item.usable;

import com.MinerDimensionNeptunia.NeptuniaMod.client.ClientEvents;
import com.MinerDimensionNeptunia.NeptuniaMod.client.gui.GoddessSelectionScreen;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GoddessDiskItem extends Item {
    public GoddessDiskItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (level.isClientSide) {
            // ===== 客户端：检查缓存，判断是否已拥有能力 =====
            boolean hasAbility = ClientEvents.hasGoddessAbility(); // 新增方法，下面会添加
            if (hasAbility) {
                // 已拥有能力，提示并阻止使用
                player.displayClientMessage(Component.literal("你已经拥有女神化的能力了！"), true);
                // 返回 PASS 不消耗物品
                return InteractionResultHolder.pass(itemstack);
            }

            // 未拥有能力，打开选择 GUI
            Minecraft.getInstance().setScreen(new GoddessSelectionScreen());
            return InteractionResultHolder.pass(itemstack);
        }

        // 服务端不处理任何事，返回 PASS（消耗由选择包处理）
        return InteractionResultHolder.pass(itemstack);
    }
}