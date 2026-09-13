package com.MinerDimensionNeptunia.NeptuniaMod;

import com.MinerDimensionNeptunia.NeptuniaMod.capability.GoddessCapabilityImplementation;
import com.MinerDimensionNeptunia.NeptuniaMod.capability.GoddessCapabilityProvider;
import com.MinerDimensionNeptunia.NeptuniaMod.client.KeyBindings;
import com.MinerDimensionNeptunia.NeptuniaMod.config.GoddessConfig;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.Goddess;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.GoddessRegistry;
import com.MinerDimensionNeptunia.NeptuniaMod.item.usable.GoddessDiskItem;
import com.MinerDimensionNeptunia.NeptuniaMod.item.weapon.GoddessWeaponItem;
import com.MinerDimensionNeptunia.NeptuniaMod.network.GoddessAbilitySyncPacket;
import com.MinerDimensionNeptunia.NeptuniaMod.network.GoddessTypeSelectPacket;
import com.MinerDimensionNeptunia.NeptuniaMod.network.TransformRequestPacket;
import com.MinerDimensionNeptunia.NeptuniaMod.util.GoddessType;
import com.MinerDimensionNeptunia.NeptuniaMod.command.GoddessCommand;
import com.MinerDimensionNeptunia.NeptuniaMod.item.ModCreativeTabs;
import com.MinerDimensionNeptunia.NeptuniaMod.loot.ModLootModifiers;
import com.MinerDimensionNeptunia.NeptuniaMod.recipe.ModRecipeSerializers;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.client.ConfigScreenHandler;
import com.MinerDimensionNeptunia.NeptuniaMod.client.gui.ModConfigScreen;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod(Neptunia.MODID)
public class Neptunia {
        public static final String MODID = "miner_dimension_neptunia";
        private static final Logger LOGGER = LogUtils.getLogger();
        private static final String PROTOCOL_VERSION = "1";
        private static final int TRANSFORM_DURATION = 180;

        public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
                        new ResourceLocation(MODID, "main"),
                        () -> PROTOCOL_VERSION,
                        PROTOCOL_VERSION::equals,
                        PROTOCOL_VERSION::equals);

        public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

        public static final RegistryObject<Item> GODDESS_DISK = ITEMS.register("goddess_disk",
                        () -> new GoddessDiskItem(new Item.Properties().stacksTo(64)));

        // ---- 女神武器：5 位女神 × 6 阶 ----
        // 伤害阶梯（总伤害）：7 / 8 / 12 / 18 / 27 / 54
        // 攻速按武器类型差异化（刺剑最快、战锤最慢），完整数值表见 tools/weapon_tiers.csv
        // 耐久为伪耐久条：不会损坏，随时间与击杀恢复（见 GoddessWeaponEvents）
        public static final RegistryObject<Item> PROTOTYPE_RAPIER_TIER1 = registerWeapon("prototype_rapier_tier1", GoddessType.PROTOTYPE, 3, -2.0F);
        public static final RegistryObject<Item> PROTOTYPE_RAPIER_TIER2 = registerWeapon("prototype_rapier_tier2", GoddessType.PROTOTYPE, 4, -2.0F);
        public static final RegistryObject<Item> PROTOTYPE_RAPIER_TIER3 = registerWeapon("prototype_rapier_tier3", GoddessType.PROTOTYPE, 8, -2.1F);
        public static final RegistryObject<Item> PROTOTYPE_RAPIER_TIER4 = registerWeapon("prototype_rapier_tier4", GoddessType.PROTOTYPE, 14, -2.1F);
        public static final RegistryObject<Item> PROTOTYPE_RAPIER_TIER5 = registerWeapon("prototype_rapier_tier5", GoddessType.PROTOTYPE, 23, -2.2F);
        public static final RegistryObject<Item> PROTOTYPE_RAPIER_TIER6 = registerWeapon("prototype_rapier_tier6", GoddessType.PROTOTYPE, 50, -2.2F);

        public static final RegistryObject<Item> NEPTUNE_SWORD_TIER1 = registerWeapon("neptune_sword_tier1", GoddessType.PURPLE_HEART, 3, -2.2F);
        public static final RegistryObject<Item> NEPTUNE_SWORD_TIER2 = registerWeapon("neptune_sword_tier2", GoddessType.PURPLE_HEART, 4, -2.2F);
        public static final RegistryObject<Item> NEPTUNE_SWORD_TIER3 = registerWeapon("neptune_sword_tier3", GoddessType.PURPLE_HEART, 8, -2.3F);
        public static final RegistryObject<Item> NEPTUNE_SWORD_TIER4 = registerWeapon("neptune_sword_tier4", GoddessType.PURPLE_HEART, 14, -2.3F);
        public static final RegistryObject<Item> NEPTUNE_SWORD_TIER5 = registerWeapon("neptune_sword_tier5", GoddessType.PURPLE_HEART, 23, -2.4F);
        public static final RegistryObject<Item> NEPTUNE_SWORD_TIER6 = registerWeapon("neptune_sword_tier6", GoddessType.PURPLE_HEART, 50, -2.4F);

        public static final RegistryObject<Item> NOIRE_SWORD_TIER1 = registerWeapon("noire_sword_tier1", GoddessType.BLACK_HEART, 3, -2.4F);
        public static final RegistryObject<Item> NOIRE_SWORD_TIER2 = registerWeapon("noire_sword_tier2", GoddessType.BLACK_HEART, 4, -2.4F);
        public static final RegistryObject<Item> NOIRE_SWORD_TIER3 = registerWeapon("noire_sword_tier3", GoddessType.BLACK_HEART, 8, -2.5F);
        public static final RegistryObject<Item> NOIRE_SWORD_TIER4 = registerWeapon("noire_sword_tier4", GoddessType.BLACK_HEART, 14, -2.5F);
        public static final RegistryObject<Item> NOIRE_SWORD_TIER5 = registerWeapon("noire_sword_tier5", GoddessType.BLACK_HEART, 23, -2.6F);
        public static final RegistryObject<Item> NOIRE_SWORD_TIER6 = registerWeapon("noire_sword_tier6", GoddessType.BLACK_HEART, 50, -2.6F);

        public static final RegistryObject<Item> BLANC_HAMMER_TIER1 = registerWeapon("blanc_hammer_tier1", GoddessType.WHITE_HEART, 3, -2.8F);
        public static final RegistryObject<Item> BLANC_HAMMER_TIER2 = registerWeapon("blanc_hammer_tier2", GoddessType.WHITE_HEART, 4, -2.8F);
        public static final RegistryObject<Item> BLANC_HAMMER_TIER3 = registerWeapon("blanc_hammer_tier3", GoddessType.WHITE_HEART, 8, -2.9F);
        public static final RegistryObject<Item> BLANC_HAMMER_TIER4 = registerWeapon("blanc_hammer_tier4", GoddessType.WHITE_HEART, 14, -2.9F);
        public static final RegistryObject<Item> BLANC_HAMMER_TIER5 = registerWeapon("blanc_hammer_tier5", GoddessType.WHITE_HEART, 23, -3.0F);
        public static final RegistryObject<Item> BLANC_HAMMER_TIER6 = registerWeapon("blanc_hammer_tier6", GoddessType.WHITE_HEART, 50, -3.0F);

        public static final RegistryObject<Item> VERT_SPEAR_TIER1 = registerWeapon("vert_spear_tier1", GoddessType.GREEN_HEART, 3, -2.6F);
        public static final RegistryObject<Item> VERT_SPEAR_TIER2 = registerWeapon("vert_spear_tier2", GoddessType.GREEN_HEART, 4, -2.6F);
        public static final RegistryObject<Item> VERT_SPEAR_TIER3 = registerWeapon("vert_spear_tier3", GoddessType.GREEN_HEART, 8, -2.7F);
        public static final RegistryObject<Item> VERT_SPEAR_TIER4 = registerWeapon("vert_spear_tier4", GoddessType.GREEN_HEART, 14, -2.7F);
        public static final RegistryObject<Item> VERT_SPEAR_TIER5 = registerWeapon("vert_spear_tier5", GoddessType.GREEN_HEART, 23, -2.8F);
        public static final RegistryObject<Item> VERT_SPEAR_TIER6 = registerWeapon("vert_spear_tier6", GoddessType.GREEN_HEART, 50, -2.8F);

        /** 所有女神武器（创造标签页与 JEI 展示用） */
        public static final java.util.List<RegistryObject<Item>> ALL_GODDESS_WEAPONS = java.util.List.of(
                        PROTOTYPE_RAPIER_TIER1, PROTOTYPE_RAPIER_TIER2, PROTOTYPE_RAPIER_TIER3,
                        PROTOTYPE_RAPIER_TIER4, PROTOTYPE_RAPIER_TIER5, PROTOTYPE_RAPIER_TIER6,
                        NEPTUNE_SWORD_TIER1, NEPTUNE_SWORD_TIER2, NEPTUNE_SWORD_TIER3,
                        NEPTUNE_SWORD_TIER4, NEPTUNE_SWORD_TIER5, NEPTUNE_SWORD_TIER6,
                        NOIRE_SWORD_TIER1, NOIRE_SWORD_TIER2, NOIRE_SWORD_TIER3,
                        NOIRE_SWORD_TIER4, NOIRE_SWORD_TIER5, NOIRE_SWORD_TIER6,
                        BLANC_HAMMER_TIER1, BLANC_HAMMER_TIER2, BLANC_HAMMER_TIER3,
                        BLANC_HAMMER_TIER4, BLANC_HAMMER_TIER5, BLANC_HAMMER_TIER6,
                        VERT_SPEAR_TIER1, VERT_SPEAR_TIER2, VERT_SPEAR_TIER3,
                        VERT_SPEAR_TIER4, VERT_SPEAR_TIER5, VERT_SPEAR_TIER6);

        private static RegistryObject<Item> registerWeapon(String name, GoddessType type, int damageBonus, float speedModifier) {
                return ITEMS.register(name,
                                () -> new GoddessWeaponItem(type, Tiers.DIAMOND, damageBonus, speedModifier,
                                                new Item.Properties().durability(GoddessWeaponItem.DURABILITY)));
        }

        private static final Map<UUID, SavedPlayerData> PLAYER_DATA_CACHE = new ConcurrentHashMap<>();

        private static class SavedPlayerData {
                final boolean ability;
                final GoddessType goddessType;
                final long transformStartTime;

                SavedPlayerData(boolean ability, GoddessType goddessType, long transformStartTime) {
                        this.ability = ability;
                        this.goddessType = goddessType;
                        this.transformStartTime = transformStartTime;
                }
        }

        public static void updatePlayerCache(UUID uuid, boolean ability, GoddessType type, long startTime) {
                if (!ability && type == GoddessType.NONE && startTime == 0) {
                        PLAYER_DATA_CACHE.remove(uuid);
                        return;
                }
                PLAYER_DATA_CACHE.put(uuid, new SavedPlayerData(ability, type, startTime));
        }

        public static SavedPlayerData popPlayerCache(UUID uuid) {
                return PLAYER_DATA_CACHE.remove(uuid);
        }

        public Neptunia() {
                // ---- 1. 注册网络包 ----
                CHANNEL.registerMessage(0, GoddessAbilitySyncPacket.class,
                                GoddessAbilitySyncPacket::encode,
                                GoddessAbilitySyncPacket::decode,
                                GoddessAbilitySyncPacket::handle);
                CHANNEL.registerMessage(1, TransformRequestPacket.class,
                                TransformRequestPacket::encode,
                                TransformRequestPacket::decode,
                                TransformRequestPacket::handle);
                CHANNEL.registerMessage(2, GoddessTypeSelectPacket.class,
                                GoddessTypeSelectPacket::encode,
                                GoddessTypeSelectPacket::decode,
                                GoddessTypeSelectPacket::handle);

                // ---- 2. 注册客户端配置 ----
                try {
                        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, GoddessConfig.CLIENT_SPEC);
                } catch (Exception e) {
                        // 配置注册失败
                }

                // ---- 3. ⭐ 注册配置屏幕工厂（让配置按钮可点击） ----
                try {
                        ModLoadingContext.get().registerExtensionPoint(
                                        ConfigScreenHandler.ConfigScreenFactory.class,
                                        () -> new ConfigScreenHandler.ConfigScreenFactory(
                                                        (client, parent) -> new ModConfigScreen(parent) // 使用自定义配置屏幕
                                        ));
                } catch (Exception e) {
                        // 配置屏幕工厂注册失败
                }

                // ---- 4. 注册物品和事件 ----
                IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

                ITEMS.register(modEventBus);
                ModCreativeTabs.register(modEventBus);      // 创造标签页（见 item 包）
                ModRecipeSerializers.register(modEventBus); // 配方序列化器（见 recipe 包）
                ModLootModifiers.register(modEventBus);     // 战利品修改器（见 loot 包）
                modEventBus.addListener(this::registerKeyMappings);

                MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, this::attachCapabilities);
                MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
                MinecraftForge.EVENT_BUS.addListener(this::onPlayerDeath);
                MinecraftForge.EVENT_BUS.addListener(this::onPlayerRespawn);
                MinecraftForge.EVENT_BUS.addListener(this::onPlayerLogout);
                MinecraftForge.EVENT_BUS.addListener(this::registerCommands);

                LOGGER.info("Neptunia Mod 初始化完成！");
        }

        private void registerKeyMappings(RegisterKeyMappingsEvent event) {
                event.register(KeyBindings.transformKey);
        }

        private void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
                if (event.getObject() instanceof Player) {
                        event.addCapability(GoddessCapabilityProvider.GODDESS_CAPABILITY_LOCATION,
                                        new GoddessCapabilityImplementation());
                }
        }

        private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
                if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                        UUID uuid = serverPlayer.getUUID();

                        SavedPlayerData cached = PLAYER_DATA_CACHE.remove(uuid);
                        if (cached != null) {
                                serverPlayer.getCapability(GoddessCapabilityProvider.GODDESS_CAPABILITY)
                                                .ifPresent(cap -> {
                                                        cap.setAbility(cached.ability);
                                                        cap.setGoddessType(cached.goddessType);
                                                        cap.setTransformStartTime(cached.transformStartTime);
                                                });
                        }

                        serverPlayer.getCapability(GoddessCapabilityProvider.GODDESS_CAPABILITY).ifPresent(cap -> {
                                boolean hasAbility = cap.getAbility();
                                long startTime = cap.getTransformStartTime();
                                GoddessType type = cap.getGoddessType();

                                if (startTime > 0) {
                                        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                                        if (elapsed >= TRANSFORM_DURATION) {
                                                cap.setTransformStartTime(0);
                                                startTime = 0;
                                        }
                                }

                                updatePlayerCache(uuid, hasAbility, type, startTime);

                                CHANNEL.send(
                                                PacketDistributor.PLAYER.with(() -> serverPlayer),
                                                new GoddessAbilitySyncPacket(hasAbility, startTime, type));
                        });
                }
        }

        private void onPlayerDeath(LivingDeathEvent event) {
                if (event.getEntity() instanceof ServerPlayer player) {
                        UUID uuid = player.getUUID();
                        player.getCapability(GoddessCapabilityProvider.GODDESS_CAPABILITY).ifPresent(cap -> {
                                if (cap.getAbility() || cap.getTransformStartTime() > 0) {
                                        updatePlayerCache(uuid, cap.getAbility(), cap.getGoddessType(),
                                                        cap.getTransformStartTime());
                                } else {
                                        PLAYER_DATA_CACHE.remove(uuid);
                                }
                        });
                }
        }

        private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
                if (!(event.getEntity() instanceof ServerPlayer player))
                        return;

                UUID uuid = player.getUUID();
                SavedPlayerData cached = PLAYER_DATA_CACHE.remove(uuid);

                if (cached == null) {
                        player.getCapability(GoddessCapabilityProvider.GODDESS_CAPABILITY).ifPresent(cap -> {
                                if (cap.getTransformStartTime() > 0) {
                                        Goddess goddess = GoddessRegistry.getInstance()
                                                        .getGoddess(cap.getGoddessType());
                                        if (goddess != null) {
                                                TransformRequestPacket.applyGoddessBoost(player, goddess, false);
                                        }
                                        cap.setTransformStartTime(0);
                                        CHANNEL.send(
                                                        PacketDistributor.PLAYER.with(() -> player),
                                                        new GoddessAbilitySyncPacket(cap.getAbility(), 0,
                                                                        cap.getGoddessType()));
                                }
                        });
                        return;
                }

                player.getCapability(GoddessCapabilityProvider.GODDESS_CAPABILITY).ifPresent(cap -> {
                        cap.setAbility(cached.ability);
                        cap.setGoddessType(cached.goddessType);

                        if (cached.transformStartTime > 0) {
                                Goddess goddess = GoddessRegistry.getInstance().getGoddess(cached.goddessType);
                                if (goddess != null) {
                                        TransformRequestPacket.applyGoddessBoost(player, goddess, false);
                                }
                                cap.setTransformStartTime(0);
                        } else {
                                cap.setTransformStartTime(0);
                        }

                        if (cap.getAbility()) {
                                updatePlayerCache(uuid, cap.getAbility(), cap.getGoddessType(), 0);
                        }

                        CHANNEL.send(
                                        PacketDistributor.PLAYER.with(() -> player),
                                        new GoddessAbilitySyncPacket(
                                                        cap.getAbility(),
                                                        cap.getTransformStartTime(),
                                                        cap.getGoddessType()));
                });
        }

        private void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
                if (event.getEntity() instanceof ServerPlayer player) {
                        UUID uuid = player.getUUID();
                        PLAYER_DATA_CACHE.remove(uuid);
                }
        }

        private void registerCommands(RegisterCommandsEvent event) {
                GoddessCommand.register(event.getDispatcher());
        }
}