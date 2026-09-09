package com.MinerDimensionNeptunia.NeptuniaMod.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import com.MinerDimensionNeptunia.NeptuniaMod.Neptunia;

public class GoddessCapabilityProvider {
        public static final Capability<GoddessCapability> GODDESS_CAPABILITY = CapabilityManager
                        .get(new CapabilityToken<>() {
                        });

        public static final ResourceLocation GODDESS_CAPABILITY_LOCATION = new ResourceLocation(Neptunia.MODID,
                        "goddess_ability");
}