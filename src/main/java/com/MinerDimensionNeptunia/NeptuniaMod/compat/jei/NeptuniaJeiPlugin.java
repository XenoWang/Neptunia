package com.MinerDimensionNeptunia.NeptuniaMod.compat.jei;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/**
 * JEI 兼容插件（仅当安装了 JEI 时才会被加载，未安装 JEI 时本类不会被加载，不会影响游戏）。
 * 注意：JEI 在 Forge 端通过扫描 @JeiPlugin 注解发现插件（不是 ServiceLoader），
 * 所以必须保留本注解。
 * 1. 女神磁盘显示"可在末地城宝箱中找到"的信息提示
 * 2. 隐藏女神磁盘的合成配方（配方默认不展示）
 */
@JeiPlugin
public class NeptuniaJeiPlugin implements IModPlugin {
    public static final ResourceLocation UID = new ResourceLocation(Neptunia.MODID, "jei_plugin");

    /** 需要从 JEI 中隐藏的磁盘合成配方（Gen3 / Gen5 暂无配方） */
    private static final List<ResourceLocation> HIDDEN_DISK_RECIPE_IDS = List.of(
            new ResourceLocation(Neptunia.MODID, "goddess_disk_gen1"),
            new ResourceLocation(Neptunia.MODID, "goddess_disk_gen2"),
            new ResourceLocation(Neptunia.MODID, "goddess_disk_gen4"));

    private static IJeiRuntime runtime;

    public NeptuniaJeiPlugin() {
        // 本类只会在 JEI 加载插件时实例化，此时才注册事件监听
        MinecraftForge.EVENT_BUS.register(NeptuniaJeiPlugin.class);
    }

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        // 双保险：磁盘虽在创造标签页中，仍手动注册一次，保证 JEI 必然能搜到
        List<ItemStack> disks = new java.util.ArrayList<>();
        for (var disk : Neptunia.GODDESS_DISKS) {
            disks.add(new ItemStack(disk.get()));
        }
        registration.addExtraItemStacks(disks);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 每个世代的磁盘显示各自的获取来源提示
        for (var disk : Neptunia.GODDESS_DISKS) {
            registration.addItemStackInfo(
                    new ItemStack(disk.get()),
                    Component.translatable("jei.miner_dimension_neptunia." + disk.getId().getPath() + ".info"));
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        hideGoddessDiskRecipe();
    }

    /** 配方同步到客户端后 JEI 会重建配方列表，延迟到下一轮任务中重新隐藏 */
    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        Minecraft.getInstance().execute(NeptuniaJeiPlugin::hideGoddessDiskRecipe);
    }

    private static void hideGoddessDiskRecipe() {
        if (runtime == null)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;
        List<CraftingRecipe> toHide = new java.util.ArrayList<>();
        for (ResourceLocation id : HIDDEN_DISK_RECIPE_IDS) {
            mc.level.getRecipeManager().byKey(id).ifPresent(recipe -> {
                if (recipe instanceof CraftingRecipe crafting) {
                    toHide.add(crafting);
                }
            });
        }
        if (!toHide.isEmpty()) {
            runtime.getRecipeManager().hideRecipes(RecipeTypes.CRAFTING, toHide);
        }
    }
}
