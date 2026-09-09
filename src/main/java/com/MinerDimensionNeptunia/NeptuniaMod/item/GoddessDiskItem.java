package com.MinerDimensionNeptunia.NeptuniaMod.item;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import com.MinerDimensionNeptunia.NeptuniaMod.client.gui.GoddessSelectionScreen;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import net.minecraft.client.Minecraft;
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
            // 客户端打开选择 GUI
            Minecraft.getInstance().setScreen(new GoddessSelectionScreen());
            System.out.println("🖥️ [客户端] 打开女神选择界面");
            return InteractionResultHolder.pass(itemstack);
        } else {
            // 服务端逻辑：由于选择界面在客户端，服务端仅负责处理选择包，所以这里不做任何事
            return InteractionResultHolder.pass(itemstack);
        }
    }
}