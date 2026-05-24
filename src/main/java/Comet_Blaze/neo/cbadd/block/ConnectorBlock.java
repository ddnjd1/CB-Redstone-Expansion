package Comet_Blaze.neo.cbadd.block;

import Comet_Blaze.neo.cbadd.entity.ConnectorTileEntity;
import Comet_Blaze.neo.cbadd.init.CbaddModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;

public class ConnectorBlock extends Block implements EntityBlock {
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    public final boolean isInput;

    public ConnectorBlock(boolean isInput) {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.0f)
                .noOcclusion()
                .isValidSpawn((state, getter, pos, type) -> false));
        this.isInput = isInput;
        this.registerDefaultState(this.stateDefinition.any().setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConnectorTileEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return type == CbaddModBlockEntities.CONNECTOR.get()
                ? (lvl, pos, st, be) -> ((ConnectorTileEntity) be).tick()
                : null;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return !isInput;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (!isInput) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ConnectorTileEntity connector) {
                return connector.getSignal();
            }
        }
        return 0;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide && isInput) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ConnectorTileEntity connector) {
                connector.onNeighborChange();
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ConnectorTileEntity connector) {
                if (connector.isInput()) {
                    connector.notifyOutputsRemoved();
                } else {
                    connector.notifyInputsRemoved();
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
