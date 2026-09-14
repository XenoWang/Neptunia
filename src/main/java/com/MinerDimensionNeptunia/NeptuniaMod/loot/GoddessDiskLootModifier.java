package com.MinerDimensionNeptunia.NeptuniaMod.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

/**
 * 女神磁盘战利品修改器：按概率向指定战利品表（JSON 的 conditions 指定）
 * 注入指定世代的女神磁盘。
 * <p>
 * 每个 JSON 实例独立配置「注入哪个磁盘 / 哪个战利品表 / 概率」，
 * 例如 Gen1 注入主世界宝箱（低概率）、Gen4 注入末地城宝箱（高概率）。
 * 配置见 data/miner_dimension_neptunia/loot_modifiers/。
 */
public class GoddessDiskLootModifier extends LootModifier {
    public static final Codec<GoddessDiskLootModifier> CODEC = RecordCodecBuilder.create(inst ->
            codecStart(inst)
                    .and(ItemStack.CODEC.fieldOf("item").forGetter(m -> m.item))
                    .and(Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance))
                    .and(Codec.INT.fieldOf("min").forGetter(m -> m.minCount))
                    .and(Codec.INT.fieldOf("max").forGetter(m -> m.maxCount))
                    .apply(inst, GoddessDiskLootModifier::new));

    private final ItemStack item;
    private final float chance;
    private final int minCount;
    private final int maxCount;

    public GoddessDiskLootModifier(LootItemCondition[] conditions, ItemStack item,
                                   float chance, int minCount, int maxCount) {
        super(conditions);
        this.item = item;
        this.chance = chance;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() < this.chance) {
            int count = this.minCount + (this.maxCount > this.minCount
                    ? context.getRandom().nextInt(this.maxCount - this.minCount + 1) : 0);
            ItemStack stack = this.item.copy();
            stack.setCount(count);
            generatedLoot.add(stack);
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
