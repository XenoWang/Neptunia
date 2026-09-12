package com.MinerDimensionNeptunia.NeptuniaMod.recipe;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 配方序列化器注册中心。
 */
public class ModRecipeSerializers {
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Neptunia.MODID);

    /** 女神磁盘的隐藏合成配方（配方默认不在配方书/JEI 中展示） */
    public static final RegistryObject<RecipeSerializer<HiddenGoddessDiskRecipe>> GODDESS_DISK =
            RECIPE_SERIALIZERS.register("goddess_disk", HiddenGoddessDiskRecipe.Serializer::new);

    public static void register(IEventBus modEventBus) {
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
