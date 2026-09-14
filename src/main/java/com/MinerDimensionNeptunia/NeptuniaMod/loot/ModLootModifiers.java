package com.MinerDimensionNeptunia.NeptuniaMod.loot;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 战利品修改器注册中心。
 * 实例配置见 data/miner_dimension_neptunia/loot_modifiers/，
 * 启用列表见 data/forge/loot_modifiers/global_loot_modifiers.json。
 */
public class ModLootModifiers {
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Neptunia.MODID);

    /** 女神磁盘战利品修改器（注入的磁盘世代 / 战利品表 / 概率等均由 JSON 实例配置） */
    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> GODDESS_DISK =
            LOOT_MODIFIER_SERIALIZERS.register("goddess_disk", () -> GoddessDiskLootModifier.CODEC);

    public static void register(IEventBus modEventBus) {
        LOOT_MODIFIER_SERIALIZERS.register(modEventBus);
    }
}
