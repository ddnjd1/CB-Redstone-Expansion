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
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof KeyTriggerBlockEntity entity) {
                ItemStack dropStack = new ItemStack(this.asItem());
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

                if (!bindList.isEmpty()) {
                    CompoundTag customTag = new CompoundTag();
                    customTag.put("BindList", bindList);
                    dropStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                            net.minecraft.world.item.component.CustomData.of(customTag));
                }
                net.minecraft.world.item.ItemStack singleDrop = dropStack.copy();
                net.minecraft.world.Containers.dropItemStack(
                        level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, singleDrop);
                level.removeBlock(pos, false);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack stack, boolean dropExperience) {
    }

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
