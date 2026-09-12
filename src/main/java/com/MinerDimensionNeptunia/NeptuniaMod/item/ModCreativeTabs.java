package com.MinerDimensionNeptunia.NeptuniaMod.item;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 创造模式标签页注册中心。
 * 物品进入标签页后，原版创造物品栏搜索和 JEI 搜索都能找到它。
 */
public class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Neptunia.MODID);

    public static final RegistryObject<CreativeModeTab> NEPTUNIA_TAB = CREATIVE_TABS.register("neptunia",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.miner_dimension_neptunia"))
                    .icon(() -> new ItemStack(Neptunia.GODDESS_DISK.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(Neptunia.GODDESS_DISK.get());
                        // 女神默认武器
                        output.accept(Neptunia.PROTOTYPE_RAPIER.get());
                        output.accept(Neptunia.PURPLE_HEART_KATANA.get());
                        output.accept(Neptunia.BLACK_HEART_LONGSWORD.get());
                        output.accept(Neptunia.WHITE_HEART_HAMMER.get());
                        output.accept(Neptunia.GREEN_HEART_SPEAR.get());
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        CREATIVE_TABS.register(modEventBus);
    }
}
