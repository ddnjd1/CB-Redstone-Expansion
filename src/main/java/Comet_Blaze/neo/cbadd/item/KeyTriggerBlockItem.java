package Comet_Blaze.neo.cbadd.item;

import Comet_Blaze.neo.cbadd.client.screen.BindKeyScreen;
import Comet_Blaze.neo.cbadd.entity.ConnectorTileEntity;
import Comet_Blaze.neo.cbadd.entity.KeyTriggerBlockEntity;
import Comet_Blaze.neo.cbadd.network.BindKeyPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class KeyTriggerBlockItem extends BlockItem {

    private static final int[] KEY_COLORS = {
            0xFF3333, 0x33FF33, 0x3333FF, 0xFFFF33,
            0xFF33FF, 0x33FFFF, 0xFF9933, 0x9933FF
    };

    public KeyTriggerBlockItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.FAIL;
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof ConnectorTileEntity connector && !connector.isInput()) {
            if (level.isClientSide) {
                if (player.isShiftKeyDown()) {
                    PacketDistributor.sendToServer(new BindKeyPacket(context.getHand(), pos, "CLEAR_ALL"));
                    return InteractionResult.SUCCESS;
                } else {
                    Minecraft.getInstance().setScreen(new BindKeyScreen(context.getHand(), pos));
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.CONSUME;
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult result = super.place(context);
        if (result.consumesAction() && !context.getLevel().isClientSide) {
            BlockPos placedPos = context.getClickedPos();
            BlockEntity be = context.getLevel().getBlockEntity(placedPos);
            if (be instanceof KeyTriggerBlockEntity entity) {
                ItemStack stack = context.getItemInHand();
                ListTag bindList = getBindList(stack);
                if (bindList != null) {
                    entity.loadBindsFromItem(bindList);
                }
            }
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.cbadd.key_trigger_block.desc").withStyle(ChatFormatting.GRAY));

        ListTag bindList = getBindList(stack);
        int bindCount = bindList != null ? bindList.size() : 0;
        tooltip.add(Component.translatable("item.cbadd.key_trigger_block.bind_count", bindCount).withStyle(ChatFormatting.DARK_GREEN));

        boolean shiftDown = Screen.hasShiftDown();
        if (shiftDown) {
            if (bindCount > 0 && bindList != null) {
                for (int i = 0; i < bindList.size(); i++) {
                    CompoundTag entry = bindList.getCompound(i);
                    int x = entry.getInt("X");
                    int y = entry.getInt("Y");
                    int z = entry.getInt("Z");
                    String key = entry.getString("Key");

                    int color = KEY_COLORS[i % KEY_COLORS.length];
                    MutableComponent line = Component.literal("[")
                            .append(String.format("%d, %d, %d", x, y, z))
                            .append("] : ")
                            .append(Component.translatable("item.cbadd.key_trigger_block.bound_key"))
                            .append(" ");  // 添加一个空格分隔
                    MutableComponent keyComp = Component.literal(key)
                            .withStyle(s -> s.withColor(TextColor.fromRgb(color & 0xFFFFFF)));
                    line.append(keyComp);
                    tooltip.add(line.withStyle(ChatFormatting.WHITE));
                }
            } else {
                tooltip.add(Component.translatable("item.cbadd.key_trigger_block.no_binds").withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            tooltip.add(Component.translatable("item.cbadd.key_trigger_block.press_shift").withStyle(ChatFormatting.GOLD));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }

    // ==================== NBT 工具方法（保持不变） ====================
    public static ListTag getBindList(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return tag.contains("BindList", Tag.TAG_LIST) ? tag.getList("BindList", Tag.TAG_COMPOUND) : null;
    }

    public static void addBind(ItemStack stack, BlockPos absolutePos, String key) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        ListTag list = tag.contains("BindList", Tag.TAG_LIST) ? tag.getList("BindList", Tag.TAG_COMPOUND) : new ListTag();
        removeBindFromList(list, absolutePos, key);
        CompoundTag entry = new CompoundTag();
        entry.putInt("X", absolutePos.getX());
        entry.putInt("Y", absolutePos.getY());
        entry.putInt("Z", absolutePos.getZ());
        entry.putString("Key", key);
        list.add(entry);
        tag.put("BindList", list);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag));
    }

    public static void removeBind(ItemStack stack, BlockPos absolutePos) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        ListTag list = tag.contains("BindList", Tag.TAG_LIST) ? tag.getList("BindList", Tag.TAG_COMPOUND) : new ListTag();
        if (removeBindFromList(list, absolutePos)) {
            tag.put("BindList", list);
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(tag));
        }
    }

    public static void removeAllBindsForPos(ItemStack stack, BlockPos pos) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        ListTag list = tag.contains("BindList", Tag.TAG_LIST) ? tag.getList("BindList", Tag.TAG_COMPOUND) : new ListTag();
        boolean removed = false;
        for (int i = list.size() - 1; i >= 0; i--) {
            CompoundTag entry = list.getCompound(i);
            if (entry.getInt("X") == pos.getX() && entry.getInt("Y") == pos.getY() && entry.getInt("Z") == pos.getZ()) {
                list.remove(i);
                removed = true;
            }
        }
        if (removed) {
            tag.put("BindList", list);
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(tag));
        }
    }

    private static boolean removeBindFromList(ListTag list, BlockPos pos) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.getInt("X") == pos.getX() && entry.getInt("Y") == pos.getY() && entry.getInt("Z") == pos.getZ()) {
                list.remove(i);
                return true;
            }
        }
        return false;
    }

    private static boolean removeBindFromList(ListTag list, BlockPos pos, String key) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.getInt("X") == pos.getX() && entry.getInt("Y") == pos.getY() && entry.getInt("Z") == pos.getZ()
                    && entry.getString("Key").equals(key)) {
                list.remove(i);
                return true;
            }
        }
        return false;
    }

    public static boolean isAlreadyBound(ItemStack stack, BlockPos pos) {
        ListTag list = getBindList(stack);
        if (list == null) return false;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.getInt("X") == pos.getX() && entry.getInt("Y") == pos.getY() && entry.getInt("Z") == pos.getZ())
                return true;
        }
        return false;
    }

    public static boolean isAlreadyBound(ItemStack stack, BlockPos pos, String key) {
        ListTag list = getBindList(stack);
        if (list == null) return false;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.getInt("X") == pos.getX() && entry.getInt("Y") == pos.getY() && entry.getInt("Z") == pos.getZ()
                    && entry.getString("Key").equals(key))
                return true;
        }
        return false;
    }
}