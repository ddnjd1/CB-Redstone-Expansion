package Comet_Blaze.neo.cbadd.client.screen;

import Comet_Blaze.neo.cbadd.item.KeyBindings;
import Comet_Blaze.neo.cbadd.item.KeyTriggerBlockItem;
import Comet_Blaze.neo.cbadd.network.BindKeyPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public class BindKeyScreen extends Screen {

    private final InteractionHand hand;
    private final BlockPos targetPos;

    public BindKeyScreen(InteractionHand hand, BlockPos targetPos) {
        super(Component.translatable("screen.cbadd.bind_key"));
        this.hand = hand;
        this.targetPos = targetPos;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }

        KeyBindings bind = KeyBindings.fromCode(keyCode);
        if (bind != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                ItemStack stack = mc.player.getItemInHand(this.hand);
                if (stack.getItem() instanceof KeyTriggerBlockItem) {
                    if (KeyTriggerBlockItem.isAlreadyBound(stack, this.targetPos, bind.name())) {
                        mc.player.displayClientMessage(Component.translatable("message.cbadd.bind_exists"), true);
                        return true;
                    }
                }
            }
            PacketDistributor.sendToServer(new BindKeyPacket(hand, targetPos, bind.name()));
            this.onClose();
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 20, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.translatable("screen.cbadd.prompt"),
                this.width / 2, this.height / 2, 0xCCCCCC);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
    }
}
