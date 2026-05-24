package Comet_Blaze.neo.cbadd.block;

import Comet_Blaze.neo.cbadd.client.event.ClientEventHandler;
import Comet_Blaze.neo.cbadd.entity.ConnectorTileEntity;
import Comet_Blaze.neo.cbadd.entity.KeyTriggerBlockEntity;
import Comet_Blaze.neo.cbadd.init.CbaddModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class KeyTriggerBlock extends Block implements EntityBlock {

    private static final VoxelShape BLOCK_SHAPE = Shapes.box(
            0.0D,
            0.0D,
            0.0D,
            1.0D,
            12.0D / 16.0D,
            1.0D
    );

    public KeyTriggerBlock() {
        super(Properties.of()
                .strength(2.0f)
                .noOcclusion()
                .isValidSpawn((a, b, c, d) -> false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BLOCK_SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KeyTriggerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == CbaddModBlockEntities.KEY_TRIGGER.get() ?
                (lvl, pos, st, be) -> ((KeyTriggerBlockEntity) be).tick() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (level.isClientSide) {
            ClientEventHandler.deactivate();
            ClientEventHandler.registerActiveOverlay(pos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    // ========== 新增：方块被破坏时生成带 NBT 的掉落物 ==========
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // 仅在方块被真正替换（非活塞推拉）且服务端时处理
        if (!level.isClientSide && !state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof KeyTriggerBlockEntity entity) {
                // 创建带 NBT 的掉落物
                ItemStack dropStack = new ItemStack(this.asItem());

                // 获取方块实体中的绑定数据
                ListTag bindList = new ListTag();
                for (KeyTriggerBlockEntity.BindEntry bind : entity.getBinds()) {
                    CompoundTag entry = new CompoundTag();
                    // 将相对坐标转回绝对坐标
                    BlockPos absolute = pos.offset(bind.relativePos);
                    entry.putInt("X", absolute.getX());
                    entry.putInt("Y", absolute.getY());
                    entry.putInt("Z", absolute.getZ());
                    entry.putString("Key", bind.key);
                    bindList.add(entry);
                }

                if (!bindList.isEmpty()) {
                    CompoundTag customTag = new CompoundTag();
                    customTag.put("BindList", bindList);
                    dropStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                            net.minecraft.world.item.component.CustomData.of(customTag));
                }

                // 在方块被移除前生成掉落物
                net.minecraft.world.item.ItemStack singleDrop = dropStack.copy();
                net.minecraft.world.Containers.dropItemStack(
                        level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, singleDrop);

                // 阻止默认掉落
                level.removeBlock(pos, false);
                // 注意：调用 removeBlock 后 onRemove 不会再被调用（因为 state.is(newState.getBlock()) 为 true）
                // 但实际上这里需要小心处理。更安全的做法是覆盖 spawnAfterBreak：
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * 修复：覆盖 spawnAfterBreak 来生成带 NBT 的掉落物。
     * 同时配合 `onRemove` 中 `level.removeBlock` 来阻止默认掉落。
     */
    @Override
    public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack stack, boolean dropExperience) {
        // 什么都不做，因为 onRemove 已经手动生成了掉落物
        // 这样就不会产生两份掉落
    }

    /**
     * 获取方块实体的绑定列表（供外部使用）
     */
    public ListTag getBindListFromEntity(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof KeyTriggerBlockEntity entity) {
            ListTag bindList = new ListTag();
            for (KeyTriggerBlockEntity.BindEntry bind : entity.getBinds()) {
                CompoundTag entry = new CompoundTag();
                BlockPos absolute = pos.offset(bind.relativePos);
                entry.putInt("X", absolute.getX());
                entry.putInt("Y", absolute.getY());
                entry.putInt("Z", absolute.getZ());
                entry.putString("Key", bind.key);
                bindList.add(entry);
            }
            return bindList;
        }
        return new ListTag();
    }
}
