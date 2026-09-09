package com.MinerDimensionNeptunia.NeptuniaMod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class KeyBindings {
    public static final String KEY_CATEGORY = "key.category.neptuniamod";
    public static final String KEY_TRANSFORM = "key.neptuniamod.transform";

    public static final KeyMapping transformKey = new KeyMapping(
            KEY_TRANSFORM,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KEY_CATEGORY
    );
}