package Comet_Blaze.neo.cbadd.client.event;

import Comet_Blaze.neo.cbadd.CbaddMod;
import Comet_Blaze.neo.cbadd.item.KeyBindings;
import Comet_Blaze.neo.cbadd.item.KeyTriggerBlockItem;
import Comet_Blaze.neo.cbadd.network.TriggerKeyPacket;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.*;

@EventBusSubscriber(modid = CbaddMod.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    @Nullable
    private static BlockPos currentActivePos = null;
    private static final Set<KeyBindings> pressedKeys = new HashSet<>();
    private static final Map<KeyBindings, Integer> lastHeartbeatTick = new HashMap<>();
    private static int justActivatedTimer = 0;
    private static final int ACTIVATION_COOLDOWN_TICKS = 5;
    private static boolean shiftWasDown = false;

    // 原版 KeyMapping 拦截列表
    private static List<KeyMapping> movementKeys;
    private static List<KeyMapping> interactionKeys;

    // Q/F/G 原版 KeyMapping 引用
    private static KeyMapping keyDrop;   // Q → 丢出物品
    private static KeyMapping keySwapOffhand; // F → 副手交换

    // 粒子生成频率控制
    private static int particleTickCounter = 0;

    // 粒子颜色池（循环使用）
    private static final Vector3f[] PARTICLE_COLORS = {
            new Vector3f(1.0F, 0.2F, 0.2F), // 红
            new Vector3f(0.2F, 1.0F, 0.2F), // 绿
            new Vector3f(0.2F, 0.2F, 1.0F), // 蓝
            new Vector3f(1.0F, 1.0F, 0.2F), // 黄
            new Vector3f(1.0F, 0.2F, 1.0F), // 品红
            new Vector3f(0.2F, 1.0F, 1.0F), // 青
            new Vector3f(1.0F, 0.6F, 0.2F), // 橙
            new Vector3f(0.6F, 0.2F, 1.0F)  // 紫
    };

    private static void cacheKeyMappings(Minecraft mc) {
        if (mc.options == null) return;
        movementKeys = List.of(
                mc.options.keyUp,
                mc.options.keyDown,
                mc.options.keyLeft,
                mc.options.keyRight
        );
        interactionKeys = new ArrayList<>();
        interactionKeys.add(mc.options.keyInventory);
        interactionKeys.add(mc.options.keyChat);
        interactionKeys.add(mc.options.keySocialInteractions);

        keyDrop = mc.options.keyDrop;
        keySwapOffhand = mc.options.keySwapOffhand;
    }

    // ========== 修复：补回此方法 ==========
    private static boolean isKeyBindingActive(KeyMapping km) {
        int code = km.getKey().getValue();
        return KeyBindings.fromCode(code) != null;
    }
    // ==================== HUD 提示 ====================
    @SubscribeEvent
    public static void onRenderHud(RenderGuiEvent.Post event) {
        if (currentActivePos != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            event.getGuiGraphics().drawString(
                    mc.font,
                    Component.translatable("screen.cbadd.press_key"),
                    10, 10, 0xAAFFFFFF
            );
        }
    }

    // ==================== 每 Tick 拦截按键并发送信号 ====================
    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        if (currentActivePos == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (mc.screen instanceof PauseScreen) {
            deactivate();
            return;
        }

        if (justActivatedTimer > 0) {
            justActivatedTimer--;
            return;
        }

        long window = mc.getWindow().getWindow();

        boolean shiftDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (shiftDown && !shiftWasDown) {
            deactivate();
            shiftWasDown = true;
            return;
        }
        shiftWasDown = shiftDown;

        if (movementKeys == null || interactionKeys == null) {
            cacheKeyMappings(mc);
            if (movementKeys == null) return;
        }

        for (KeyMapping km : movementKeys) {
            if (isKeyBindingActive(km)) km.setDown(false);
        }
        for (KeyMapping km : interactionKeys) {
            if (isKeyBindingActive(km)) {
                int limit = 0;
                while (km.consumeClick() && limit < 10) limit++;
            }
        }
        if (keyDrop != null && isKeyBindingActive(keyDrop)) {
            int limit = 0;
            while (keyDrop.consumeClick() && limit < 10) limit++;
        }
        if (keySwapOffhand != null && isKeyBindingActive(keySwapOffhand)) {
            int limit = 0;
            while (keySwapOffhand.consumeClick() && limit < 10) limit++;
        }

        for (KeyBindings bind : KeyBindings.values()) {
            boolean pressed = GLFW.glfwGetKey(window, bind.getCode()) == GLFW.GLFW_PRESS;
            if (pressed) {
                if (!pressedKeys.contains(bind)) {
                    pressedKeys.add(bind);
                    lastHeartbeatTick.put(bind, (int) mc.level.getGameTime());
                    PacketDistributor.sendToServer(new TriggerKeyPacket(currentActivePos, bind.name(), true));
                }
            } else {
                if (pressedKeys.contains(bind)) {
                    pressedKeys.remove(bind);
                    lastHeartbeatTick.remove(bind);
                    PacketDistributor.sendToServer(new TriggerKeyPacket(currentActivePos, bind.name(), false));
                }
            }
        }

        long gameTime = mc.level.getGameTime();
        for (KeyBindings bind : new HashSet<>(pressedKeys)) {
            int lastTick = lastHeartbeatTick.getOrDefault(bind, 0);
            if (gameTime - lastTick >= 10) {
                PacketDistributor.sendToServer(new TriggerKeyPacket(currentActivePos, bind.name(), true));
                lastHeartbeatTick.put(bind, (int) gameTime);
            }
        }
    }

    // ==================== 粒子生成（替代文字） ====================
    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // 仅当主手持有 KeyTriggerBlockItem 且有绑定时才生成粒子
        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof KeyTriggerBlockItem)) return;

        CustomData customData = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (!tag.contains("BindList", Tag.TAG_LIST)) return;

        ListTag list = tag.getList("BindList", Tag.TAG_COMPOUND);
        if (list.isEmpty()) return;

        // 每 4 tick 生成一波粒子，避免过密
        particleTickCounter++;
        if (particleTickCounter % 2 != 0) return;

        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            BlockPos pos = new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z"));
            Vector3f color = PARTICLE_COLORS[i % PARTICLE_COLORS.length];
            // 限制粒子在方块中心 0.8m 立方体内生成
            double x = pos.getX() + 0.5 + (mc.level.random.nextDouble() - 0.5) * 0.9;
            double y = pos.getY() + 0.5 + (mc.level.random.nextDouble() - 0.5) * 0.9;
            double z = pos.getZ() + 0.5 + (mc.level.random.nextDouble() - 0.5) * 0.9;
            mc.level.addParticle(new DustParticleOptions(color, 0.8F), x, y, z, 0.0D, 0.02D, 0.0D);
        }
    }

    // ==================== 世界渲染（仅保留线框） ====================
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof KeyTriggerBlockItem)) return;

        CustomData customData = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (!tag.contains("BindList", Tag.TAG_LIST)) return;

        ListTag list = tag.getList("BindList", Tag.TAG_COMPOUND);
        if (list.isEmpty()) return;

        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        PoseStack poseStack = event.getPoseStack();

        // 只绘制线框，不再绘制文字
        drawBlockWireframes(poseStack, bufferSource, list, cam);

        bufferSource.endBatch();
    }

    private static final ResourceLocation LINE_TEXTURE = ResourceLocation.fromNamespaceAndPath(CbaddMod.MODID, "textures/lines/line1.png");

    private static void drawBlockWireframes(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                            ListTag list, Vec3 cam) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.text(LINE_TEXTURE));
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            BlockPos pos = new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z"));
            drawCubeFrame(poseStack, consumer, pos, cam);
        }
    }

    private static void drawCubeFrame(PoseStack poseStack, VertexConsumer consumer, BlockPos pos, Vec3 cam) {
        float minX = (float) (pos.getX() - cam.x);
        float minY = (float) (pos.getY() - cam.y);
        float minZ = (float) (pos.getZ() - cam.z);
        float maxX = minX + 1;
        float maxY = minY + 1;
        float maxZ = minZ + 1;
        float thickness = 0.02F;

        float[][] edges = {
                {minX, minY, minZ, maxX, minY, minZ},
                {maxX, minY, minZ, maxX, minY, maxZ},
                {maxX, minY, maxZ, minX, minY, maxZ},
                {minX, minY, maxZ, minX, minY, minZ},
                {minX, maxY, minZ, maxX, maxY, minZ},
                {maxX, maxY, minZ, maxX, maxY, maxZ},
                {maxX, maxY, maxZ, minX, maxY, maxZ},
                {minX, maxY, maxZ, minX, maxY, minZ},
                {minX, minY, minZ, minX, maxY, minZ},
                {maxX, minY, minZ, maxX, maxY, minZ},
                {maxX, minY, maxZ, maxX, maxY, maxZ},
                {minX, minY, maxZ, minX, maxY, maxZ}
        };

        for (float[] edge : edges) {
            drawLineSegment(poseStack, consumer, edge[0], edge[1], edge[2], edge[3], edge[4], edge[5], thickness, cam);
        }
    }

    private static void drawLineSegment(PoseStack poseStack, VertexConsumer consumer,
                                        float x1, float y1, float z1,
                                        float x2, float y2, float z2,
                                        float thickness, Vec3 cam) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float midX = (x1 + x2) / 2f;
        float midY = (y1 + y2) / 2f;
        float midZ = (z1 + z2) / 2f;
        float viewDirX = -midX;
        float viewDirY = -midY;
        float viewDirZ = -midZ;
        float len = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 0.0001f) return;
        dx /= len; dy /= len; dz /= len;

        float rightX = dy * viewDirZ - dz * viewDirY;
        float rightY = dz * viewDirX - dx * viewDirZ;
        float rightZ = dx * viewDirY - dy * viewDirX;
        float rLen = (float)Math.sqrt(rightX*rightX + rightY*rightY + rightZ*rightZ);
        if (rLen < 0.0001f) {
            rightX = -dz; rightY = 0; rightZ = dx;
            rLen = (float)Math.sqrt(rightX*rightX + rightZ*rightZ);
            if (rLen < 0.0001f) {
                rightX = 0; rightY = -dz; rightZ = dy;
                rLen = (float)Math.sqrt(rightY*rightY + rightZ*rightZ);
            }
        }
        rightX /= rLen; rightY /= rLen; rightZ /= rLen;
        float offsetX = rightX * thickness;
        float offsetY = rightY * thickness;
        float offsetZ = rightZ * thickness;

        Matrix4f matrix = poseStack.last().pose();
        int fullBright = LightTexture.FULL_BRIGHT;
        int noOverlay = OverlayTexture.NO_OVERLAY;

        consumer.addVertex(matrix, x1 - offsetX, y1 - offsetY, z1 - offsetZ)
                .setColor(255, 255, 255, 255).setUv(0, 0).setUv2(fullBright, fullBright).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 + offsetX, y1 + offsetY, z1 + offsetZ)
                .setColor(255, 255, 255, 255).setUv(1, 0).setUv2(fullBright, fullBright).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 + offsetX, y2 + offsetY, z2 + offsetZ)
                .setColor(255, 255, 255, 255).setUv(1, 1).setUv2(fullBright, fullBright).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 - offsetX, y2 - offsetY, z2 - offsetZ)
                .setColor(255, 255, 255, 255).setUv(0, 1).setUv2(fullBright, fullBright).setNormal(0, 1, 0);
    }

    // ==================== 公共 API ====================
    public static void registerActiveOverlay(BlockPos pos) {
        if (currentActivePos != null) deactivate();
        currentActivePos = pos;
        justActivatedTimer = ACTIVATION_COOLDOWN_TICKS;
        if (movementKeys == null) cacheKeyMappings(Minecraft.getInstance());
    }

    public static void deactivate() {
        if (currentActivePos == null) return;
        for (KeyBindings bind : pressedKeys) {
            PacketDistributor.sendToServer(new TriggerKeyPacket(currentActivePos, bind.name(), false));
        }
        pressedKeys.clear();
        lastHeartbeatTick.clear();
        shiftWasDown = false;
        currentActivePos = null;
    }
}