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

        // ---- 女神默认武器（钻石强度基准，选定女神后自动获得） ----
        // 参数说明：SwordItem(Tier, 额外伤害, 攻速) -> 实际伤害 = 额外伤害 + 钻石基础加成 3
        // 耐久为伪耐久条：不会损坏，随时间与击杀恢复（见 GoddessWeaponEvents）
        public static final RegistryObject<Item> PROTOTYPE_RAPIER = ITEMS.register("prototype_rapier",
                        () -> new GoddessWeaponItem(GoddessType.PROTOTYPE, Tiers.DIAMOND, 2, -1.8F,
                                        new Item.Properties().durability(GoddessWeaponItem.DURABILITY)));

        public static final RegistryObject<Item> PURPLE_HEART_KATANA = ITEMS.register("purple_heart_katana",
                        () -> new GoddessWeaponItem(GoddessType.PURPLE_HEART, Tiers.DIAMOND, 3, -2.2F,
                                        new Item.Properties().durability(GoddessWeaponItem.DURABILITY)));

        public static final RegistryObject<Item> BLACK_HEART_LONGSWORD = ITEMS.register("black_heart_longsword",
                        () -> new GoddessWeaponItem(GoddessType.BLACK_HEART, Tiers.DIAMOND, 3, -2.4F,
                                        new Item.Properties().durability(GoddessWeaponItem.DURABILITY)));

        public static final RegistryObject<Item> WHITE_HEART_HAMMER = ITEMS.register("white_heart_hammer",
                        () -> new GoddessWeaponItem(GoddessType.WHITE_HEART, Tiers.DIAMOND, 5, -3.0F,
                                        new Item.Properties().durability(GoddessWeaponItem.DURABILITY)));

        public static final RegistryObject<Item> GREEN_HEART_SPEAR = ITEMS.register("green_heart_spear",
                        () -> new GoddessWeaponItem(GoddessType.GREEN_HEART, Tiers.DIAMOND, 4, -2.6F,
                                        new Item.Properties().durability(GoddessWeaponItem.DURABILITY)));

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