package com.MinerDimensionNeptunia.NeptuniaMod.client;

import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;
import com.MinerDimensionNeptunia.NeptuniaMod.entity.GoddessProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Neptunia.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GoddessProjectileRendering {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Neptunia.GODDESS_PROJECTILE.get(),
                ProjectileRenderer::new);
    }

    /** 模型尖端朝局部 +Z，随弹射物速度朝向旋转。 */
    private static class ProjectileRenderer extends EntityRenderer<GoddessProjectile> {
        private final ItemRenderer itemRenderer;

        ProjectileRenderer(EntityRendererProvider.Context context) {
            super(context);
            itemRenderer = context.getItemRenderer();
        }

        @Override
        public void render(GoddessProjectile projectile, float yaw, float partialTick, PoseStack pose,
                           MultiBufferSource buffers, int light) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, projectile.yRotO, projectile.getYRot())));
            pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, projectile.xRotO, projectile.getXRot())));
            pose.scale(0.6F, 0.6F, 0.6F);
            itemRenderer.renderStatic(projectile.getItem(), ItemDisplayContext.NONE, light,
                    OverlayTexture.NO_OVERLAY, pose, buffers, projectile.level(), projectile.getId());
            pose.popPose();
            super.render(projectile, yaw, partialTick, pose, buffers, light);
        }

        @Override
        public ResourceLocation getTextureLocation(GoddessProjectile projectile) {
            return TextureAtlas.LOCATION_BLOCKS;
        }
    }
}
