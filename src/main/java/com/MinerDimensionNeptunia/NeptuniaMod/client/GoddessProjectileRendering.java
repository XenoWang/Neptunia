package com.MinerDimensionNeptunia.NeptuniaMod.client;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Neptunia.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GoddessProjectileRendering {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Neptunia.GODDESS_PROJECTILE.get(),
                context -> new ThrownItemRenderer<>(context, 0.6F, true));
    }
}
