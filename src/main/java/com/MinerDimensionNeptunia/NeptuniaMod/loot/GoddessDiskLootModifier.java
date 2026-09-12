package com.MinerDimensionNeptunia.NeptuniaMod.loot;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
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
 * 女神磁盘战利品修改器：按概率向指定战利品表（默认末地城宝箱）中添加女神磁盘。
 * 具体概率和条件见 data/miner_dimension_neptunia/loot_modifiers/goddess_disk_in_end_city.json
 */
public class GoddessDiskLootModifier extends LootModifier {
    public static final Codec<GoddessDiskLootModifier> CODEC = RecordCodecBuilder.create(inst ->
            codecStart(inst)
                    .and(Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance))
                    .and(Codec.INT.fieldOf("min").forGetter(m -> m.minCount))
                    .and(Codec.INT.fieldOf("max").forGetter(m -> m.maxCount))
                    .apply(inst, GoddessDiskLootModifier::new));

    private final float chance;
    private final int minCount;
    private final int maxCount;

    public GoddessDiskLootModifier(LootItemCondition[] conditions, float chance, int minCount, int maxCount) {
        super(conditions);
        this.chance = chance;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() < this.chance) {
            int count = this.minCount + (this.maxCount > this.minCount
                    ? context.getRandom().nextInt(this.maxCount - this.minCount + 1) : 0);
            generatedLoot.add(new ItemStack(Neptunia.GODDESS_DISK.get(), count));
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
