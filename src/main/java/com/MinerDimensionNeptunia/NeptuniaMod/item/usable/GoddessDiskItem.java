package com.MinerDimensionNeptunia.NeptuniaMod.item.usable;

import com.MinerDimensionNeptunia.NeptuniaMod.client.gui.GoddessSelectionScreen;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessDiskGen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 女神磁盘（Gen1~Gen5）。
 * <p>
 * 使用后打开女神选择界面，选定后<b>消耗 1 张</b>本世代磁盘；
 * 已拥有能力的玩家也可以再次使用磁盘**重新选择女神**（更换女神/世代）。
 * 磁盘世代记录在玩家能力中，决定属性强弱与变身时长（见 {@link GoddessDiskGen}）。
 */
public class GoddessDiskItem extends Item {
    private final GoddessDiskGen gen;

    public GoddessDiskItem(GoddessDiskGen gen, Properties properties) {
        super(properties);
        this.gen = gen;
    }

    public GoddessDiskGen getGen() {
        return gen;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (level.isClientSide) {
            // 打开选择 GUI（磁盘世代随选择一起提交；消耗在服务端选择包中处理）
            Minecraft.getInstance().setScreen(new GoddessSelectionScreen(this.gen));
            return InteractionResultHolder.pass(itemstack);
        }

        // 服务端不处理任何事，返回 PASS（选定与消耗均由选择包处理）
        return InteractionResultHolder.pass(itemstack);
    }
}
