package com.MinerDimensionNeptunia.NeptuniaMod.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * 女神磁盘的隐藏合成配方：
 * 与普通有序合成完全一致（可以正常在工作台合成），
 * 但标记为"特殊配方"，因此不会显示在配方书中（JEI 中由 JEI 插件负责隐藏）。
 */
public class HiddenGoddessDiskRecipe extends ShapedRecipe {
    public HiddenGoddessDiskRecipe(ResourceLocation id, String group, CraftingBookCategory category,
                                   int width, int height, NonNullList<Ingredient> ingredients,
                                   ItemStack result, boolean showNotification) {
        super(id, group, category, width, height, ingredients, result, showNotification);
    }

    /** 标记为特殊配方：不在配方书中展示（不影响工作台合成） */
    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<HiddenGoddessDiskRecipe> {
        private static final ShapedRecipe.Serializer SHAPED = new ShapedRecipe.Serializer();

        @Override
        public HiddenGoddessDiskRecipe fromJson(ResourceLocation id, JsonObject json) {
            return wrap(SHAPED.fromJson(id, json));
        }

        @Override
        public HiddenGoddessDiskRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return wrap(SHAPED.fromNetwork(id, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, HiddenGoddessDiskRecipe recipe) {
            SHAPED.toNetwork(buffer, recipe);
        }

        /** 复用原版 shaped 序列化器的解析结果，包装成隐藏配方 */
        private static HiddenGoddessDiskRecipe wrap(ShapedRecipe shaped) {
            return new HiddenGoddessDiskRecipe(
                    shaped.getId(), shaped.getGroup(), shaped.category(),
                    shaped.getWidth(), shaped.getHeight(), shaped.getIngredients(),
                    shaped.getResultItem(RegistryAccess.EMPTY), shaped.showNotification());
        }
    }
}
