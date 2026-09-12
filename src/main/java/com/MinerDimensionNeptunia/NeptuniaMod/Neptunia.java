package com.MinerDimensionNeptunia.NeptuniaMod;

import com.MinerDimensionNeptunia.NeptuniaMod.capability.GoddessCapabilityImplementation;
import com.MinerDimensionNeptunia.NeptuniaMod.capability.GoddessCapabilityProvider;
import com.MinerDimensionNeptunia.NeptuniaMod.client.KeyBindings;
import com.MinerDimensionNeptunia.NeptuniaMod.config.GoddessConfig;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.Goddess;
import com.MinerDimensionNeptunia.NeptuniaMod.goddess.GoddessRegistry;
import com.MinerDimensionNeptunia.NeptuniaMod.item.GoddessDiskItem;
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
                        System.out.println("✅ [Neptunia] 客户端配置已注册！");
                } catch (Exception e) {
                        System.err.println("❌ [Neptunia] 配置注册失败: " + e.getMessage());
                        e.printStackTrace();
                }

                // ---- 3. ⭐ 注册配置屏幕工厂（让配置按钮可点击） ----
                try {
                        ModLoadingContext.get().registerExtensionPoint(
                                        ConfigScreenHandler.ConfigScreenFactory.class,
                                        () -> new ConfigScreenHandler.ConfigScreenFactory(
                                                        (client, parent) -> new ModConfigScreen(parent) // 使用自定义配置屏幕
                                        ));
                        System.out.println("✅ [Neptunia] 配置屏幕工厂已注册！");
                } catch (Exception e) {
                        System.err.println("❌ [Neptunia] 配置屏幕工厂注册失败: " + e.getMessage());
                        e.printStackTrace();
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
                        System.out.println("🔗 [Capability] 已附加到玩家");
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
                                                        System.out.println("📥 [服务端] 玩家 "
                                                                        + serverPlayer.getName().getString() +
                                                                        " 登录，从缓存恢复数据 - 能力:" + cached.ability);
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
                                                System.out.println("⏰ [服务端] 玩家 " + serverPlayer.getName().getString()
                                                                + " 的变身已超时，自动解除");
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
                                        System.out.println("💀 [服务端] 玩家 " + player.getName().getString() +
                                                        " 死亡，数据已存入缓存 - 能力:" + cap.getAbility() +
                                                        ", 类型:" + cap.getGoddessType());
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
                                System.out.println("💀 [服务端] 玩家 " + player.getName().getString() +
                                                " 死亡复活，已解除变身状态，能力保留: " + cached.ability);
                        } else {
                                cap.setTransformStartTime(0);
                                System.out.println("💀 [服务端] 玩家 " + player.getName().getString() +
                                                " 死亡复活，无变身状态，能力保留: " + cached.ability);
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
                        System.out.println("📤 [服务端] 死亡复活后同步状态给客户端 - 能力: " +
                                        cap.getAbility() + ", 时间: " + cap.getTransformStartTime() +
                                        ", 类型: " + cap.getGoddessType());
                });
        }

        private void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
                if (event.getEntity() instanceof ServerPlayer player) {
                        UUID uuid = player.getUUID();
                        if (PLAYER_DATA_CACHE.remove(uuid) != null) {
                                System.out.println("🧹 [缓存] 玩家 " + player.getName().getString() + " 登出，已清理缓存数据");
                        }
                }
        }

        private void registerCommands(RegisterCommandsEvent event) {
                GoddessCommand.register(event.getDispatcher());
        }
}