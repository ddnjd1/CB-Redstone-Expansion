package Comet_Blaze.neo.cbadd;

import Comet_Blaze.neo.cbadd.config.RedstoneConnectorConfig;
import Comet_Blaze.neo.cbadd.init.*;
import Comet_Blaze.neo.cbadd.network.BindKeyPacket;
import Comet_Blaze.neo.cbadd.network.TriggerKeyPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod(CbaddMod.MODID)
public class CbaddMod {
    public static final String MODID = "cbadd";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Collection<AbstractMap.SimpleEntry<Runnable, Integer>> workQueue = new ConcurrentLinkedQueue<>();

    public CbaddMod(IEventBus modEventBus, ModContainer modContainer) {
        // 注册配置
        modContainer.registerConfig(ModConfig.Type.SERVER, RedstoneConnectorConfig.SPEC);

        // 注册各内容
        CbaddModBlocks.BLOCKS.register(modEventBus);
        CbaddModItems.ITEMS.register(modEventBus);
        CbaddModTabs.CREATIVE_MODE_TABS.register(modEventBus);
        CbaddModBlockEntities.REGISTRY.register(modEventBus);

        // 注册游戏总线事件
        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterPayloads);

    }
    // === 网络包注册 ===
    private void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        // 注册一个从客户端发往服务端的包
        registrar.playToServer(
                BindKeyPacket.TYPE,
                BindKeyPacket.STREAM_CODEC,
                BindKeyPacket::handleServer
        );
        registrar.playToServer(
                TriggerKeyPacket.TYPE,
                TriggerKeyPacket.STREAM_CODEC,
                TriggerKeyPacket::handleServer
        );
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("HELLO FROM CLIENT SETUP");
        LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("CBADD Mod Initialized with:");
        LOGGER.info("- {} blocks", CbaddModBlocks.BLOCKS.getEntries().size());
        LOGGER.info("- {} items", CbaddModItems.ITEMS.getEntries().size());
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // 可选
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!workQueue.isEmpty()) {
            List<AbstractMap.SimpleEntry<Runnable, Integer>> actionsToExecute = new ArrayList<>();
            synchronized (workQueue) {
                workQueue.removeIf(entry -> {
                    int remaining = entry.getValue() - 1;
                    if (remaining <= 0) {
                        actionsToExecute.add(entry);
                        return true;
                    } else {
                        entry.setValue(remaining);
                        return false;
                    }
                });
            }
            for (AbstractMap.SimpleEntry<Runnable, Integer> actionEntry : actionsToExecute) {
                try {
                    actionEntry.getKey().run();
                } catch (Exception e) {
                    LOGGER.error("Error executing queued work: {}", e.getMessage(), e);
                }
            }
        }
    }
}
