package Comet_Blaze.neo.cbadd.item;

import Comet_Blaze.neo.cbadd.entity.ConnectorTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class ConnectorItem extends Item {
    public ConnectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();

        // 潜行清除绑定
        if (player.isShiftKeyDown()) {
            clearBoundInput(stack, player);
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ConnectorTileEntity connector)) {
            return InteractionResult.PASS;
        }

        // 读取当前绑定的坐标
        CompoundTag tag = getOrCreateCustomTag(stack);
        BlockPos boundPos = null;
        if (tag.contains("BoundX") && tag.contains("BoundY") && tag.contains("BoundZ")) {
            boundPos = new BlockPos(
                    tag.getInt("BoundX"),
                    tag.getInt("BoundY"),
                    tag.getInt("BoundZ")
            );
        }

        // ====== WightlessConnectorBlock 专属逻辑 ======
        if (connector.isWightless()) {
            if (boundPos != null) {
                // 确保绑定的也是 WightlessConnectorBlock
                BlockEntity boundBe = level.getBlockEntity(boundPos);
                if (!(boundBe instanceof ConnectorTileEntity boundTile) || !boundTile.isWightless()) {
                    player.displayClientMessage(
                            Component.translatable("message.connector.bound_input_invalid"), true);
                    clearBoundInput(stack, player); // 无效绑定，告知清除
                    return InteractionResult.FAIL;
                }

                // 防止自连
                if (boundPos.equals(pos)) {
                    player.displayClientMessage(
                            Component.translatable("message.connector.cannot_pair_self"), true);
                    // 清除物品绑定，但不发送清除消息
                    tag.remove("BoundX");
                    tag.remove("BoundY");
                    tag.remove("BoundZ");
                    setCustomTag(stack, tag);
                    return InteractionResult.FAIL;
                }

                // 正确判断当前是否已配对（必须双向同时存在才算已连接）
                boolean alreadyPaired = boundTile.isPairedWith(pos) && connector.isPairedWith(boundPos);
                if (alreadyPaired) {
                    // 已配对 → 解除链接
                    boundTile.removePairedConnector(pos);
                    connector.removePairedConnector(boundPos);
                    player.displayClientMessage(
                            Component.translatable("message.connector.wightless_pair_removed"), true);
                } else {
                    // 未配对 → 建立新链接（先清理可能残留的单向记录）
                    if (boundTile.isPairedWith(pos)) boundTile.removePairedConnector(pos);
                    if (connector.isPairedWith(boundPos)) connector.removePairedConnector(boundPos);

                    boolean a = boundTile.addPairedConnector(pos);
                    boolean b = connector.addPairedConnector(boundPos);
                    if (a && b) {
                        player.displayClientMessage(
                                Component.translatable("message.connector.wightless_pair_success"), true);
                    } else {
                        // 失败回滚
                        if (a) boundTile.removePairedConnector(pos);
                        if (b) connector.removePairedConnector(boundPos);
                        player.displayClientMessage(
                                Component.translatable("message.connector.wightless_pair_failed"), true);
                    }
                }
                // 无论建立还是解除，都清理物品绑定，但不弹出“已清除绑定”提示
                tag.remove("BoundX");
                tag.remove("BoundY");
                tag.remove("BoundZ");
                setCustomTag(stack, tag);
                return InteractionResult.SUCCESS;
            } else {
                // 无绑定 → 记录当前 Wightless 坐标
                tag.putInt("BoundX", pos.getX());
                tag.putInt("BoundY", pos.getY());
                tag.putInt("BoundZ", pos.getZ());
                setCustomTag(stack, tag);
                player.displayClientMessage(
                        Component.translatable("message.connector.select_wightless"), true);
                return InteractionResult.SUCCESS;
            }
        }

        // ====== 原有输入/输出方块逻辑 ======
        if (connector.isInput()) {
            // 右键输入方块：记录坐标
            tag.putInt("BoundX", pos.getX());
            tag.putInt("BoundY", pos.getY());
            tag.putInt("BoundZ", pos.getZ());
            setCustomTag(stack, tag);
            player.displayClientMessage(
                    Component.translatable("message.connector.select_input", ConnectorTileEntity.getMaxOutputs()),
                    true);
            return InteractionResult.SUCCESS;
        } else {
            // 右键普通输出方块
            if (boundPos == null) {
                player.displayClientMessage(
                        Component.translatable("message.connector.need_input_first"), true);
                return InteractionResult.FAIL;
            }

            BlockEntity inputBe = level.getBlockEntity(boundPos);
            if (!(inputBe instanceof ConnectorTileEntity inputTile) || !inputTile.isInput()) {
                player.displayClientMessage(
                        Component.translatable("message.connector.bound_input_invalid"), true);
                clearBoundInput(stack, player);
                return InteractionResult.FAIL;
            }

            // 若已连接则断开
            if (inputTile.containsOutput(pos) && connector.containsInput(boundPos)) {
                inputTile.removeOutput(pos);
                connector.removeInput(boundPos);
                player.displayClientMessage(
                        Component.translatable("message.connector.connection_removed"), true);
                return InteractionResult.SUCCESS;
            }

            // 检查连接上限
            if (inputTile.getConnectedOutputs().size() >= ConnectorTileEntity.getMaxOutputs()) {
                player.displayClientMessage(
                        Component.translatable("message.connector.max_outputs_reached",
                                ConnectorTileEntity.getMaxOutputs()), true);
                return InteractionResult.FAIL;
            }

            // 建立连接
            if (!inputTile.addOutput(pos)) {
                player.displayClientMessage(
                        Component.translatable("message.connector.failed_to_establish"), true);
                return InteractionResult.FAIL;
            }
            if (!connector.addInput(boundPos)) {
                inputTile.removeOutput(pos);
                player.displayClientMessage(
                        Component.translatable("message.connector.failed_to_establish"), true);
                return InteractionResult.FAIL;
            }

            player.displayClientMessage(
                    Component.translatable("message.connector.connect_success"), true);
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                clearBoundInput(stack, player);
            }
            return InteractionResultHolder.success(stack);
        }
        return super.use(level, player, hand);
    }

    private void clearBoundInput(ItemStack stack, Player player) {
        CompoundTag tag = getOrCreateCustomTag(stack);
        if (tag.contains("BoundX")) {
            tag.remove("BoundX");
            tag.remove("BoundY");
            tag.remove("BoundZ");
            setCustomTag(stack, tag);
            player.displayClientMessage(
                    Component.translatable("message.connector.bound_input_cleared"), true);
        } else {
            player.displayClientMessage(
                    Component.translatable("message.connector.no_bound_input"), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CompoundTag tag = getOrCreateCustomTag(stack);
        if (tag.contains("BoundX")) {
            BlockPos pos = new BlockPos(
                    tag.getInt("BoundX"),
                    tag.getInt("BoundY"),
                    tag.getInt("BoundZ")
            );
            tooltip.add(Component.translatable("tooltip.connector.bound_input",
                    pos.getX(), pos.getY(), pos.getZ()));
        } else {
            tooltip.add(Component.translatable("tooltip.connector.no_bound_input"));
        }
        tooltip.add(Component.translatable("tooltip.connector.clear_instruction"));
    }

    private CompoundTag getOrCreateCustomTag(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag();
    }

    private void setCustomTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
